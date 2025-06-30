package com.yral.shared.core.dispatchers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Deprecated("use AppDispatchers from coroutines-x")
class AppDispatchers(
    val main: CoroutineDispatcher = Dispatchers.Main,
    val io: CoroutineDispatcher = provideIODispatcher(),
    val mainImmediate: CoroutineDispatcher = Dispatchers.Main.immediate,
)

expect fun provideIODispatcher(): CoroutineDispatcher
