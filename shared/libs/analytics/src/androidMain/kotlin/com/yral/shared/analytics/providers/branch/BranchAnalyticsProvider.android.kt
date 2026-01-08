package com.yral.shared.analytics.providers.branch

import android.content.Context
import com.yral.shared.analytics.AnalyticsProvider
import com.yral.shared.analytics.EventToMapConverter
import com.yral.shared.analytics.User
import com.yral.shared.analytics.events.EventData
import com.yral.shared.analytics.events.TokenType
import io.branch.referral.Branch
import io.branch.referral.util.BranchEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual class BranchAnalyticsProvider actual constructor(
    private val eventFilter: (EventData) -> Boolean,
    private val mapConverter: EventToMapConverter,
) : AnalyticsProvider,
    KoinComponent {
    private val context: Context by inject()
    override val name: String = "branch"
    private val branch: Branch = Branch.getInstance()

    override fun shouldTrackEvent(event: EventData): Boolean = eventFilter(event)

    override fun trackEvent(event: EventData) {
        val properties: Map<String, Any?> = mapConverter.toMap(event)
        val branchEvent = BranchEvent(toValidKeyName(event.event))
        properties.forEach { (key, value) ->
            when (value) {
                is String -> branchEvent.addCustomDataProperty(key, value)
                is Number -> branchEvent.addCustomDataProperty(key, value.toString())
                is Boolean -> branchEvent.addCustomDataProperty(key, value.toString())
                else -> value?.let { branchEvent.addCustomDataProperty(key, it.toString()) }
            }
        }
        branchEvent.logEvent(context)
    }

    override fun setUserProperties(user: User) {
        val userProperties: MutableMap<String, Any?> =
            mutableMapOf(
                "is_creator" to (user.isCreator ?: false),
                "is_logged_in" to user.isLoggedIn,
                "wallet_balance" to user.walletBalance,
                "wallet_token_type" to (user.tokenType?.serialName ?: ""),
                "canister_id" to user.canisterId,
                "email_id" to user.emailId,
            )

        // Attach UTM attribution as user-level properties
        val utmParamsMap = user.utmParams?.toMap() ?: emptyMap()

        if (user.isLoggedIn == true) {
            branch.setIdentity(user.userId)
            userProperties["user_id"] = user.userId
            userProperties["visitor_id"] = null
        } else {
            userProperties["visitor_id"] = user.userId
            userProperties["user_id"] = null
        }

        // Set user properties using Branch's setUserData
        (userProperties + utmParamsMap).forEach { (key, value) ->
            value?.let {
                branch.setRequestMetadata(key, it.toString())
            }
        }
    }

    override fun reset() {
        branch.logout()
    }

    override fun applyCommonContext(common: Map<String, Any?>) {
        common.forEach { (key, value) ->
            value?.let {
                branch.setRequestMetadata(key, it.toString())
            }
        }
    }

    override fun flush() {
        // Branch SDK automatically flushes events, but we can force it
    }

    override fun toValidKeyName(key: String): String = key

    val TokenType.serialName: String
        get() =
            when (this) {
                TokenType.CENTS -> "cents"
                TokenType.SATS -> "sats"
                TokenType.YRAL -> "yral"
            }
}
