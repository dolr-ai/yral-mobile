package com.yral.shared.features.chat.domain

import co.touchlab.kermit.Logger
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.yral.featureflag.AppFeatureFlags
import com.yral.featureflag.FeatureFlagManager
import com.yral.shared.core.session.SessionManager
import com.yral.shared.core.session.SessionState
import com.yral.shared.features.chat.analytics.ChatTelemetry
import com.yral.shared.features.chat.domain.usecases.CheckChatAccessUseCase
import com.yral.shared.features.chat.domain.usecases.GrantChatAccessParams
import com.yral.shared.features.chat.domain.usecases.GrantChatAccessUseCase
import com.yral.shared.iap.IAPManager
import com.yral.shared.iap.core.model.Purchase
import com.yral.shared.iap.core.model.PurchaseState
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Launch-time safety net for store purchases the billing backend never learned
 * about — e.g. the app died between the Play purchase and the grant/verify
 * call, and the user never re-entered that bot's chat screen (where
 * ConversationViewModel's retryUngrantedChatAccess would normally self-heal).
 *
 * Once per app start, after the session is signed in, it restores owned
 * purchases and re-sends the grant for any PURCHASED purchase that maps to a
 * bot in [BotSubscriptionCatalog] but has no backend access. Products the
 * catalog cannot map back to a bot are skipped ([com.yral.shared.iap.core.model.ProductId.DAILY_CHAT]
 * is one product shared by many bots; yral_pro is not chat).
 *
 * Failures are logged and reported via telemetry only; the sweep runs on a
 * background scope and must never affect startup.
 */
class UngrantedChatPurchaseSweep(
    private val sessionManager: SessionManager,
    private val flagManager: FeatureFlagManager,
    private val iapManager: IAPManager,
    private val checkChatAccessUseCase: CheckChatAccessUseCase,
    private val grantChatAccessUseCase: GrantChatAccessUseCase,
    private val chatTelemetry: ChatTelemetry,
    appDispatchers: AppDispatchers,
) {
    private val scope = CoroutineScope(SupervisorJob() + appDispatchers.network)
    private var started = false

    /**
     * Idempotent; safe to call from Application.onCreate. Waits for the first
     * signed-in session, then runs the sweep once for this process.
     *
     * Wired on Android only: the iOS restorePurchases path goes through
     * StoreKit's restoreCompletedTransactions, which can prompt for App Store
     * credentials — not acceptable on every launch.
     */
    fun start() {
        if (started) return
        started = true
        scope.launch {
            runCatching {
                awaitSignedInSession()
                sweep()
            }.onFailure { error ->
                Logger.e(TAG, error) { "launch sweep failed" }
            }
        }
    }

    private suspend fun awaitSignedInSession() {
        sessionManager.observeSessionState { it }.first { it is SessionState.SignedIn }
        // Post-login the app may auto-switch to the last active account;
        // wait for that to settle so access is granted to the right principal.
        delay(SESSION_SETTLE_DELAY_MS)
        sessionManager.observeSessionState { it }.first { it is SessionState.SignedIn }
    }

    private suspend fun sweep() {
        if (!flagManager.isEnabled(AppFeatureFlags.Common.EnableSubscription)) return
        val purchases =
            iapManager
                .restorePurchases()
                .getOrElse { error ->
                    Logger.w(TAG) { "launch sweep: restorePurchases failed: ${error.message}" }
                    return
                }.purchases
        val candidates =
            purchases
                .filter { it.state == PurchaseState.PURCHASED && it.purchaseToken != null }
                .mapNotNull { purchase ->
                    val productId = purchase.productId ?: return@mapNotNull null
                    val botId = BotSubscriptionCatalog.botIdFor(productId) ?: return@mapNotNull null
                    botId to purchase
                }.distinctBy { (_, purchase) -> purchase.purchaseToken }
        Logger.d(TAG) {
            "launch sweep: restored=${purchases.size} catalogCandidates=${candidates.size}"
        }
        candidates.forEach { (botId, purchase) -> recoverIfUngranted(botId, purchase) }
    }

    private suspend fun recoverIfUngranted(
        botId: String,
        purchase: Purchase,
    ) {
        checkChatAccessUseCase(botId)
            .onSuccess { status ->
                if (status.hasAccess) {
                    Logger.d(TAG) { "launch sweep: access already granted for bot=$botId" }
                } else {
                    grant(botId, purchase)
                }
            }.onFailure { error ->
                // Can't tell whether access exists; stay quiet and let the next
                // launch (or the chat screen retry) handle it.
                Logger.w(TAG) { "launch sweep: checkChatAccess failed for bot=$botId: ${error.message}" }
            }
    }

    private suspend fun grant(
        botId: String,
        purchase: Purchase,
    ) {
        val productId = purchase.productId?.productId ?: return
        val purchaseToken = purchase.purchaseToken ?: return
        Logger.d(TAG) { "launch sweep: re-sending grant for $productId bot=$botId" }
        grantChatAccessUseCase(
            GrantChatAccessParams(
                botId = botId,
                purchaseToken = purchaseToken,
                productId = productId,
            ),
        ).onSuccess {
            Logger.d(TAG) { "launch sweep: recovered ungranted $productId for bot=$botId" }
            chatTelemetry.subscriptionSuccess(botId)
        }.onFailure { error ->
            // A rejected subscription token must not be consumed here (the
            // store owns its lifecycle); transient failures retry next launch.
            Logger.e(TAG, error) { "launch sweep: grant failed for $productId" }
            chatTelemetry.subscriptionFailed(botId, "launch_sweep_${error.message}")
        }
    }

    companion object {
        private const val TAG = "SubscriptionX"
        private const val SESSION_SETTLE_DELAY_MS = 5_000L
    }
}
