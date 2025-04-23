package com.yral.shared.http

enum class HTTPResponseStatus {
    SUCCESS,
    UNAUTHORISED,
    CLIENT_ERROR,
    SERVER_ERROR,
    INFORMATIONAL,
    REDIRECTION,
    UNKNOWN,
    ;

    @Suppress("MagicNumber")
    companion object {
        fun from(statusCode: Int): HTTPResponseStatus =
            when (statusCode) {
                in 200..299 -> SUCCESS
                401 -> UNAUTHORISED
                in 402..499 -> CLIENT_ERROR
                in 500..599 -> SERVER_ERROR
                in 100..199 -> INFORMATIONAL
                in 300..399 -> REDIRECTION
                else -> UNKNOWN
            }
    }
}
