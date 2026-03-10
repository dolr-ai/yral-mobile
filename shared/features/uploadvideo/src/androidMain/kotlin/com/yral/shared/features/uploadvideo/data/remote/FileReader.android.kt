package com.yral.shared.features.uploadvideo.data.remote

import java.io.File

internal actual fun readFileBytes(filePath: String): ByteArray = File(filePath).readBytes()
