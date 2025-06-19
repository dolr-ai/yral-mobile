package com.yral.shared.firebaseStore.model

import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardItemDto(
    override val id: String,
    val coins: Long,
) : FirestoreDocument
