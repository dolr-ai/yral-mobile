package com.yral.shared.iap.utils

internal expect object PackageNameProvider {
    fun getPackageName(): String
}
