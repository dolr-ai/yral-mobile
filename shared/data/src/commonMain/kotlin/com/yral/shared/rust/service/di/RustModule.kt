package com.yral.shared.rust.service.di

import com.yral.shared.rust.service.data.IndividualUserDataSource
import com.yral.shared.rust.service.data.IndividualUserDataSourceImpl
import com.yral.shared.rust.service.data.IndividualUserRepositoryImpl
import com.yral.shared.rust.service.data.UserInfoDataSource
import com.yral.shared.rust.service.data.UserInfoDataSourceImpl
import com.yral.shared.rust.service.data.UserInfoRepositoryImpl
import com.yral.shared.rust.service.domain.IndividualUserRepository
import com.yral.shared.rust.service.domain.UserInfoRepository
import com.yral.shared.rust.service.domain.pagedDataSource.UserInfoPagingSourceFactory
import com.yral.shared.rust.service.domain.performance.FirebaseRustApiTracer
import com.yral.shared.rust.service.domain.performance.RustApiPerformanceTracer
import com.yral.shared.rust.service.domain.usecases.AcceptNewUserRegistrationV2UseCase
import com.yral.shared.rust.service.domain.usecases.FollowUserUseCase
import com.yral.shared.rust.service.domain.usecases.GetUserProfileDetailsV7UseCase
import com.yral.shared.rust.service.domain.usecases.GetUsersProfileDetailsUseCase
import com.yral.shared.rust.service.domain.usecases.UnfollowUserUseCase
import com.yral.shared.rust.service.domain.usecases.UpdateProfileDetailsUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val rustModule: Module =
    module {
        // Individual User Service
        factoryOf(::IndividualUserRepositoryImpl) { bind<IndividualUserRepository>() }
        factoryOf(::IndividualUserDataSourceImpl) { bind<IndividualUserDataSource>() }

        // User Info Service
        factoryOf(::UserInfoDataSourceImpl) { bind<UserInfoDataSource>() }
        factoryOf(::UserInfoRepositoryImpl) { bind<UserInfoRepository>() }

        // User Info Use Cases
        factoryOf(::FollowUserUseCase)
        factoryOf(::UnfollowUserUseCase)
        factoryOf(::GetUserProfileDetailsV7UseCase)
        factoryOf(::GetUsersProfileDetailsUseCase)
        factoryOf(::UpdateProfileDetailsUseCase)
        factoryOf(::AcceptNewUserRegistrationV2UseCase)

        // Paging Data Sources Factory
        factoryOf(::UserInfoPagingSourceFactory)

        singleOf(::FirebaseRustApiTracer) bind RustApiPerformanceTracer::class
    }
