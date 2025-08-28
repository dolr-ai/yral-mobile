package com.yral.shared.features.auth.utils

import com.yral.shared.core.session.AccountInfo
import com.yral.shared.core.session.SessionManager
import com.yral.shared.rust.service.utils.propicFromPrincipal

fun SessionManager.getAccountInfo(): AccountInfo? =
    userPrincipal?.let {
        AccountInfo(
            userPrincipal = it,
            profilePic = propicFromPrincipal(it),
        )
    }
