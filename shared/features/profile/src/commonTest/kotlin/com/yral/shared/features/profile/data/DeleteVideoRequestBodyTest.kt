package com.yral.shared.features.profile.data

import com.yral.shared.analytics.events.VideoDeleteCTA
import com.yral.shared.data.domain.models.FeedDetails
import com.yral.shared.features.profile.domain.models.DeleteVideoRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteVideoRequestBodyTest {
    @Test
    fun deleteVideoRequestBodyUsesPostIdAndVideoIdFromFeedDetails() {
        val body =
            DeleteVideoRequest(
                feedDetails =
                    FeedDetails(
                        postID = "post-123",
                        videoID = "video-456",
                        canisterID = "canister-id",
                        principalID = "principal-id",
                        url = "https://example.com/video.mp4",
                        hashtags = emptyList(),
                        thumbnail = "https://example.com/thumb.jpg",
                        viewCount = 0u,
                        displayName = "Test User",
                        postDescription = "Test video",
                        profileImageURL = null,
                        likeCount = 0u,
                        isLiked = false,
                        nsfwProbability = null,
                        isFollowing = false,
                        isFromServiceCanister = false,
                        userName = "testuser",
                        isDraft = false,
                    ),
                ctaType = VideoDeleteCTA.PROFILE_THUMBNAIL,
            ).toDeleteVideoRequestBody(
                principal = "publisher-principal",
            )

        assertEquals("publisher-principal", body.principal)
        assertEquals("post-123", body.postId)
        assertEquals("video-456", body.videoId)
    }
}
