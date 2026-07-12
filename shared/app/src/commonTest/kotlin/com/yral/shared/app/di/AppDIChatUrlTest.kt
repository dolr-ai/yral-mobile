package com.yral.shared.app.di

import com.yral.shared.analytics.di.IS_DEBUG
import com.yral.shared.core.AppConfigurations
import com.yral.shared.core.di.CHAT_SERVER_BASE_URL
import com.yral.shared.core.di.COACH_SERVER_BASE_URL
import org.koin.core.qualifier.Qualifier
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

    @Test
    fun coachServerBaseUrl_usesAgentHost() {
        assertEquals(
            AppConfigurations.COACH_BASE_URL,
            resolveBaseUrl(COACH_SERVER_BASE_URL),
        )
    }

    private fun resolveChatBaseUrl(isDebug: Boolean): String = resolveBaseUrl(CHAT_SERVER_BASE_URL, isDebug)

    private fun resolveBaseUrl(
        qualifier: Qualifier,
        isDebug: Boolean = false,
    ): String =
        koinApplication {
            modules(
                module {
                    single<Boolean>(IS_DEBUG) { isDebug }
                },
                featureUrlsModule,
            )
        }.koin.get(qualifier = qualifier)
}
