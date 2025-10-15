package com.yral.shared.app.di

import com.yral.shared.libs.sharing.IosBranchLinkGenerator
import com.yral.shared.libs.sharing.IosShareService
import com.yral.shared.libs.sharing.LinkGenerator
import com.yral.shared.libs.sharing.ShareService
import org.koin.core.scope.Scope

actual fun Scope.createShareService(): ShareService = IosShareService(get())

actual fun Scope.createLinkGenerator(): LinkGenerator = IosBranchLinkGenerator()
