package com.yral.shared.libs.sharing

@Suppress("ForbiddenComment")
class StubIosShareService : ShareService {
    override suspend fun shareImageWithText(
        imageUrl: String,
        text: String,
    ) {
        // TODO: STUB
    }
}
