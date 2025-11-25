package com.yral.shared.libs.arch.domain

interface UseCaseFailureListener {
    fun onFailure(
        throwable: Throwable,
        tag: String? = null,
        message: () -> String,
        exceptionType: UseCaseExceptionType? = null,
    )
}
