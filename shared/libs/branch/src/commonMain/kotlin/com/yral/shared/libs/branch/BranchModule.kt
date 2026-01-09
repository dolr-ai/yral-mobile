package com.yral.shared.libs.branch

/**
 * Branch SDK module providing Branch dependencies and cinterop bindings.
 *
 * This module centralizes Branch SDK dependencies:
 * - BranchSDK pod (iOS) - provides cinterop bindings via cocoapods.BranchSDK
 * - Branch Android SDK
 * - Google Play Services Ads Identifier
 *
 * Consuming modules can access:
 * - iOS: cocoapods.BranchSDK.* (Branch, BranchEvent, BranchUniversalObject, etc.)
 * - Android: io.branch.referral.* classes
 */
object BranchModule
