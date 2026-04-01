package com.yral.shared.features.chat.viewmodel

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ChatUnreadRefreshSignal {
    private val mutableRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requests: SharedFlow<Unit> = mutableRequests.asSharedFlow()

    fun requestRefresh() {
        mutableRequests.tryEmit(Unit)
    }
}
