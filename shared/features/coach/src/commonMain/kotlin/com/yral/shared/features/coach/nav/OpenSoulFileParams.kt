package com.yral.shared.features.coach.nav

import kotlinx.serialization.Serializable

/**
 * Coach pivot Bucket 2 — navigation parameters for the Soul File
 * View/Edit page. Opened from the creator's own AI-influencer profile
 * via an "Edit Soul File" entry next to the existing "Make your AI
 * Influencer better" Coach button.
 */
@Serializable
data class OpenSoulFileParams(
    val botId: String,
    val botName: String? = null,
    val avatarUrl: String? = null,
)
