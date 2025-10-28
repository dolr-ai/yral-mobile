package com.yral.shared.features.uploadvideo.utils

import co.touchlab.kermit.Logger
import java.io.File

class VideoPlayerUtils {
    data class WidgetVideoConfig(
        val autoPlay: Boolean = true,
        val loop: Boolean = true,
        val showControls: Boolean = false,
        val muted: Boolean = true, // Widgets should typically be muted
    )

    fun getWidgetVideoConfig(): WidgetVideoConfig =
        WidgetVideoConfig(
            autoPlay = true,
            loop = true,
            showControls = false,
            muted = true,
        )
}

data class VideoFileInfo(
    val path: String,
    val name: String,
    val size: Long,
    val exists: Boolean,
    val formattedSize: String,
)

fun String.toVideoFileInfo(): VideoFileInfo =
    runCatching {
        val file = File(this)
        VideoFileInfo(
            path = this,
            name = file.name,
            size = if (file.exists()) file.length() else 0L,
            exists = file.exists(),
            formattedSize = formatFileSize(if (file.exists()) file.length() else 0L),
        )
    }.onFailure { exception ->
        Logger.e("Error getting video file info for: $this", exception)
    }.getOrElse {
        VideoFileInfo(
            path = this,
            name = File(this).name,
            size = 0L,
            exists = false,
            formattedSize = formatFileSize(0L),
        )
    }
