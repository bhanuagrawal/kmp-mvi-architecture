/**
 * Tiny upload test server for the uploader SDK.
 *
 *  HEAD /<filename>   → 200, Accept-Ranges: bytes
 *  PUT  /<filename>   → writes body to ./uploads/<filename>
 *                       honors Content-Range: bytes <off>-<end>/<total>
 *
 * Run: node server.js  (listens on 0.0.0.0:8080)
 * From Android emulator, use   http://10.0.2.2:8080/<filename>
 */
const http = require('http')
const fs = require('fs')
const path = require('path')
const url = require('url')

const PORT = 8080
// Artificial per-PUT delay (ms) so uploads are slow enough to test cancellation.
// Set to 0 for full speed:  CHUNK_DELAY_MS=0 node server.js
const CHUNK_DELAY_MS = parseInt(process.env.CHUNK_DELAY_MS ?? '2000', 10)
const UPLOAD_DIR = path.join(__dirname, 'uploads')
fs.mkdirSync(UPLOAD_DIR, { recursive: true })

function log(...args) {
    const ts = new Date().toISOString().slice(11, 23)
    console.log(`[${ts}]`, ...args)
}

http.createServer((req, res) => {
    const parsed = url.parse(req.url, true)
    const name = path.basename(parsed.pathname || '/file.bin') || 'file.bin'
    const safe = name.replace(/[^a-zA-Z0-9._-]/g, '_')
    const target = path.join(UPLOAD_DIR, safe)

    if (req.method === 'HEAD') {
        log(`HEAD ${safe}`)
        res.setHeader('Accept-Ranges', 'bytes')
        res.writeHead(200)
        res.end()
        return
    }

    if (req.method !== 'PUT') {
        res.writeHead(405).end('Method not allowed')
        return
    }

    const range = req.headers['content-range']
    let offset = 0
    let total = null
    if (range) {
        const m = range.match(/bytes (\d+)-(\d+)\/(\d+|\*)/)
        if (m) {
            offset = parseInt(m[1], 10)
            total = m[3] === '*' ? null : parseInt(m[3], 10)
        }
    }

    // Pre-allocate the file to the declared total size so positional writes work.
    if (total != null) {
        if (!fs.existsSync(target) || fs.statSync(target).size < total) {
            const fd = fs.openSync(target, fs.existsSync(target) ? 'r+' : 'w')
            fs.ftruncateSync(fd, total)
            fs.closeSync(fd)
        }
    } else if (!fs.existsSync(target)) {
        fs.closeSync(fs.openSync(target, 'w'))
    }

    const writeStream = fs.createWriteStream(target, {
        flags: 'r+',
        start: offset,
    })

    req.pipe(writeStream)
    req.on('end', () => {
        // Hold the response open to simulate a slow upload — gives you a window
        // to hit Cancel in the app. The socket close on cancel aborts this.
        setTimeout(() => {
            const size = fs.statSync(target).size
            log(`PUT  ${safe}  offset=${offset}  range="${range || '(none)'}"  fileSize=${size}`)
            res.setHeader('Content-Type', 'application/json')
            res.writeHead(200)
            res.end(JSON.stringify({ ok: true, offset, total, size }))
        }, CHUNK_DELAY_MS)
    })
    req.on('error', err => {
        log(`PUT  ${safe}  ERROR: ${err.message}`)
        res.writeHead(500).end('error')
    })
}).listen(PORT, '0.0.0.0', () => {
    log(`Upload server listening on http://0.0.0.0:${PORT}`)
    log(`Files saved to ${UPLOAD_DIR}`)
})
