package com.yral.shared.rust.service.di

import com.yral.shared.rust.service.services.IndividualUserServiceFactory
import com.yral.shared.rust.service.services.RateLimitServiceFactory
import com.yral.shared.rust.service.services.UserPostServiceFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val rustModule: Module =
    module {
        single { IndividualUserServiceFactory() }
        single { RateLimitServiceFactory() }
        single { UserPostServiceFactory() }
    }
