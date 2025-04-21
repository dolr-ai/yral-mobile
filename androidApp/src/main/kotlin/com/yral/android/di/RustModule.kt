package com.yral.android.di

import com.yral.shared.rust.data.IndividualUserDataSource
import com.yral.shared.rust.data.IndividualUserDataSourceImpl
import com.yral.shared.rust.data.IndividualUserRepositoryImpl
import com.yral.shared.rust.domain.IndividualUserRepository
import com.yral.shared.rust.services.IndividualUserServiceFactory
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal val rustModule =
    module {
        singleOf(::IndividualUserRepositoryImpl) { bind<IndividualUserRepository>() }
        singleOf(::IndividualUserDataSourceImpl) { bind<IndividualUserDataSource>() }
        single { IndividualUserServiceFactory() }
    }
