@file:Suppress("MagicNumber")

package com.yral.shared.features.feed.data.models

import com.yral.shared.http.createClientJson
import com.yral.shared.rust.service.domain.models.toPartialFeedDetails
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AIPostResponseDTOTest {
    private val json = createClientJson()

    @Test
    fun parsesRecommendWithMetadataResponse() {
        val dto =
            json.decodeFromString<AIPostResponseDTO>(
                """
                {
                  "user_id": "viewer-1",
                  "videos": [
                    {
                      "video_id": "video-1",
                      "canister_id": "canister-1",
                      "post_id": "post-1",
                      "publisher_user_id": "publisher-1",
                      "num_views_loggedin": 7,
                      "num_views_all": 42,
                      "from_ai_influencer": true,
                      "is_following": true,
                      "username": "creator",
                      "is_pro_user": true,
                      "profile_image_url": "https://example.com/profile.png"
                    }
                  ],
                  "count": 1,
                  "sources": {
                    "mixed": 1
                  },
                  "timestamp": 123456789
                }
                """.trimIndent(),
            )

        val post = dto.toPostResponse().posts.single()

        assertEquals("viewer-1", dto.userId)
        assertEquals(1, dto.count)
        assertEquals(1, dto.sources["mixed"])
        assertEquals(123456789L, dto.timestamp)
        assertEquals("video-1", post.videoID)
        assertEquals("canister-1", post.canisterID)
        assertEquals("post-1", post.postID)
        assertEquals("publisher-1", post.publisherUserId)
        assertEquals(7uL, post.numViewsLoggedIn)
        assertEquals(42uL, post.numViewsAll)
        assertTrue(post.fromAiInfluencer == true)
        assertTrue(post.isFollowing == true)
        assertEquals("creator", post.username)
        assertTrue(post.isProUser == true)
        assertEquals("https://example.com/profile.png", post.profileImageUrl)
    }

    @Test
    fun defaultsOptionalMetadataWhenResponseOmitsIt() {
        val dto =
            json.decodeFromString<AIPostResponseDTO>(
                """
                {
                  "user_id": "viewer-1",
                  "videos": [
                    {
                      "video_id": "video-1",
                      "canister_id": "canister-1",
                      "post_id": "post-1",
                      "publisher_user_id": "publisher-1"
                    }
                  ],
                  "count": 1
                }
                """.trimIndent(),
            )

        val post = dto.toPostResponse().posts.single()

        assertEquals(emptyMap(), dto.sources)
        assertEquals(0L, dto.timestamp)
        assertNull(post.fromAiInfluencer)
        assertNull(post.isFollowing)
        assertNull(post.username)
        assertNull(post.isProUser)
        assertNull(post.profileImageUrl)
    }

    @Test
    fun mapsMetadataIntoPartialFeedDetails() {
        val post =
            json
                .decodeFromString<AIPostResponseDTO>(
                    """
                    {
                      "user_id": "viewer-1",
                      "videos": [
                        {
                          "video_id": "video-1",
                          "canister_id": "canister-1",
                          "post_id": "post-1",
                          "publisher_user_id": "publisher-1",
                          "num_views_all": 42,
                          "from_ai_influencer": true,
                          "is_following": true,
                          "username": "creator",
                          "is_pro_user": true,
                          "profile_image_url": "https://example.com/profile.png"
                        }
                      ],
                      "count": 1,
                      "sources": {},
                      "timestamp": 123456789
                    }
                    """.trimIndent(),
                ).toPostResponse()
                .posts
                .single()

        val details =
            post.toPartialFeedDetails(
                isFromServiceCanister = false,
                profileImageUrlFallback = "fallback-profile",
            )

        assertEquals("creator", details.displayName)
        assertEquals("creator", details.userName)
        assertEquals("https://example.com/profile.png", details.profileImageURL)
        assertEquals(42uL, details.viewCount)
        assertTrue(details.isFollowing)
        assertTrue(details.isProUser)
        assertTrue(details.isAiInfluencer == true)
    }

    @Test
    fun mapsNullMetadataIntoSafePartialFeedDefaults() {
        val post =
            json
                .decodeFromString<AIPostResponseDTO>(
                    """
                    {
                      "user_id": "viewer-1",
                      "videos": [
                        {
                          "video_id": "video-1",
                          "canister_id": "canister-1",
                          "post_id": "post-1",
                          "publisher_user_id": "publisher-1",
                          "username": null,
                          "profile_image_url": null
                        }
                      ],
                      "count": 1
                    }
                    """.trimIndent(),
                ).toPostResponse()
                .posts
                .single()

        val details =
            post.toPartialFeedDetails(
                isFromServiceCanister = false,
                profileImageUrlFallback = "fallback-profile",
            )

        assertEquals("", details.displayName)
        assertNull(details.userName)
        assertEquals("fallback-profile", details.profileImageURL)
        assertFalse(details.isFollowing)
        assertFalse(details.isProUser)
        assertNull(details.isAiInfluencer)
    }
}
