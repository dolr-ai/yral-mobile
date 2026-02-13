package com.yral.shared.features.chat.domain.models

sealed class ChatError {
    abstract val code: ChatErrorCode
    abstract val throwable: Throwable?
    abstract val isRetryable: Boolean
    abstract val retry: () -> Unit

    data class NetworkError(
        override val throwable: Throwable? = null,
        override val retry: () -> Unit,
    ) : ChatError() {
        override val code = ChatErrorCode.NETWORK_ERROR
        override val isRetryable = true
    }

    data class AuthenticationError(
        override val throwable: Throwable? = null,
        override val retry: () -> Unit = {},
    ) : ChatError() {
        override val code = ChatErrorCode.AUTH_ERROR
        override val isRetryable = false
    }

    data class ServerError(
        override val throwable: Throwable? = null,
        override val retry: () -> Unit,
    ) : ChatError() {
        override val code = ChatErrorCode.SERVER_ERROR
        override val isRetryable = true
    }

    data class UnknownError(
        override val throwable: Throwable? = null,
        override val retry: () -> Unit,
    ) : ChatError() {
        override val code = ChatErrorCode.UNKNOWN_ERROR
        override val isRetryable = true
    }
}

enum class ChatErrorCode {
    NETWORK_ERROR,
    AUTH_ERROR,
    SERVER_ERROR,
    UNKNOWN_ERROR,
}
