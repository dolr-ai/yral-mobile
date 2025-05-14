package com.yral.shared.core.exceptions

class YralException : Exception {
    var text: String? = null
        private set

    constructor(cause: Throwable) : super(cause)
    constructor(text: String) : super("Unknown exception: $text") {
        this.text = text
    }
}
