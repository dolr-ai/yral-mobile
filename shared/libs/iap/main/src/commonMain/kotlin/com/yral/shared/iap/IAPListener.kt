package com.yral.shared.iap

import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.IAPListener as CoreIAPListener

interface IAPListener : CoreIAPListener {
    fun onWarning(message: String)
    fun onRestoreError(error: List<IAPError>)
}
