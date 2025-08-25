package com.yral.shared.features.uploadvideo.presentation

import androidx.lifecycle.ViewModel
import com.yral.shared.analytics.events.VideoCreationType
import com.yral.shared.features.uploadvideo.analytics.UploadVideoTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FlowSelectionViewModel(
    private val uploadVideoTelemetry: UploadVideoTelemetry,
) : ViewModel() {
    private val _state = MutableStateFlow(ViewState())
    val state: StateFlow<ViewState> = _state.asStateFlow()

    fun setFlowType(flowType: FlowType?) {
        val change = if (_state.value.flowType == flowType) null else flowType
        _state.update { it.copy(flowType = change) }
        when (change) {
            FlowType.AI_VIDEO_GEN -> uploadVideoTelemetry.videoUploadTypeSelected(VideoCreationType.AI_VIDEO)
            FlowType.UPLOAD_VIDEO -> uploadVideoTelemetry.videoUploadTypeSelected(VideoCreationType.UPLOAD_VIDEO)
            else -> Unit
        }
    }

    fun pushScreenView() {
        uploadVideoTelemetry.uploadVideoScreenViewed()
    }
}

data class ViewState(
    val flowType: FlowType? = null,
)

enum class FlowType {
    AI_VIDEO_GEN,
    UPLOAD_VIDEO,
}
