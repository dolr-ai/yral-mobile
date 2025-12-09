package com.yral.shared.rust.service.di

import com.yral.shared.rust.service.data.IndividualUserDataSource
import com.yral.shared.rust.service.data.IndividualUserDataSourceImpl
import com.yral.shared.rust.service.data.IndividualUserRepositoryImpl
import com.yral.shared.rust.service.data.RateLimitDataSource
import com.yral.shared.rust.service.data.RateLimitDataSourceImpl
import com.yral.shared.rust.service.data.RateLimitRepositoryImpl
import com.yral.shared.rust.service.data.UserInfoDataSource
import com.yral.shared.rust.service.data.UserInfoDataSourceImpl
import com.yral.shared.rust.service.data.UserInfoRepositoryImpl
import com.yral.shared.rust.service.domain.IndividualUserRepository
import com.yral.shared.rust.service.domain.RateLimitRepository
import com.yral.shared.rust.service.domain.UserInfoRepository
import com.yral.shared.rust.service.domain.pagedDataSource.UserInfoPagingSourceFactory
import com.yral.shared.rust.service.domain.performance.FirebaseRustApiTracer
import com.yral.shared.rust.service.domain.performance.RustApiPerformanceTracer
import com.yral.shared.rust.service.domain.usecases.FollowUserUseCase
import com.yral.shared.rust.service.domain.usecases.GetProfileDetailsV4UseCase
import com.yral.shared.rust.service.domain.usecases.UnfollowUserUseCase
import com.yral.shared.rust.service.domain.usecases.UpdateProfileDetailsUseCase
import com.yral.shared.rust.service.services.ICPLedgerServiceFactory
import com.yral.shared.rust.service.services.IndividualUserServiceFactory
import com.yral.shared.rust.service.services.LogForwardingService
import com.yral.shared.rust.service.services.RateLimitServiceFactory
import com.yral.shared.rust.service.services.SnsLedgerServiceFactory
import com.yral.shared.rust.service.services.UserInfoServiceFactory
import com.yral.shared.rust.service.services.UserPostServiceFactory
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

        // Rate Limit Service
        factoryOf(::RateLimitRepositoryImpl) { bind<RateLimitRepository>() }
        factoryOf(::RateLimitDataSourceImpl) { bind<RateLimitDataSource>() }

        // User Info Service
        factoryOf(::UserInfoDataSourceImpl) { bind<UserInfoDataSource>() }
        factoryOf(::UserInfoRepositoryImpl) { bind<UserInfoRepository>() }

        // Service Factories
        single { IndividualUserServiceFactory() }
        single { RateLimitServiceFactory() }
        single { UserPostServiceFactory() }
        single { UserInfoServiceFactory() }
        single { SnsLedgerServiceFactory() }
        single { ICPLedgerServiceFactory() }

        // User Info Use Cases
        factoryOf(::FollowUserUseCase)
        factoryOf(::UnfollowUserUseCase)
        factoryOf(::GetProfileDetailsV4UseCase)
        factoryOf(::UpdateProfileDetailsUseCase)

        // Paging Data Sources Factory
        factoryOf(::UserInfoPagingSourceFactory)

        singleOf(::LogForwardingService)
        singleOf(::FirebaseRustApiTracer) bind RustApiPerformanceTracer::class
    }
