package com.yral.shared.analytics

data class User(
    val userId: String,
    val canisterId: String,
    val isLoggedIn: Boolean?,
    val isCreator: Boolean?,
    val satsBalance: Double?,
)
