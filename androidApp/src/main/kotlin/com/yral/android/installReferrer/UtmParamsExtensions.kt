package com.yral.android.installReferrer

import com.yral.shared.preferences.UtmParams

fun UtmParams.isEmpty(): Boolean = listOf(source, medium, campaign, term, content).all { it.isNullOrBlank() }

fun UtmParams.isNotEmpty(): Boolean = !isEmpty()
