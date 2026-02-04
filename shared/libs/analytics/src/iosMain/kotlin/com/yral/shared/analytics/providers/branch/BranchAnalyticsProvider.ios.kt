package com.yral.shared.analytics.providers.branch

import cocoapods.BranchSDK.Branch
import cocoapods.BranchSDK.BranchEvent
import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.EventToMapConverter
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.EventData
import com.yral.shared.analytics.events.TokenType
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual class BranchAnalyticsProvider actual constructor(
    private val eventFilter: (EventData) -> Boolean,
    private val mapConverter: EventToMapConverter,
) : AnalyticsProvider {
    override val name: String = "branch"
    private val branch: Branch = Branch.getInstance()

    override fun shouldTrackEvent(event: EventData): Boolean = eventFilter(event)

    override fun trackEvent(event: EventData) {
        val props: Map<String, Any?> = mapConverter.toMap(event)
        val branchEvent = BranchEvent(toValidKeyName(event.event))
        branchEvent.setCustomData(props.mapValues { it.value })
        branchEvent.logEvent()
    }

    override fun setUserProperties(user: User) {
        val superProps: MutableMap<Any?, Any?> =
            mutableMapOf(
                "is_creator" to (user.isCreator ?: false),
                "is_logged_in" to user.isLoggedIn,
                "wallet_balance" to user.walletBalance,
                "wallet_token_type" to (user.tokenType?.serialName ?: ""),
                "canister_id" to user.canisterId,
                "email_id" to user.emailId,
                "pro_status" to (user.proStatus ?: false),
            )

        // Attach UTM attribution as user-level properties
        val utmParamsMap = user.utmParams?.toMap() ?: emptyMap()

        if (user.isLoggedIn ?: false) {
            branch.setIdentity(user.userId)
            superProps["user_id"] = user.userId
            superProps["visitor_id"] = null
        } else {
            superProps["visitor_id"] = user.userId
            superProps["user_id"] = null
        }

        // Set user properties using Branch's setRequestMetadata
        (superProps + utmParamsMap).forEach { (key, value) ->
            value?.let {
                branch.setRequestMetadataKey(key as? String ?: key.toString(), it.toString())
            }
        }
    }

    override fun reset() {
        branch.logout()
    }

    override fun applyCommonContext(common: Map<String, Any?>) {
        common.forEach { (key, value) ->
            value?.let {
                branch.setRequestMetadataKey(key, it.toString())
            }
        }
    }

    override fun flush() {
        // Branch SDK automatically flushes events, but we can force it
    }

    override fun toValidKeyName(key: String) = key

    val TokenType.serialName: String
        get() =
            when (this) {
                TokenType.CENTS -> "cents"
                TokenType.SATS -> "sats"
                TokenType.YRAL -> "yral"
            }
}
