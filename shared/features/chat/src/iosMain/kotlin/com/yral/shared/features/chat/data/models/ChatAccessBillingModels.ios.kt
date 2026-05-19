package com.yral.shared.features.chat.data.models

internal actual fun GrantChatAccessRequestDto.toPlatformGrantRequestBody(): Any =
    GrantAppleChatAccessRequestDto(
        transactionId = purchaseToken,
        productId = productId,
        botId = botId,
    )
