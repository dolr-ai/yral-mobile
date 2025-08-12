package com.yral.shared.core.di

import com.yral.shared.core.session.SessionManager
import org.koin.dsl.module

val coreModule =
    module {
        single { SessionManager() }
    }
