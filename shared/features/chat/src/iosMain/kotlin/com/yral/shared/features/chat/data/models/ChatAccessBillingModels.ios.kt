package com.yral.shared.features.chat.data.models

import platform.Foundation.NSBundle

internal actual fun GrantChatAccessRequestDto.toPlatformGrantRequestBody(): Any =
    GrantAppleChatAccessRequestDto(
        transactionId = purchaseToken,
        productId = productId,
        botId = botId,
        environment = appleStoreEnvironment(),
    )

// Debug and TestFlight builds carry a "sandboxReceipt"; App Store builds carry "receipt".
// The billing server looks the transaction up in the matching App Store Server API environment.
private fun appleStoreEnvironment(): String =
    if (NSBundle.mainBundle.appStoreReceiptURL?.lastPathComponent == "sandboxReceipt") {
        "sandbox"
    } else {
        "production"
    }
