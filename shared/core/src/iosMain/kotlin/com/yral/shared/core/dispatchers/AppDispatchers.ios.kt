package com.yral.shared.core.dispatchers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual fun provideIODispatcher(): CoroutineDispatcher = Dispatchers.Default
