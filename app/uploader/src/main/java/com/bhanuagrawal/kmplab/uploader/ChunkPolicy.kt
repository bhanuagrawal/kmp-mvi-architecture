package com.bhanuagrawal.kmplab.uploader

import java.io.File
import java.net.URI

interface ChunkPolicy {
    fun getChunk(uri: URI): List<Chunk>
}

data class Chunk(
    val totalSize: Int,
    val offset: Int,
)

/** Treats the whole file as a single chunk. Useful for small files or non-resumable uploads. */
class NoChunkingPolicy : ChunkPolicy {
    override fun getChunk(uri: URI): List<Chunk> {
        val size = File(uri).length().toInt()
        return listOf(Chunk(totalSize = size, offset = 0))
    }
}

/** Splits the file into fixed-size chunks (last chunk may be smaller). */
class FixedSizeChunkPolicy(private val chunkSize: Int) : ChunkPolicy {
    init { require(chunkSize > 0) { "chunkSize must be > 0" } }

    override fun getChunk(uri: URI): List<Chunk> {
        val total = File(uri).length()
        if (total == 0L) return emptyList()
        val chunks = mutableListOf<Chunk>()
        var offset = 0L
        while (offset < total) {
            val remaining = total - offset
            val size = minOf(remaining, chunkSize.toLong()).toInt()
            chunks += Chunk(totalSize = size, offset = offset.toInt())
            offset += size
        }
        return chunks
    }
}

/**
 * Picks a chunk size based on file size:
 *  - <= 1 MB           → single chunk
 *  - <= 100 MB         → 1 MB chunks
 *  - >  100 MB         → 8 MB chunks
 */
class AdaptiveChunkPolicy : ChunkPolicy {
    override fun getChunk(uri: URI): List<Chunk> {
        val total = File(uri).length()
        val chunkSize = when {
            total <= ONE_MB -> total.toInt().coerceAtLeast(0)
            total <= 100 * ONE_MB -> ONE_MB.toInt()
            else -> 8 * ONE_MB.toInt()
        }
        if (chunkSize == 0) return emptyList()
        return FixedSizeChunkPolicy(chunkSize).getChunk(uri)
    }

    private companion object {
        const val ONE_MB = 1024L * 1024L
    }
}
