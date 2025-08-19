package com.yral.featureflag.core

sealed class FlagResult<out T> {
    data class Sourced<T>(
        val value: T,
    ) : FlagResult<T>()
    data object NotSet : FlagResult<Nothing>()
}
