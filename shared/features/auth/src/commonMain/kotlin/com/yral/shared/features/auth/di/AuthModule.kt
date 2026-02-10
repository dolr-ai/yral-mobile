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
import com.yral.shared.features.auth.domain.useCases.CreateAiAccountUseCase
import com.yral.shared.features.auth.domain.useCases.DeleteAccountUseCase
import com.yral.shared.features.auth.domain.useCases.DeregisterNotificationTokenUseCase
import com.yral.shared.features.auth.domain.useCases.ExchangePrincipalIdUseCase
import com.yral.shared.features.auth.domain.useCases.ObtainAnonymousIdentityUseCase
import com.yral.shared.features.auth.domain.useCases.PhoneAuthLoginUseCase
import com.yral.shared.features.auth.domain.useCases.RefreshTokenUseCase
import com.yral.shared.features.auth.domain.useCases.RegisterNotificationTokenUseCase
import com.yral.shared.features.auth.domain.useCases.UpdateSessionAsRegisteredUseCase
import com.yral.shared.features.auth.domain.useCases.VerifyPhoneAuthUseCase
import com.yral.shared.features.auth.viewModel.LoginViewModel
import com.yral.shared.libs.phonevalidation.countries.CountryRepository
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.scope.Scope
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
        factoryOf(::PhoneAuthLoginUseCase)
        factoryOf(::VerifyPhoneAuthUseCase)
        factoryOf(::RequiredUseCases)
        factoryOf(::AuthTelemetry)
        factoryOf(::RegisterNotificationTokenUseCase)
        factoryOf(::DeregisterNotificationTokenUseCase)
        factoryOf(::CreateAiAccountUseCase)
        singleOf(::LoginViewModel)
        singleOf(::CountryRepository)
        single { createAuthEnv() }
    }

internal expect fun Scope.createAuthEnv(): AuthEnv

data class AuthEnv(
    val clientId: String,
    val redirectUri: RedirectUri,
) {
    data class RedirectUri(
        val scheme: String,
        val host: String = REDIRECT_URI_HOST,
        val path: String = REDIRECT_URI_PATH,
    ) {
        val uriString = "$scheme://$host$path"
        companion object {
            const val REDIRECT_URI_HOST = "oauth"
            const val REDIRECT_URI_PATH = "/callback"
        }
    }
}
