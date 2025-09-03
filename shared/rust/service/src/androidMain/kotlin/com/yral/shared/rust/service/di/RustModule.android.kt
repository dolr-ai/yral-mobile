package com.yral.shared.rust.service.di

import com.yral.shared.rust.service.data.IndividualUserDataSource
import com.yral.shared.rust.service.data.IndividualUserDataSourceImpl
import com.yral.shared.rust.service.data.IndividualUserRepositoryImpl
import com.yral.shared.rust.service.data.RateLimitDataSource
import com.yral.shared.rust.service.data.RateLimitDataSourceImpl
import com.yral.shared.rust.service.data.RateLimitRepositoryImpl
import com.yral.shared.rust.service.domain.IndividualUserRepository
import com.yral.shared.rust.service.domain.RateLimitRepository
import com.yral.shared.rust.service.services.IndividualUserServiceFactory
import com.yral.shared.rust.service.services.RateLimitServiceFactory
import com.yral.shared.rust.service.services.UserPostServiceFactory
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

actual val rustModule: Module =
    module {
        factoryOf(::IndividualUserRepositoryImpl) { bind<IndividualUserRepository>() }
        factoryOf(::IndividualUserDataSourceImpl) { bind<IndividualUserDataSource>() }
        factoryOf(::RateLimitRepositoryImpl) { bind<RateLimitRepository>() }
        factoryOf(::RateLimitDataSourceImpl) { bind<RateLimitDataSource>() }
        single { IndividualUserServiceFactory() }
        single { RateLimitServiceFactory() }
        single { UserPostServiceFactory() }
    }
