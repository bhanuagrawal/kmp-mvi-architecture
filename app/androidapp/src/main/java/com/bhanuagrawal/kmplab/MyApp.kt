package com.bhanuagrawal.kmplab

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import com.bhanuagrawal.kmplab.datalayer.localdatasource.buildSharedModule
import com.bhanuagrawal.kmplab.di.ShareModule
import com.bhanuagrawal.kmplab.uploader.UploadSDK

class MyApp : Application(), Configuration.Provider {

    lateinit var uploadSdk: UploadSDK
        private set

    lateinit var sharedLogicModule: ShareModule
        private set



    override fun onCreate() {
        super.onCreate()
        instance = this
        uploadSdk = UploadSDK.create(this, namespace = "app")
        sharedLogicModule = buildSharedModule(this)
    }

    // WorkManager calls this on-demand (first getInstance), which happens well
    // after onCreate — so uploadSdk is already initialized. Requires the default
    // WorkManager initializer to be removed from the manifest (we use on-demand init).
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(uploadSdk.workerFactory)
            .build()

    companion object {
        lateinit var instance: MyApp
            private set
    }
}
