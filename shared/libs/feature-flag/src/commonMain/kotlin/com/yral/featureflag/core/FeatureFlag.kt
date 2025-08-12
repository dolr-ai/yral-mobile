package com.yral.featureflag.core

sealed interface FeatureFlag<T> {
    val key: String
    val name: String
    val description: String
    val defaultValue: T
    val audience: FlagAudience
    val codec: FlagCodec<T>
}

enum class FlagAudience { DEVELOPER, INTERNAL_QA, PUBLIC }
