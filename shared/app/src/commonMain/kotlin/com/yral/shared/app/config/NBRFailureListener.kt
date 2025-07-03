package com.yral.shared.app.config

import co.touchlab.kermit.Logger
import com.yral.shared.libs.arch.data.NetworkBoundResource

internal class NBRFailureListener(private val logger: Logger) :
    NetworkBoundResource.OnFailureListener {
    override fun onFetchFailed(throwable: Throwable) {
        logger.w(throwable) { "onFetchFailed" }
    }
}
