package com.yral.shared.features.auth.utils

import com.yral.shared.core.exceptions.YralException

interface OAuthListener {
    fun exception(e: YralException)
}
