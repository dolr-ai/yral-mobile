package com.yral.shared.libs.filedownloader

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
