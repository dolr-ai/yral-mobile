package com.yral.shared.features.chat.data.models

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InfluencerFeedMapperTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `recsys influencer feed maps nested signal counts into influencer response`() {
        val response =
            json.decodeFromString<InfluencerFeedResponseDto>(
                """
                {
                  "influencers": [
                    {
                      "id": "bot-1",
                      "name": "bot-name",
                      "display_name": "Bot Name",
                      "avatar_url": "https://example.com/avatar.png",
                      "description": "about bot",
                      "category": "fitness",
                      "created_at": "2026-04-02T14:11:34.355Z",
                      "scores": {
                        "engagement_score": 1.5,
                        "discovery_score": 2.5
                      },
                      "ranking": {
                        "final_rank": 2,
                        "selected_rank_source": "discovery"
                      },
                      "signals": {
                        "conversation_count": 12,
                        "message_count": 34
                      }
                    }
                  ],
                  "total_count": 25,
                  "offset": 10,
                  "limit": 5,
                  "has_more": true,
                  "feed_generated_at": "2026-04-02T14:11:34.355Z"
                }
                """.trimIndent(),
            )

        val mapped = response.toInfluencersResponseDto()
        val influencer = mapped.influencers.single()

        assertEquals(25, mapped.total)
        assertEquals(10, mapped.offset)
        assertEquals(5, mapped.limit)
        assertEquals(true, mapped.hasMore)
        assertEquals("bot-1", influencer.id)
        assertEquals("Bot Name", influencer.displayName)
        assertEquals("https://example.com/avatar.png", influencer.avatarUrl)
        assertEquals("fitness", influencer.category)
        assertEquals("active", influencer.isActive)
        assertEquals(12, influencer.conversationCount)
        assertEquals(34, influencer.messageCount)
    }

    @Test
    fun `toDomainActiveOnly uses hasMore when present for next offset`() {
        val result =
            InfluencersResponseDto(
                influencers =
                    listOf(
                        InfluencerDto(
                            id = "bot-1",
                            name = "bot-name",
                            displayName = "Bot Name",
                            avatarUrl = "https://example.com/avatar.png",
                            description = "about bot",
                            category = "fitness",
                            isActive = "active",
                            createdAt = "2026-04-02T14:11:34.355Z",
                            conversationCount = 12,
                            messageCount = 34,
                        ),
                    ),
                total = 999,
                limit = 1,
                offset = 20,
                hasMore = true,
            ).toDomainActiveOnly()

        assertEquals(21, result.nextOffset)
    }

    @Test
    fun `toDomainActiveOnly stops pagination when hasMore is false`() {
        val result =
            InfluencersResponseDto(
                influencers =
                    listOf(
                        InfluencerDto(
                            id = "bot-1",
                            name = "bot-name",
                            displayName = "Bot Name",
                            avatarUrl = "https://example.com/avatar.png",
                            description = "about bot",
                            category = "fitness",
                            isActive = "active",
                            createdAt = "2026-04-02T14:11:34.355Z",
                            conversationCount = 12,
                            messageCount = 34,
                        ),
                    ),
                total = 999,
                limit = 1,
                offset = 20,
                hasMore = false,
            ).toDomainActiveOnly()

        assertNull(result.nextOffset)
    }
}
