package com.yral.shared.features.auth.di

import com.yral.shared.features.auth.AuthClientFactory
import com.yral.shared.features.auth.DefaultAuthClient.RequiredUseCases
import com.yral.shared.features.auth.DefaultAuthClientFactory
import com.yral.shared.features.auth.analytics.AuthTelemetry
import com.yral.shared.features.auth.data.AuthDataSource
import com.yral.shared.features.auth.data.AuthDataSourceImpl
import com.yral.shared.features.auth.data.AuthRepositoryImpl
import com.yral.shared.features.auth.domain.AuthRepository
import com.yral.shared.features.auth.domain.useCases.AuthenticateTokenUseCase
import com.yral.shared.features.auth.domain.useCases.DeleteAccountUseCase
import com.yral.shared.features.auth.domain.useCases.ExchangePrincipalIdUseCase
import com.yral.shared.features.auth.domain.useCases.ObtainAnonymousIdentityUseCase
import com.yral.shared.features.auth.domain.useCases.RefreshTokenUseCase
import com.yral.shared.features.auth.domain.useCases.UpdateSessionAsRegisteredUseCase
import com.yral.shared.features.auth.utils.OAuthUtils
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val authModule =
    module {
        factoryOf(::DefaultAuthClientFactory) { bind<AuthClientFactory>() }
        factoryOf(::AuthDataSourceImpl) { bind<AuthDataSource>() }
        factoryOf(::AuthenticateTokenUseCase)
        factoryOf(::ObtainAnonymousIdentityUseCase)
        factoryOf(::RefreshTokenUseCase)
        factoryOf(::UpdateSessionAsRegisteredUseCase)
        factoryOf(::ExchangePrincipalIdUseCase)
        factoryOf(::DeleteAccountUseCase)
        factoryOf(::RequiredUseCases)
        factoryOf(::AuthTelemetry)

        // Required single
        // Reason: Verified in Repo, Callback in Repo required once app resumes
        singleOf(::OAuthUtils)
        singleOf(::AuthRepositoryImpl) { bind<AuthRepository>() }
    }
