package com.yral.shared.features.feed.domain.useCases

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FetchFeedDetailsWithCreatorInfoUseCaseTest {
    @Test
    fun resolveAiInfluencerFlagPrefersFeedMetadataWhenPresent() {
        assertTrue(resolveAiInfluencerFlag(feedMetadataFlag = true, profileFlag = false) == true)
        assertFalse(resolveAiInfluencerFlag(feedMetadataFlag = false, profileFlag = true) == true)
    }

    @Test
    fun resolveAiInfluencerFlagFallsBackToProfileWhenFeedMetadataIsMissing() {
        assertTrue(resolveAiInfluencerFlag(feedMetadataFlag = null, profileFlag = true) == true)
        assertFalse(resolveAiInfluencerFlag(feedMetadataFlag = null, profileFlag = false) == true)
        assertNull(resolveAiInfluencerFlag(feedMetadataFlag = null, profileFlag = null))
    }
}
