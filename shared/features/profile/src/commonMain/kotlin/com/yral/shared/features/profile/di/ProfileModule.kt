package com.yral.shared.features.profile.di

import com.yral.shared.features.profile.viewmodel.ProfileViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profileModule =
    module {
        viewModelOf(::ProfileViewModel)
    }
