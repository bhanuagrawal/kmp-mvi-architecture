package com.bhanuagrawal.kmplab.uploader

import android.content.Context
import androidx.room3.Room
import androidx.work.WorkerFactory
import com.bhanuagrawal.kmplab.uploader.data.CoroutineBaseUploadService
import com.bhanuagrawal.kmplab.uploader.data.FileStagingServiceImpl
import com.bhanuagrawal.kmplab.uploader.data.StagedFileCleanupServiceImpl
import com.bhanuagrawal.kmplab.uploader.data.UploadMetaRepoImpl
import com.bhanuagrawal.kmplab.uploader.data.UploadServiceImpl
import com.bhanuagrawal.kmplab.uploader.data.UploadWorkerFactory
import com.bhanuagrawal.kmplab.uploader.data.UploaderDatabase
import com.bhanuagrawal.kmplab.uploader.data.WorkManagerBasedUploadService
import com.bhanuagrawal.kmplab.uploader.data.network.RetrofitRemoteDataSource
import com.bhanuagrawal.kmplab.uploader.data.network.UploadApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.io.File

class UploadSDK private constructor(
    private val uploader: Uploader,
    private val scope: CoroutineScope,
    /**
     * Register this on WorkManager so background chunk workers get their
     * dependencies. See [Application] wiring:
     * `Configuration.Builder().setWorkerFactory(uploadSdk.workerFactory)`.
     */
    val workerFactory: WorkerFactory,
) : Uploader by uploader {

    fun close() {
        scope.cancel()
    }

    companion object {
        fun create(
            appContext: Context,
            namespace: String = "default",
            okHttpClient: OkHttpClient? = null,
            chunkPolicy: ChunkPolicy = AdaptiveChunkPolicy(),
        ): UploadSDK {
            val context = appContext.applicationContext
            val client = okHttpClient ?: OkHttpClient.Builder().build()

            val retrofit = Retrofit.Builder()
                .baseUrl("http://localhost/")
                .client(client)
                .build()
            val api = retrofit.create(UploadApi::class.java)
            val remoteDataSource = RetrofitRemoteDataSource(api)

            val db = Room.databaseBuilder(
                context,
                UploaderDatabase::class.java,
                "uploader_$namespace.db",
            ).build()
            val dao = db.uploadMetaDao()

            val metaRepo = UploadMetaRepoImpl(dao)
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

            val coroutineService = CoroutineBaseUploadService(remoteDataSource, metaRepo)
            val workManagerService = WorkManagerBasedUploadService(context, remoteDataSource, metaRepo)
            val uploadService = UploadServiceImpl(
                coroutineBaseUploadService = coroutineService,
                workManagerBasedUploadService = workManagerService,
                remoteDataSource = remoteDataSource,
                metaRepo = metaRepo,
            )

            val stagingDir = File(context.filesDir, "uploader/$namespace/staged")
            val fileStagingService = FileStagingServiceImpl(
                context = context,
                stagingDir = stagingDir,
                metaRepo = metaRepo,
            )
            val cleanupService = StagedFileCleanupServiceImpl(
                stagingDir = stagingDir,
                metaRepo = metaRepo,
                activePathsProvider = { dao.activeLocalPaths().toSet() },
            )

            val uploader = UploaderImpl(
                uploadMetaRepo = metaRepo,
                remoteDataSource = remoteDataSource,
                chunkPolicy = chunkPolicy,
                scope = scope,
                uploadService = uploadService,
                fileStagingService = fileStagingService,
                cleanupService = cleanupService,
            )

            // Boot sweep — delete any orphaned staged files left from prior runs.
            scope.launch { cleanupService.sweepOrphans() }

            val workerFactory = UploadWorkerFactory(remoteDataSource, metaRepo)

            return UploadSDK(uploader, scope, workerFactory)
        }
    }
}
