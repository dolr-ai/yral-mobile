package com.yral.shared.rust.base.exception

class UnknownException : Exception {
    var text: String? = null
        private set

    constructor(cause: Throwable) : super(cause)
    constructor(text: String) : super("Unknown exception: $text") {
        this.text = text
    }
}
