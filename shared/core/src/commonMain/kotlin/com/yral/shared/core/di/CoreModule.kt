package com.yral.shared.core.di

import com.yral.shared.core.analytics.AffiliateAttributionStore
import com.yral.shared.core.session.SessionManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val coreModule =
    module {
        single { SessionManager() }
        singleOf(::AffiliateAttributionStore)
    }
