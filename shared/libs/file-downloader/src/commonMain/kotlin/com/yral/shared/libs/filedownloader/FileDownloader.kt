package com.yral.shared.libs.filedownloader

import com.github.michaelbull.result.Result
import com.yral.shared.core.exceptions.YralException

internal const val TIMEOUT = 5 * 60 * 1000L
internal const val BUFFER_SIZE = 64 * 1024

interface FileDownloader {
    suspend fun downloadFile(
        url: String,
        fileName: String,
        saveToGallery: Boolean = false,
    ): Result<String, YralException>
}
