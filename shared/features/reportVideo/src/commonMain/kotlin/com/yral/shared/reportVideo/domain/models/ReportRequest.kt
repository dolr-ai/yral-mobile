package com.yral.shared.reportVideo.domain.models

import com.yral.shared.core.rust.KotlinDelegatedIdentityWire
import com.yral.shared.reportVideo.data.models.ReportRequestDto

data class ReportRequest(
    val postId: String,
    val videoId: String,
    val reason: String,
    val canisterID: String,
    val principal: String,
    val userCanisterId: String,
    val userPrincipal: String,
    val delegatedIdentityWire: KotlinDelegatedIdentityWire,
)

fun ReportRequest.toDto(): ReportRequestDto =
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
