package com.yral.shared.libs.arch.domain

/**
 * Maps UseCaseExceptionType to app-specific exception type.
 */
interface UseCaseExceptionTypeMapper {
    fun map(type: UseCaseExceptionType): Any
}
