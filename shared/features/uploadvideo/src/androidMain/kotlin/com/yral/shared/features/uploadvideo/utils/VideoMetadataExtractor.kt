package com.yral.shared.features.uploadvideo.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import co.touchlab.kermit.Logger
import java.io.File

class VideoMetadataExtractor {
    /**
     * Get video duration in seconds from a file path
     */
    fun getVideoDuration(filePath: String): Double? =
        runCatching {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            @Suppress("MagicNumber")
            durationStr?.toDoubleOrNull()?.div(1000.0) // Convert milliseconds to seconds
        }.onFailure { exception ->
            Logger.e("Error getting video duration for: $filePath", exception)
        }.getOrNull()

    /**
     * Get video duration in seconds from a URI
     */
    fun getVideoDuration(
        context: Context,
        uri: Uri,
    ): Double? =
        runCatching {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            @Suppress("MagicNumber")
            durationStr?.toDoubleOrNull()?.div(1000.0) // Convert milliseconds to seconds
        }.onFailure { exception ->
            Logger.e("Error getting video duration for URI: $uri", exception)
        }.getOrNull()

    /**
     * Get video file size from a URI
     */
    fun getVideoFileSize(
        context: Context,
        uri: Uri,
    ): Long? =
        runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { fileDescriptor ->
                fileDescriptor.statSize
            }
        }.onFailure { exception ->
            Logger.e("Error getting video file size for URI: $uri", exception)
        }.getOrNull()

    fun getVideoFileSize(filePath: String): Long =
        runCatching {
            File(filePath).length()
        }.onFailure { exception ->
            Logger.e("Permission denied accessing file: $filePath", exception)
        }.getOrDefault(0L)
}
