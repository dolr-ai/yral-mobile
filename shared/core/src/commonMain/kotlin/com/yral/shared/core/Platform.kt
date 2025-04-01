package com.yral.shared.core

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform