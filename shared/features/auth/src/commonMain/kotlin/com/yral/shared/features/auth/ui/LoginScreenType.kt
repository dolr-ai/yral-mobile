package com.yral.shared.features.auth.ui

sealed interface LoginScreenType {
    data object Overlay : LoginScreenType
    data class BottomSheet(
        val bottomSheetType: LoginBottomSheetType,
    ) : LoginScreenType
}
