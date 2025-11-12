package com.yral.shared.iap.core.providers

import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.Product
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.iap.core.model.ProductType
import com.yral.shared.iap.core.util.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.Foundation.NSLocaleCurrencyCode
import platform.Foundation.NSLock
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle
import platform.Foundation.NSSet
import platform.Foundation.setWithArray
import platform.StoreKit.SKProduct
import platform.StoreKit.SKProductsRequest
import platform.StoreKit.SKProductsRequestDelegateProtocol
import platform.StoreKit.SKProductsResponse
import platform.StoreKit.SKRequest
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class ProductFetcher {
    private val productCache = mutableMapOf<String, SKProduct>()
    private val productCacheLock = NSLock()

    companion object {
        private const val PRICE_TO_MICROS_FACTOR = 1_000_000L
    }

    suspend fun fetchProducts(productIds: List<ProductId>): Result<List<Product>> =
        suspendCancellableCoroutine { continuation ->
            val productIdStrings = productIds.map { it.productId }
            val productIdentifiers = NSSet.setWithArray(productIdStrings)
            val request = SKProductsRequest(productIdentifiers = productIdentifiers)

            val delegate =
                object : NSObject(), SKProductsRequestDelegateProtocol {
                    override fun productsRequest(
                        request: SKProductsRequest,
                        didReceiveResponse: SKProductsResponse,
                    ) {
                        val products = didReceiveResponse.products
                        val productList = mutableListOf<Product>()
                        for (i in 0 until products.size) {
                            val skProduct = products[i] as? SKProduct
                            if (skProduct != null) {
                                productCacheLock.withLock {
                                    productCache[skProduct.productIdentifier] = skProduct
                                }

                                val priceFormatter =
                                    NSNumberFormatter().apply {
                                        numberStyle = NSNumberFormatterCurrencyStyle
                                        locale = skProduct.priceLocale
                                    }

                                val priceString = priceFormatter.stringFromNumber(skProduct.price) ?: ""
                                val priceAmountMicros =
                                    (skProduct.price.doubleValue * PRICE_TO_MICROS_FACTOR).toLong()

                                val productType =
                                    when {
                                        skProduct.subscriptionPeriod != null -> ProductType.SUBS
                                        else -> ProductType.ONE_TIME
                                    }

                                productList.add(
                                    Product(
                                        id = skProduct.productIdentifier,
                                        price = priceString,
                                        priceAmountMicros = priceAmountMicros,
                                        currencyCode =
                                            skProduct.priceLocale
                                                .objectForKey(NSLocaleCurrencyCode)
                                                ?.toString() ?: "",
                                        title = skProduct.localizedTitle,
                                        description = skProduct.localizedDescription,
                                        type = productType,
                                    ),
                                )
                            }
                        }

                        request.cancel()
                        continuation.resume(Result.success(productList))
                    }

                    override fun request(
                        request: SKRequest,
                        didFailWithError: NSError,
                    ) {
                        request.cancel()
                        continuation.resumeWithException(
                            IAPError.NetworkError(
                                Exception("Failed to fetch products: ${didFailWithError.localizedDescription}"),
                            ),
                        )
                    }
                }

            request.delegate = delegate
            request.start()

            continuation.invokeOnCancellation {
                request.cancel()
            }
        }

    suspend fun getOrFetchSKProduct(productId: ProductId): SKProduct? {
        val productIdString = productId.productId
        val cached = productCacheLock.withLock { productCache[productIdString] }
        if (cached != null) return cached
        val fetchResult = fetchProducts(listOf(productId))
        return if (fetchResult.isFailure) {
            null
        } else {
            productCacheLock.withLock { productCache[productIdString] }
        }
    }
}
