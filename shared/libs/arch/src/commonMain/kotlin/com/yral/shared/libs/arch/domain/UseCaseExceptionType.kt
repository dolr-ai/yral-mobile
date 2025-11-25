package com.yral.shared.libs.arch.domain

/**
 * Exception type categories for use cases.
 */
sealed class UseCaseExceptionType {
    object Unknown : UseCaseExceptionType()
    object Rust : UseCaseExceptionType()
    object Auth : UseCaseExceptionType()
    object Feed : UseCaseExceptionType()
    object Deeplink : UseCaseExceptionType()
}
