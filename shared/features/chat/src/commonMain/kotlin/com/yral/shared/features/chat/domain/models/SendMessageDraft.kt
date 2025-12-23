package com.yral.shared.features.chat.domain.models

import com.yral.shared.features.chat.attachments.ChatAttachment

data class SendMessageDraft(
    val messageType: ChatMessageType,
    val content: String? = null,
    val mediaAttachments: List<ChatAttachment> = emptyList(),
    val audioAttachment: ChatAttachment? = null,
    val audioDurationSeconds: Int? = null,
)
