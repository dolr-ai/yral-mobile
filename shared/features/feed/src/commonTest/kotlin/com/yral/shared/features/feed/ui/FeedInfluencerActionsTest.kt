package com.yral.shared.features.feed.ui

import com.yral.shared.data.domain.models.ConversationInfluencerSource
import com.yral.shared.data.domain.models.FeedDetails
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedInfluencerActionsTest {
    @Test
    fun shouldShowInfluencerFeedActionsOnlyForAiInfluencerFeed() {
        assertTrue(feedDetails(isAiInfluencer = true).shouldShowInfluencerFeedActions())
        assertFalse(feedDetails(isAiInfluencer = false).shouldShowInfluencerFeedActions())
        assertFalse(feedDetails(isAiInfluencer = null).shouldShowInfluencerFeedActions())
    }

    @Test
    fun toOpenConversationParamsMapsFeedDetails() {
        val params =
            feedDetails(
                isAiInfluencer = true,
                displayName = "Creator Display",
                userName = "creator",
                profileImageURL = "https://example.com/avatar.png",
            ).toOpenConversationParams()

        assertEquals("publisher-1", params.influencerId)
        assertEquals(ConversationInfluencerSource.CARD, params.influencerSource)
        assertEquals("creator", params.username)
        assertEquals("Creator Display", params.displayName)
        assertEquals("https://example.com/avatar.png", params.avatarUrl)
    }

    @Test
    fun toOpenConversationParamsOmitsBlankDisplayName() {
        val params =
            feedDetails(
                isAiInfluencer = true,
                displayName = "",
                userName = null,
                profileImageURL = null,
            ).toOpenConversationParams()

        assertNull(params.displayName)
        assertNull(params.username)
        assertNull(params.avatarUrl)
    }

    private fun feedDetails(
        isAiInfluencer: Boolean?,
        displayName: String = "Creator",
        userName: String? = "creator",
        profileImageURL: String? = "profile",
    ): FeedDetails =
        FeedDetails(
            postID = "post-1",
            videoID = "video-1",
            canisterID = "canister-1",
            principalID = "publisher-1",
            url = "https://example.com/video.mp4",
            hashtags = emptyList(),
            thumbnail = "https://example.com/thumbnail.png",
            viewCount = 0u,
            displayName = displayName,
            postDescription = "",
            profileImageURL = profileImageURL,
            likeCount = 0u,
            isLiked = false,
            nsfwProbability = null,
            isFollowing = false,
            isFromServiceCanister = false,
            userName = userName,
            isAiInfluencer = isAiInfluencer,
        )
}
