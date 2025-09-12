package com.yral.shared.app.di

import com.yral.shared.libs.sharing.LinkGenerator
import com.yral.shared.libs.sharing.ShareService
import com.yral.shared.libs.sharing.StubBranchLinkGenerator
import com.yral.shared.libs.sharing.StubIosShareService
import org.koin.core.scope.Scope

actual fun Scope.createShareService(): ShareService = StubIosShareService()

actual fun Scope.createLinkGenerator(): LinkGenerator = StubBranchLinkGenerator()
