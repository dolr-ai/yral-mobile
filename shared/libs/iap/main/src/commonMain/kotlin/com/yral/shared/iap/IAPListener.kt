package com.yral.shared.iap

import com.yral.shared.iap.core.IAPListener as CoreIAPListener

interface IAPListener : CoreIAPListener {
    fun onWarning(message: String)
}
