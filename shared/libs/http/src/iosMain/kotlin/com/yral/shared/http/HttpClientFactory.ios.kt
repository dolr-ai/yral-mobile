package com.yral.shared.http

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

actual fun platformEngineFactory(): HttpClientEngineFactory<*> = Darwin

actual fun platformApplyEngineConfig(
    context: Any,
    config: HttpClientConfig<*>,
    httpEventListener: HTTPEventListener,
) {
    // No-op: Darwin engine uses system DNS; keep everything else from commonMain
}
