package com.yral.shared.features.chat.domain.models

import com.yral.shared.features.chat.attachments.ChatAttachment

data class SendMessageDraft(
    val messageType: ChatMessageType,
    val content: String? = null,
    val mediaAttachments: List<ChatAttachment> = emptyList(),
    val audioAttachment: ChatAttachment? = null,
    val audioDurationSeconds: Int? = null,
    // COLLAGE drafts: reference to the influencer photo collage being shared.
    // The message never carries image URLs (hard backend requirement).
    val collageId: String? = null,
    val collageBotId: String? = null,
    val collageDate: String? = null,
)
