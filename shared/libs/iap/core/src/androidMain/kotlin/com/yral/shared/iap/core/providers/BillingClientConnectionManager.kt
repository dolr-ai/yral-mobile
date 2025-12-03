package com.yral.shared.iap.core.providers

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.yral.shared.iap.core.IAPError
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class BillingClientConnectionManager(
    context: Context,
    purchasesUpdatedListener: PurchasesUpdatedListener,
) {
    private val billingClient: BillingClient =
        BillingClient
            .newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enableAutoServiceReconnection()
            .build()

    private val connectionMutex = Mutex()
    private val pendingContinuations = mutableListOf<Continuation<BillingClient>>()

    @Volatile
    private var isConnecting = false

    @Volatile
    private var mainContinuation: Continuation<BillingClient>? = null

    suspend fun ensureReady(): BillingClient {
        if (billingClient.isReady) {
            return billingClient
        }

        val action =
            connectionMutex.withLock {
                if (billingClient.isReady) {
                    return@withLock ConnectionAction.READY
                }
                if (isConnecting) {
                    return@withLock ConnectionAction.WAIT
                }
                isConnecting = true
                return@withLock ConnectionAction.START
            }

        return when (action) {
            ConnectionAction.READY -> billingClient
            ConnectionAction.WAIT -> waitForExistingConnection()
            ConnectionAction.START -> startNewConnection()
        }
    }

    private enum class ConnectionAction {
        READY,
        WAIT,
        START,
    }

    private suspend fun waitForExistingConnection(): BillingClient =
        suspendCancellableCoroutine { continuation ->
            synchronized(pendingContinuations) {
                pendingContinuations.add(continuation)
            }
            continuation.invokeOnCancellation {
                synchronized(pendingContinuations) {
                    pendingContinuations.remove(continuation)
                }
            }
        }

    private suspend fun startNewConnection(): BillingClient =
        suspendCancellableCoroutine { continuation ->
            synchronized(this@BillingClientConnectionManager) {
                mainContinuation = continuation
            }

            val listener =
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        handleConnectionResult(
                            success = billingResult.responseCode == BillingClient.BillingResponseCode.OK,
                            error =
                                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                    null
                                } else {
                                    IAPError.BillingUnavailable(
                                        Exception("Billing setup failed: ${billingResult.debugMessage}"),
                                    )
                                },
                        )
                    }

                    override fun onBillingServiceDisconnected() {
                        handleConnectionResult(
                            success = false,
                            error =
                                IAPError.BillingUnavailable(
                                    Exception("Billing service disconnected"),
                                ),
                        )
                    }
                }

            billingClient.startConnection(listener)

            continuation.invokeOnCancellation {
                synchronized(this@BillingClientConnectionManager) {
                    if (mainContinuation == continuation) {
                        mainContinuation = null
                        isConnecting = false
                    } else {
                        synchronized(pendingContinuations) {
                            pendingContinuations.remove(continuation)
                        }
                    }
                }
            }
        }

    private fun handleConnectionResult(
        success: Boolean,
        error: IAPError?,
    ) {
        val pending: List<Continuation<BillingClient>>
        val main: Continuation<BillingClient>?
        synchronized(this@BillingClientConnectionManager) {
            isConnecting = false
            synchronized(pendingContinuations) {
                pending = pendingContinuations.toList()
                pendingContinuations.clear()
            }
            main = mainContinuation
            mainContinuation = null
        }

        if (success && error == null) {
            main?.resume(billingClient)
            pending.forEach { it.resume(billingClient) }
        } else {
            val exception = error ?: IAPError.BillingUnavailable(Exception("Unknown connection error"))
            main?.resumeWithException(exception)
            pending.forEach { it.resumeWithException(exception) }
        }
    }
}
