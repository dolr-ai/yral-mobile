package com.yral.shared.firebaseStore.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameConfigDto(
    override val id: String,
    @SerialName("available_smileys")
    val availableSmileys: List<GameIconDto>,
    @SerialName("loss_penalty")
    val lossPenalty: Int,
) : FirestoreDocument

@Serializable
data class GameIconDto(
    val id: String,
    @SerialName("image_name")
    val imageName: String,
    @SerialName("image_url")
    val imageUrl: String,
    @SerialName("click_animation")
    val clickAnimation: String,
    @SerialName("is_active")
    val isActive: Boolean,
)
