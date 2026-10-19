package com.bhanuagrawal.kmplab.uploader

import java.net.URI


//create a builder for this class and fix the typos
class UploadRequest private constructor(
    val isForeground: Boolean,
    val uploadUrl: String,
    val fileURI: URI,
    val isForegroundOnly: Boolean,
    val authHeader: String, //Json ,
    val uploadPriority: UploadPriority,
) {
    internal fun withFileUri(uri: URI): UploadRequest = UploadRequest(
        isForeground = isForeground,
        uploadUrl = uploadUrl,
        fileURI = uri,
        isForegroundOnly = isForegroundOnly,
        authHeader = authHeader,
        uploadPriority = uploadPriority,
    )

    class Builder {
        private var isForeground: Boolean = false
        private var uploadUrl: String? = null
        private var fileURI: URI? = null
        private var isForegroundOnly: Boolean = false
        private var authHeader: String? = null
        private var uploadPriority: UploadPriority? = null

        fun isForeground(value: Boolean) = apply { this.isForeground = value }
        fun uploadUrl(value: String) = apply { this.uploadUrl = value }
        fun fileURI(value: URI) = apply { this.fileURI = value }
        fun isForegroundOnly(value: Boolean) = apply { this.isForegroundOnly = value }
        fun authHeader(value: String) = apply { this.authHeader = value }
        fun uploadPriority(value: UploadPriority) = apply { this.uploadPriority = value }

        fun build(): UploadRequest = UploadRequest(
            isForeground = isForeground,
            uploadUrl = requireNotNull(uploadUrl) { "uploadUrl is required" },
            fileURI = requireNotNull(fileURI) { "fileURI is required" },
            isForegroundOnly = isForegroundOnly,
            authHeader = requireNotNull(authHeader) { "authHeader is required" },
            uploadPriority = requireNotNull(uploadPriority) { "uploadPriority is required" },
        )
    }
}

sealed class UploadPriority
class Immediate : UploadPriority()
class Deferrable : UploadPriority()
