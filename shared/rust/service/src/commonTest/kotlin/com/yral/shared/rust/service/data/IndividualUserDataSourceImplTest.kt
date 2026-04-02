package com.yral.shared.rust.service.data

import kotlin.test.Test
import kotlin.test.assertEquals

class IndividualUserDataSourceImplTest {
    @Test
    fun videoUrl_usesCdnHost() {
        assertEquals(
            expected = "https://cdn-yral-sfw.yral.com/publisher-principal/video-uid.mp4",
            actual =
                IndividualUserDataSourceImpl.videoUrl(
                    videoUid = "video-uid",
                    publisherUserId = "publisher-principal",
                ),
        )
    }

    @Test
    fun thumbnailUrl_usesCdnHost() {
        assertEquals(
            expected = "https://cdn-yral-sfw.yral.com/publisher-principal/video-uid_thumbnail.png",
            actual =
                IndividualUserDataSourceImpl.thumbnailUrl(
                    videoUid = "video-uid",
                    publisherUserId = "publisher-principal",
                ),
        )
    }
}
