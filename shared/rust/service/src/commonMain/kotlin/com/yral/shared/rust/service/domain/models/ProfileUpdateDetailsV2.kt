package com.yral.shared.rust.service.domain.models

data class ProfileUpdateDetailsV2(
    val bio: String? = null,
    val websiteUrl: String? = null,
    val profilePictureUrl: String? = null,
)
