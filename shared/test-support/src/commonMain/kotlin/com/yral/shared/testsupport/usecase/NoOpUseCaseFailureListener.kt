package com.yral.shared.testsupport.usecase

import com.yral.shared.libs.arch.domain.UseCaseFailureListener

class NoOpUseCaseFailureListener : UseCaseFailureListener {
    override fun onFailure(
        throwable: Throwable,
        tag: String?,
        message: () -> String,
        exceptionType: String?,
    ) {
        // no-op
    }
}
