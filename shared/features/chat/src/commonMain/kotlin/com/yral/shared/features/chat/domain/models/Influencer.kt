package com.yral.shared.features.chat.domain.models

enum class InfluencerStatus(
    val value: String,
) {
    ACTIVE("active"),
    COMING_SOON("coming_soon"),
    DISCONTINUED("discontinued"),
    UNKNOWN(""),
    ;

    companion object {
        fun fromString(value: String): InfluencerStatus =
            entries
                .firstOrNull { it.value == value.trim() }
                ?: UNKNOWN
    }
}

data class Influencer(
    val id: String,
    val name: String,
    val displayName: String,
    val avatarUrl: String,
    val description: String,
    val category: String,
    val status: InfluencerStatus,
    val createdAt: String,
    val conversationCount: Int?,
)
