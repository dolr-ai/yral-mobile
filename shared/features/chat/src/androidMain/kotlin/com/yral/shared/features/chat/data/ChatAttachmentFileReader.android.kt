package com.yral.shared.features.chat.data

import java.io.File

internal actual fun readChatAttachmentBytes(filePath: String): ByteArray = File(filePath).readBytes()
