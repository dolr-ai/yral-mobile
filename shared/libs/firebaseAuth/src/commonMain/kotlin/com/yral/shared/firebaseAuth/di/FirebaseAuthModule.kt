package com.yral.shared.firebaseAuth.di

import com.yral.shared.firebaseAuth.repository.FBAuthRepository
import com.yral.shared.firebaseAuth.repository.FBAuthRepositoryApi
import com.yral.shared.firebaseAuth.usecase.GetCurrentUserIdUseCase
import com.yral.shared.firebaseAuth.usecase.GetIdTokenUseCase
import com.yral.shared.firebaseAuth.usecase.GetUserAuthDataUseCase
import com.yral.shared.firebaseAuth.usecase.ObserveAuthStateUseCase
import com.yral.shared.firebaseAuth.usecase.SignInAnonymouslyUseCase
import com.yral.shared.firebaseAuth.usecase.SignOutUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val firebaseAuthModule =
    module {
        factory<FBAuthRepositoryApi> { FBAuthRepository() }

        factoryOf(::SignInAnonymouslyUseCase)
        factoryOf(::SignOutUseCase)
        factoryOf(::ObserveAuthStateUseCase)
        factoryOf(::GetCurrentUserIdUseCase)
        factoryOf(::GetIdTokenUseCase)
        factoryOf(::GetUserAuthDataUseCase)
    }
