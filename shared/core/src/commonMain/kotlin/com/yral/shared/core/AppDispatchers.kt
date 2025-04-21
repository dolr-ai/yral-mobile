package com.yral.shared.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class AppDispatchers(
    val main: CoroutineDispatcher = Dispatchers.Main,
    val io: CoroutineDispatcher = provideIODispatcher(),
    val mainImmediate: CoroutineDispatcher = Dispatchers.Main.immediate,
)

expect fun provideIODispatcher(): CoroutineDispatcher
