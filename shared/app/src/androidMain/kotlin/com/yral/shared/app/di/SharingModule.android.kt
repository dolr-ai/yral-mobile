package com.yral.shared.app.di

import com.yral.shared.libs.sharing.AndroidShareService
import com.yral.shared.libs.sharing.BranchLinkGenerator
import com.yral.shared.libs.sharing.LinkGenerator
import com.yral.shared.libs.sharing.ShareService
import org.koin.core.module.dsl.new
import org.koin.core.scope.Scope

actual fun Scope.createShareService(): ShareService = new(::AndroidShareService)

actual fun Scope.createLinkGenerator(): LinkGenerator = new(::BranchLinkGenerator)
