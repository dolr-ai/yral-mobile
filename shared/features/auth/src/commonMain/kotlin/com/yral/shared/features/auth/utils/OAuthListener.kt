package com.yral.shared.features.auth.utils

import com.yral.shared.core.exceptions.YralException
import com.yral.shared.uniffi.generated.FfiException

interface OAuthListener {
    fun yralException(e: YralException)
    fun ffiException(e: FfiException)
}
