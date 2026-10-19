package com.bhanuagrawal.kmplab.ui

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread

class TestActivity: Activity() {

    lateinit var workerThread: HandlerThread

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workerThread= MyWorkerThread()
        workerThread.start()
    }

    fun testMethod(){

        Handler(workerThread.looper).post {
            //some io operation
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        workerThread.quit()
    }
}

class MyWorkerThread: HandlerThread("myworker"){

}