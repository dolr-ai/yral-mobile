package com.yral.shared.analytics

interface DistinctIdProvider {
    fun currentDistinctId(): String
}
