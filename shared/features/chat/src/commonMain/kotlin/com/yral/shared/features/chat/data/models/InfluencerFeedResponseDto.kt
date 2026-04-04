package com.yral.shared.features.chat.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InfluencerFeedResponseDto(
    @SerialName("influencers")
    val influencers: List<InfluencerFeedItemDto>,
    @SerialName("total_count")
    val totalCount: Int,
    @SerialName("offset")
    val offset: Int,
    @SerialName("limit")
    val limit: Int,
    @SerialName("has_more")
    val hasMore: Boolean,
    @SerialName("feed_generated_at")
    val feedGeneratedAt: String? = null,
)

@Serializable
data class InfluencerFeedItemDto(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("avatar_url")
    val avatarUrl: String,
    @SerialName("description")
    val description: String,
    @SerialName("category")
    val category: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("scores")
    val scores: InfluencerFeedScoresDto? = null,
    @SerialName("ranking")
    val ranking: InfluencerFeedRankingDto? = null,
    @SerialName("signals")
    val signals: InfluencerFeedSignalsDto? = null,
)

@Serializable
data class InfluencerFeedScoresDto(
    @SerialName("engagement_score")
    val engagementScore: Double? = null,
    @SerialName("discovery_score")
    val discoveryScore: Double? = null,
    @SerialName("momentum_score")
    val momentumScore: Double? = null,
    @SerialName("newness_score")
    val newnessScore: Double? = null,
    @SerialName("underexposure_score")
    val underexposureScore: Double? = null,
    @SerialName("content_activity_score")
    val contentActivityScore: Double? = null,
    @SerialName("depth_quality_score")
    val depthQualityScore: Double? = null,
)

@Serializable
data class InfluencerFeedRankingDto(
    @SerialName("final_rank")
    val finalRank: Int? = null,
    @SerialName("engagement_rank")
    val engagementRank: Int? = null,
    @SerialName("discovery_rank")
    val discoveryRank: Int? = null,
    @SerialName("selected_rank_source")
    val selectedRankSource: String? = null,
    @SerialName("eligible_for_discovery")
    val eligibleForDiscovery: Boolean? = null,
)

@Serializable
data class InfluencerFeedSignalsDto(
    @SerialName("conversation_count")
    val conversationCount: Int? = null,
    @SerialName("message_count")
    val messageCount: Int? = null,
    @SerialName("followers_count")
    val followersCount: Int? = null,
    @SerialName("total_video_views")
    val totalVideoViews: Int? = null,
    @SerialName("share_count_total")
    val shareCountTotal: Int? = null,
    @SerialName("likes_count_total")
    val likesCountTotal: Int? = null,
    @SerialName("avg_watch_pct_mean")
    val avgWatchPctMean: Double? = null,
    @SerialName("ready_post_count")
    val readyPostCount: Int? = null,
    @SerialName("last_post_at")
    val lastPostAt: String? = null,
    @SerialName("depth_ratio")
    val depthRatio: Double? = null,
    @SerialName("share_rate")
    val shareRate: Double? = null,
    @SerialName("views_per_post")
    val viewsPerPost: Double? = null,
    @SerialName("likes_per_post")
    val likesPerPost: Double? = null,
    @SerialName("posting_density")
    val postingDensity: Double? = null,
    @SerialName("conv_velocity")
    val convVelocity: Double? = null,
)
