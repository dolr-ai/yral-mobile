package com.yral.shared.rust.di

import com.yral.shared.rust.data.IndividualUserDataSource
import com.yral.shared.rust.data.IndividualUserDataSourceImpl
import com.yral.shared.rust.data.IndividualUserRepositoryImpl
import com.yral.shared.rust.data.RateLimitDataSource
import com.yral.shared.rust.data.RateLimitDataSourceImpl
import com.yral.shared.rust.data.RateLimitRepositoryImpl
import com.yral.shared.rust.domain.IndividualUserRepository
import com.yral.shared.rust.domain.RateLimitRepository
import com.yral.shared.rust.services.IndividualUserServiceFactory
import com.yral.shared.rust.services.RateLimitServiceFactory
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val rustModule =
    module {
        factoryOf(::IndividualUserRepositoryImpl) { bind<IndividualUserRepository>() }
        factoryOf(::IndividualUserDataSourceImpl) { bind<IndividualUserDataSource>() }
        factoryOf(::RateLimitRepositoryImpl) { bind<RateLimitRepository>() }
        factoryOf(::RateLimitDataSourceImpl) { bind<RateLimitDataSource>() }
        single { IndividualUserServiceFactory() }
        single { RateLimitServiceFactory() }
    }
