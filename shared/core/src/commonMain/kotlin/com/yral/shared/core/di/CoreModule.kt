package com.yral.shared.core.di

import com.yral.shared.core.dispatchers.AppDispatchers
import com.yral.shared.core.session.SessionManager
import org.koin.dsl.module

val coreModule =
    module {
        single { AppDispatchers() }
        single { SessionManager() }
    }
