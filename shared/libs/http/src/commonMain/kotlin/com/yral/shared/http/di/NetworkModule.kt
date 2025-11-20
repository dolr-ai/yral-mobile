package com.yral.shared.http.di

import co.touchlab.kermit.LogWriter
import com.yral.shared.http.HttpLogger
import com.yral.shared.http.createClient
import com.yral.shared.http.createClientJson
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.module

expect fun Scope.platformContext(): Any

val networkModule =
    module {
        single { createClientJson() }
        single {
            createClient(
                platformContext(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        single {
            val additionalWriter: LogWriter? = getOrNull(named("httpLogWriter"))
            HttpLogger(
                baseLogger = get(),
                additionalLogWriter = additionalWriter,
            )
        }
    }
