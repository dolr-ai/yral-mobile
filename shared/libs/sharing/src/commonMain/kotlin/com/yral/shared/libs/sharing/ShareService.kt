package com.yral.shared.libs.sharing

interface ShareService {
    suspend fun shareImageWithText(
        imageUrl: String,
        text: String,
    )

    suspend fun shareText(text: String)
}
