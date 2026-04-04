package com.yral.shared.app.di

import com.yral.shared.analytics.di.IS_DEBUG
import com.yral.shared.core.AppConfigurations
import com.yral.shared.core.di.CHAT_SERVER_BASE_URL
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals

class AppDIChatUrlTest {
    @Test
    fun chatServerBaseUrl_usesProdHostWhenDebugIsFalse() {
        assertEquals(
            AppConfigurations.CHAT_BASE_URL,
            resolveChatBaseUrl(isDebug = false),
        )
    }

    @Test
    fun chatServerBaseUrl_usesProdHostWhenDebugIsTrue() {
        assertEquals(
            AppConfigurations.CHAT_BASE_URL,
            resolveChatBaseUrl(isDebug = true),
        )
    }

    private fun resolveChatBaseUrl(isDebug: Boolean): String =
        koinApplication {
            modules(
                module {
                    single<Boolean>(IS_DEBUG) { isDebug }
                },
                featureUrlsModule,
            )
        }.koin.get(qualifier = CHAT_SERVER_BASE_URL)
}
