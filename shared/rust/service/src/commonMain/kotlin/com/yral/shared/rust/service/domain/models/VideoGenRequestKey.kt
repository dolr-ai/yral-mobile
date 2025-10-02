package com.yral.shared.rust.service.domain.models

data class VideoGenRequestKey(
    val principal: String,
    val counter: ULong,
)
