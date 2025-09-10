package com.yral.shared.rust.service.utils

data class CanisterData(
    val canisterId: String,
    val userPrincipalId: String,
    val profilePic: String,
    val isCreatedFromServiceCanister: Boolean,
)
