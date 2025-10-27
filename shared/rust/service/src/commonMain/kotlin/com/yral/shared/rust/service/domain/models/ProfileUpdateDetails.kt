package com.yral.shared.rust.service.domain.models

data class ProfileUpdateDetails(
    val bio: String? = null,
    val websiteUrl: String? = null,
    val profilePictureUrl: String? = null,
)
