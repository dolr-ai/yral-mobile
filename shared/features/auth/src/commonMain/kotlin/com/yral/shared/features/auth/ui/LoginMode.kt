package com.yral.shared.features.auth.ui

sealed interface LoginMode {
    data object SOCIAL : LoginMode
    data object PHONE : LoginMode
    data object BOTH : LoginMode
}
