package com.yral.shared.core.session

import kotlinx.serialization.Serializable

@Serializable
data class AccountDirectoryCache(
    val mainPrincipal: String?,
    val botPrincipals: List<String>,
    val profiles: List<AccountDirectoryProfileCache>,
) {
    fun toAccountDirectory(): AccountDirectory =
        AccountDirectory(
            mainPrincipal = mainPrincipal,
            botPrincipals = botPrincipals,
            profilesByPrincipal = profiles.associateBy({ it.principal }, { it.toProfile() }),
        )

    companion object {
        fun from(directory: AccountDirectory): AccountDirectoryCache =
            AccountDirectoryCache(
                mainPrincipal = directory.mainPrincipal,
                botPrincipals = directory.botPrincipals,
                profiles =
                    directory.profilesByPrincipal.values.map { profile ->
                        AccountDirectoryProfileCache(
                            principal = profile.principal,
                            username = profile.username,
                            avatarUrl = profile.avatarUrl,
                            isBot = profile.isBot,
                        )
                    },
            )
    }
}

@Serializable
data class AccountDirectoryProfileCache(
    val principal: String,
    val username: String,
    val avatarUrl: String,
    val isBot: Boolean,
) {
    fun toProfile(): AccountDirectoryProfile =
        AccountDirectoryProfile(
            principal = principal,
            username = username,
            avatarUrl = avatarUrl,
            isBot = isBot,
        )
}
