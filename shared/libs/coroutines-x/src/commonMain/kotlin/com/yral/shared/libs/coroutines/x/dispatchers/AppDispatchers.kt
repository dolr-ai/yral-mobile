package com.yral.shared.libs.coroutines.x.dispatchers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

class AppDispatchers
    private constructor(
        val disk: CoroutineDispatcher,
        val network: CoroutineDispatcher,
        val main: CoroutineDispatcher,
        val cpu: CoroutineDispatcher,
        val unconfined: CoroutineDispatcher,
        val mainImmediate: CoroutineDispatcher,
    ) {
        companion object {
            private val instance =
                AppDispatchers(
                    disk = Dispatchers.IO,
                    network = Dispatchers.IO,
                    main = Dispatchers.Main,
                    cpu = Dispatchers.Default,
                    unconfined = Dispatchers.Unconfined,
                    mainImmediate = Dispatchers.Main.immediate,
                )

            operator fun invoke() = instance
        }
    }
