package com.yral.android.installReferrer

import com.yral.shared.preferences.UtmParams

fun UtmParams.isEmpty(): Boolean =
    source.isNullOrBlank() &&
        medium.isNullOrBlank() &&
        campaign.isNullOrBlank() &&
        term.isNullOrBlank() &&
        content.isNullOrBlank()

fun UtmParams.isNotEmpty(): Boolean = !isEmpty()
