package com.yral.shared.libs.videoPlayer

import com.yral.shared.libs.videoplayback.ui.VideoSurfaceType
import kotlin.test.Test
import kotlin.test.assertEquals

class YRALReelPlayerTest {
    @Test
    fun scrollingFeedUsesSurfaceViewSurface() {
        assertEquals(VideoSurfaceType.SurfaceView, scrollingFeedSurfaceType())
    }
}
