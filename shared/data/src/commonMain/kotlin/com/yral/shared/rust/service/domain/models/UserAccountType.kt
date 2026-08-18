package com.yral.shared.rust.service.domain.models

sealed class UserAccountType {
    data class MainAccount(
        val bots: List<String>,
    ) : UserAccountType()

    data class BotAccount(
        val owner: String,
    ) : UserAccountType()
}
