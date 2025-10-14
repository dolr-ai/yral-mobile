package com.yral.shared.core.exceptions

open class YralException : Exception {
    var text: String? = null
        private set

    constructor(cause: Throwable) : super(cause)
    constructor(text: String) : super("Unknown exception: $text") {
        this.text = text
    }
    constructor(message: String?, cause: Throwable?) : super(message, cause) {
        this.text = message
    }
}
