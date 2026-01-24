package com.yral.shared.iap.providers

import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.Purchase as CorePurchase

data class RestoreResult(
    val purchases: List<CorePurchase>,
    val verificationErrors: List<IAPError> = emptyList(),
)
