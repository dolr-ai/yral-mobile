package com.yral.shared.core

import com.yral.shared.core.platform.Platform
import com.yral.shared.core.platform.getPlatform

class Greeting {
    private val platform: Platform = getPlatform()

    fun greet(): String = "Hello, ${platform.name}!"
}
