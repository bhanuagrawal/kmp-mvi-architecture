# Test upload server

Tiny Node.js HTTP server that accepts `HEAD` and `PUT` (with `Content-Range`) for
testing the uploader SDK.

## Run

```bash
cd test-server
node server.js
```

It listens on `http://0.0.0.0:8080` and writes files to `./uploads/`.

## From the Android emulator

Use `http://10.0.2.2:8080/<filename>` — `10.0.2.2` is the emulator's loopback
to the host machine.

## From a physical device

1. Find your laptop's LAN IP (e.g. `ifconfig | grep "inet "`).
2. Make sure the laptop firewall allows port 8080.
3. Use `http://<your-laptop-ip>:8080/<filename>` from the device.

## What you'll see in the server log

```
[12:00:01.234] HEAD test.bin
[12:00:01.345] PUT  test.bin  offset=0       range="bytes 0-1048575/5242880"   fileSize=5242880
[12:00:01.456] PUT  test.bin  offset=1048576 range="bytes 1048576-2097151/..." fileSize=5242880
...
```

After upload completes, `cmp` the file in `./uploads/` against the source to
verify correctness.
