package com.yral.shared.core.utils

import com.yral.shared.core.session.AccountInfo
import com.yral.shared.core.session.SessionManager

fun SessionManager.getAccountInfo(): AccountInfo? =
    userPrincipal?.let { principal ->
        profilePic?.let { pic ->
            AccountInfo(
                userPrincipal = principal,
                profilePic = pic,
            )
        }
    }
