package com.yral.shared.features.tournament.domain.model

data class VoteResult(
    val outcome: VoteOutcome,
    val smiley: VotedSmiley,
    val tournamentWins: Int,
    val tournamentLosses: Int,
    val diamonds: Int,
    val position: Int,
    val diamondDelta: Int? = null,
    val videoEmojis: List<VideoEmoji>? = null,
)

enum class VoteOutcome {
    WIN,
    LOSS,
    ;

    companion object {
        fun fromString(value: String): VoteOutcome =
            when (value.uppercase()) {
                "WIN" -> WIN
                else -> LOSS
            }
    }
}

data class VotedSmiley(
    val id: String,
    val unicode: String?,
    val displayName: String?,
    val imageUrl: String?,
    val isActive: Boolean?,
    val clickAnimation: String?,
    val imageFallback: String?,
)

/**
 * Video-specific emoji from Gemini analysis.
 * Each video can have its own unique set of emojis.
 */
data class VideoEmoji(
    val id: String,
    val unicode: String,
    val displayName: String,
)

/**
 * Result of fetching video-specific emojis.
 */
data class VideoEmojisResult(
    val videoId: String,
    val emojis: List<VideoEmoji>,
    val isCustom: Boolean,
)
