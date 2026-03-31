package com.yral.shared.testsupport.usecase

import com.yral.shared.libs.arch.domain.UseCaseFailureListener

class RecordingUseCaseFailureListener : UseCaseFailureListener {
    var lastFailure: Throwable? = null
        private set

    override fun onFailure(
        throwable: Throwable,
        tag: String?,
        message: () -> String,
        exceptionType: String?,
    ) {
        lastFailure = throwable
    }
}
