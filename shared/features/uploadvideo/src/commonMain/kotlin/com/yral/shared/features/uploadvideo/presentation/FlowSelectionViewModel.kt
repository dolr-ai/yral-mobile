package com.yral.shared.features.uploadvideo.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FlowSelectionViewModel : ViewModel() {
    private val _state = MutableStateFlow(ViewState())
    val state: StateFlow<ViewState> = _state.asStateFlow()

    fun setFlowType(flowType: FlowType?) {
        _state.update {
            it.copy(flowType = if (it.flowType == flowType) null else flowType)
        }
    }
}

data class ViewState(
    val flowType: FlowType? = null,
)

enum class FlowType {
    AI_VIDEO_GEN,
    UPLOAD_VIDEO,
}
