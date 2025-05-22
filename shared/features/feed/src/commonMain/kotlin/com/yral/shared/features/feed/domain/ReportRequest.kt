package com.yral.shared.features.feed.domain

import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.features.feed.data.models.ReportRequestDto

data class ReportRequest(
    val postId: Long,
    val videoId: String,
    val reason: String,
    val canisterID: String,
    val principal: String,
    val userCanisterId: String,
    val userPrincipal: String,
    val delegatedIdentityWire: KotlinDelegatedIdentityWire,
)

internal fun ReportRequest.toDto(): ReportRequestDto =
    ReportRequestDto(
        postId = postId,
        videoId = videoId,
        reason = reason,
        canisterID = canisterID,
        principal = principal,
        userCanisterId = userCanisterId,
        userPrincipal = userPrincipal,
        delegatedIdentityWire = delegatedIdentityWire,
    )
