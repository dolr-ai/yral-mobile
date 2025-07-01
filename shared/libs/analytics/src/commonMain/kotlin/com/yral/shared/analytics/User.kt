package com.yral.shared.analytics

enum class UserType {
    NEW,
    EXISTING,
}

public data class User(
    val userId: String,
    val isLoggedIn: Boolean,
    val canisterId: String,
    val isCreator: Boolean?,
    val satsBalance: Double,
)
