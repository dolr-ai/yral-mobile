package com.yral.shared.rust

class RustGreeting {
    fun greet(name: String): String {
        return "Hello, ${name}! from rustLib"
    }
}