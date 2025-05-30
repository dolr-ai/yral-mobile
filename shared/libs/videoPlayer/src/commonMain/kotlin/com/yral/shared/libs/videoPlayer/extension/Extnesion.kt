package com.yral.shared.libs.videoPlayer.extension

import com.yral.shared.libs.videoPlayer.util.formatInterval
import com.yral.shared.libs.videoPlayer.util.formatMinSec

internal fun Int.formatMinSec(): String = formatMinSec(this)

internal fun Int.formattedInterval(): Int = formatInterval(this)
