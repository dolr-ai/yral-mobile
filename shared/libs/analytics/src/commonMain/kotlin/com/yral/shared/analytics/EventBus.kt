package com.yral.shared.analytics

import com.yral.shared.analytics.events.EventData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class EventBus {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _events = MutableSharedFlow<EventData>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<EventData> = _events.asSharedFlow()

    fun publish(event: EventData) {
        scope.launch {
            _events.emit(event)
        }
    }
}
