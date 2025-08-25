package com.yral.featureflag.core

import kotlinx.serialization.json.Json

// Centralized JSON configuration for all codecs used in feature flags
internal val FeatureFlagsJson: Json =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        encodeDefaults = false
    }
