package com.yral.shared.features.feed.data.models

import com.yral.shared.data.domain.models.FeedDetails
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Serializable
internal data class FeedDetailsCache(
    @Serializable(with = InstantAsLongSerializer::class)
    val timestamp: Instant,
    val feedDetails: List<FeedDetailsForCache>,
)

@Serializable
internal data class FeedDetailsForCache(
    val postID: String,
    val videoID: String,
    val canisterID: String,
    val principalID: String,
    val url: String,
    val hashtags: List<String>,
    val thumbnail: String,
    val viewCount: ULong,
    val displayName: String,
    val postDescription: String,
    var profileImageURL: String?,
    var likeCount: ULong,
    var isLiked: Boolean,
    var nsfwProbability: Double?,
    val isFollowing: Boolean,
    val isFromServiceCanister: Boolean,
    val userName: String?,
)

internal fun FeedDetailsForCache.toFeedDetails(): FeedDetails =
    FeedDetails(
        postID = postID,
        videoID = videoID,
        canisterID = canisterID,
        principalID = principalID,
        url = url,
        hashtags = hashtags,
        thumbnail = thumbnail,
        viewCount = viewCount,
        displayName = displayName,
        postDescription = postDescription,
        profileImageURL = profileImageURL,
        likeCount = likeCount,
        isLiked = isLiked,
        nsfwProbability = nsfwProbability,
        isFollowing = isFollowing,
        isFromServiceCanister = isFromServiceCanister,
        userName = userName,
    )

internal fun FeedDetails.toFeedDetailsForCache(): FeedDetailsForCache =
    FeedDetailsForCache(
        postID = postID,
        videoID = videoID,
        canisterID = canisterID,
        principalID = principalID,
        url = url,
        hashtags = hashtags,
        thumbnail = thumbnail,
        viewCount = viewCount,
        displayName = displayName,
        postDescription = postDescription,
        profileImageURL = profileImageURL,
        likeCount = likeCount,
        isLiked = isLiked,
        nsfwProbability = nsfwProbability,
        isFollowing = isFollowing,
        isFromServiceCanister = isFromServiceCanister,
        userName = userName,
    )

@Serializable
@OptIn(ExperimentalTime::class)
internal object InstantAsLongSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)

    override fun serialize(
        encoder: Encoder,
        value: Instant,
    ) {
        encoder.encodeLong(value.toEpochMilliseconds())
    }

    override fun deserialize(decoder: Decoder): Instant = Instant.fromEpochMilliseconds(decoder.decodeLong())
}
