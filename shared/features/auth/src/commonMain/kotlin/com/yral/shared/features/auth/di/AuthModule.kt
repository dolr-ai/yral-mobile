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
import com.yral.shared.features.auth.domain.useCases.DeregisterNotificationTokenUseCase
import com.yral.shared.features.auth.domain.useCases.ExchangePrincipalIdUseCase
import com.yral.shared.features.auth.domain.useCases.ObtainAnonymousIdentityUseCase
import com.yral.shared.features.auth.domain.useCases.RefreshTokenUseCase
import com.yral.shared.features.auth.domain.useCases.RegisterNotificationTokenUseCase
import com.yral.shared.features.auth.domain.useCases.UpdateSessionAsRegisteredUseCase
import com.yral.shared.features.auth.viewModel.LoginViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val authModule =
    module {
        factoryOf(::DefaultAuthClientFactory) { bind<AuthClientFactory>() }
        factoryOf(::AuthDataSourceImpl) { bind<AuthDataSource>() }
        singleOf(::AuthRepositoryImpl) { bind<AuthRepository>() }
        factoryOf(::AuthenticateTokenUseCase)
        factoryOf(::ObtainAnonymousIdentityUseCase)
        factoryOf(::RefreshTokenUseCase)
        factoryOf(::UpdateSessionAsRegisteredUseCase)
        factoryOf(::ExchangePrincipalIdUseCase)
        factoryOf(::DeleteAccountUseCase)
        factoryOf(::RequiredUseCases)
        factoryOf(::AuthTelemetry)
        factoryOf(::RegisterNotificationTokenUseCase)
        factoryOf(::DeregisterNotificationTokenUseCase)
        singleOf(::LoginViewModel)
    }
