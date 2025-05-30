package com.yral.shared.features.auth.di

import com.yral.shared.features.auth.AuthClient
import com.yral.shared.features.auth.DefaultAuthClient
import com.yral.shared.features.auth.data.AuthDataSource
import com.yral.shared.features.auth.data.AuthDataSourceImpl
import com.yral.shared.features.auth.data.AuthRepositoryImpl
import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.features.auth.domain.useCases.AuthenticateTokenUseCase
import com.yral.shared.features.auth.domain.useCases.ObtainAnonymousIdentityUseCase
import com.yral.shared.features.auth.domain.useCases.RefreshTokenUseCase
import com.yral.shared.features.auth.utils.OAuthUtils
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val authModule =
    module {
        singleOf(::DefaultAuthClient) { bind<AuthClient>() }
        singleOf(::AuthRepositoryImpl) { bind<AuthRepository>() }
        singleOf(::AuthDataSourceImpl) { bind<AuthDataSource>() }

        singleOf(::AuthenticateTokenUseCase)
        singleOf(::ObtainAnonymousIdentityUseCase)
        singleOf(::RefreshTokenUseCase)

        singleOf(::OAuthUtils)
    }
