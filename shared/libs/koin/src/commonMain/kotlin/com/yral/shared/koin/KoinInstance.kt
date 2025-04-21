package com.yral.shared.koin

import org.koin.core.component.KoinComponent

private object AppKoinComponent : KoinComponent {
    // do-nothing
}

val koinInstance get() = AppKoinComponent.getKoin()
