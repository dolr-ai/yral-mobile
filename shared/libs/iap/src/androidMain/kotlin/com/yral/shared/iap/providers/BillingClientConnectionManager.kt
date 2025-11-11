package com.yral.shared.iap.providers

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.yral.shared.iap.IAPError
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Manages BillingClient connection lifecycle with connection pooling.
 * Ensures only one connection attempt is active at a time, with other callers waiting for the same connection.
 */
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

    /**
     * Ensures the BillingClient is connected and ready for use.
     * Implements connection pooling: if a connection is in progress, subsequent callers wait
     * for the same connection instead of creating new ones.
     *
     * @return Ready BillingClient instance
     * @throws IAPError.BillingUnavailable if connection fails
     */
    suspend fun ensureReady(): BillingClient {
        // Fast path: already connected
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
        kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
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
        kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
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
