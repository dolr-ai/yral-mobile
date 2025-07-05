package com.yral.shared.features.profile.di

import com.yral.shared.features.profile.domain.GetProfileVideosUseCase
import com.yral.shared.features.profile.viewmodel.ProfileViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profileModule =
    module {
        factoryOf(::GetProfileVideosUseCase)
        viewModelOf(::ProfileViewModel)
    }
