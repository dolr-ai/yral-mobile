package com.yral.shared.features.auth.data.models

import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeleteAccountRequestDto(
    @SerialName("delegated_identity_wire") val delegatedIdentity: KotlinDelegatedIdentityWire,
)
