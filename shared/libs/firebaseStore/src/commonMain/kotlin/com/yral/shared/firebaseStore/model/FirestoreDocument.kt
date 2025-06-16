package com.yral.shared.firebaseStore.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface FirestoreDocument {
    val id: String
}
