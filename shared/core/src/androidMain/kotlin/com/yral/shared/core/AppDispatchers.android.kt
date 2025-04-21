package com.yral.shared.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual fun provideIODispatcher(): CoroutineDispatcher = Dispatchers.IO
