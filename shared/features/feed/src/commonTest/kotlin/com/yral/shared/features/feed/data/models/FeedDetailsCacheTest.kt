package com.yral.shared.features.feed.data.models

import kotlin.test.Test
import kotlin.test.assertEquals

class FeedDetailsCacheTest {
    @Test
    fun withHyphenThumbnailSuffix_normalizesOldCdnSuffix() {
        assertEquals(
            expected = "https://cdn-yral-sfw.yral.com/user/video-uid-thumbnail.png",
            actual =
                "https://cdn-yral-sfw.yral.com/user/video-uid_thumbnail.png"
                    .withHyphenThumbnailSuffix(),
        )
    }

    @Test
    fun withHyphenThumbnailSuffix_preservesExternalUrls() {
        val thumbnail = "https://example.com/user/video-uid_thumbnail.png"

        assertEquals(
            expected = thumbnail,
            actual = thumbnail.withHyphenThumbnailSuffix(),
        )
    }
}
