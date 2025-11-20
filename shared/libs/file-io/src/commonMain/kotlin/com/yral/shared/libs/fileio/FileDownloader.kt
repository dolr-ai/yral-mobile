package com.yral.shared.libs.fileio

import com.github.michaelbull.result.Result
import com.yral.shared.core.exceptions.YralException

interface FileDownloader {
    suspend fun downloadFile(
        url: String,
        fileName: String,
        saveToGallery: Boolean = false,
    ): Result<String, YralException>
}

enum class FileType {
    VIDEO,
    IMAGE,
    DOCUMENT,
    OTHER,
}

fun String.getFileType(): FileType {
    val extension = this.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "mp4", "mov", "avi", "mkv", "webm", "m4v" -> FileType.VIDEO
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic" -> FileType.IMAGE
        "pdf", "doc", "docx", "txt", "csv", "xls", "xlsx" -> FileType.DOCUMENT
        else -> FileType.OTHER
    }
}
