package com.yral.shared.rust.service.domain.models

import com.yral.shared.uniffi.generated.Principal

sealed class UserAccountType {
    data class MainAccount(
        val bots: List<Principal>,
    ) : UserAccountType()

    data class BotAccount(
        val owner: Principal,
    ) : UserAccountType()
}
