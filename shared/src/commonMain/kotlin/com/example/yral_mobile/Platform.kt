package com.example.yral_mobile

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform


