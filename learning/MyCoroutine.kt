// Scratch experiment — NOT part of any source set, not compiled.
// Playing with a Handler/Looper driven "resume" state machine to see how a
// callback-based API would look if it were written as a coroutine.
// Kept here on purpose (see CLAUDE.md §3).
package com.bhanuagrawal.kmplab

import android.os.Handler
import android.os.Looper

class MyCoroutine {

    private var state = 0

    fun resume() {

        when (state) {

            0 -> {
                println("Step A")

                state = 1

                asyncOperation {
                    resume()
                }

                return
            }

            1 -> {
                println("Step B")

                state = 2

                asyncOperation {
                    resume()
                }

                return
            }

            2 -> {
                println("Step C")

                println("Coroutine completed")
            }
        }
    }

    private fun asyncOperation(onComplete: () -> Unit) {

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(
            Runnable {
                onComplete()
            },
            1000
        )
    }
}
