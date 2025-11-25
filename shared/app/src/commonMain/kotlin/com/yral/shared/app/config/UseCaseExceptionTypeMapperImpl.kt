package com.yral.shared.app.config

import com.yral.shared.crashlytics.core.ExceptionType
import com.yral.shared.libs.arch.domain.UseCaseExceptionType
import com.yral.shared.libs.arch.domain.UseCaseExceptionTypeMapper

internal class UseCaseExceptionTypeMapperImpl : UseCaseExceptionTypeMapper {
    override fun map(useCaseExceptionType: UseCaseExceptionType): Any =
        when (useCaseExceptionType) {
            is UseCaseExceptionType.Unknown -> ExceptionType.UNKNOWN
            is UseCaseExceptionType.Rust -> ExceptionType.RUST
            is UseCaseExceptionType.Auth -> ExceptionType.AUTH
            is UseCaseExceptionType.Feed -> ExceptionType.FEED
            is UseCaseExceptionType.Deeplink -> ExceptionType.DEEPLINK
        }
}
