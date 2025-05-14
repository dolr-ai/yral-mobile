package com.yral.shared.features.auth.utils

import com.yral.shared.core.exceptions.YralException

interface OAuthListener {
    fun setLoading(loading: Boolean)
    fun exception(e: YralException)
}
