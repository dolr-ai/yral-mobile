package com.yral.shared.iap.providers

import com.yral.shared.iap.IAPError
import com.yral.shared.iap.model.Product
import com.yral.shared.iap.model.ProductId
import com.yral.shared.iap.model.ProductType
import com.yral.shared.iap.util.withLock
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

/**
 * Handles product fetching and caching for iOS StoreKit.
 * Manages SKProduct cache to avoid redundant network requests.
 */
internal class ProductFetcher {
    // Cache SKProducts by product ID to avoid refetching
    private val productCache = mutableMapOf<String, SKProduct>()
    private val productCacheLock = NSLock()

    companion object {
        // Conversion factor: price to micros (1 dollar = 1,000,000 micros)
        private const val PRICE_TO_MICROS_FACTOR = 1_000_000L
    }

    /**
     * Fetches products from the App Store and caches SKProducts.
     *
     * @param productIds List of product IDs to fetch
     * @return Result containing list of Product models or IAPError
     */
    suspend fun fetchProducts(productIds: List<ProductId>): Result<List<Product>> =
        suspendCancellableCoroutine { continuation ->
            // Convert ProductId enum to strings for the StoreKit API
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

                        // Cache SKProducts and build Product list
                        for (i in 0 until products.size) {
                            val skProduct = products[i] as? SKProduct
                            if (skProduct != null) {
                                // Cache the SKProduct
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
                                        skProduct.subscriptionPeriod != null -> ProductType.SUBSCRIPTION
                                        else -> ProductType.NON_CONSUMABLE
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

    /**
     * Gets SKProduct from cache or fetches it if not cached.
     *
     * @param productId Product ID to get
     * @return SKProduct if found, null otherwise
     */
    suspend fun getOrFetchSKProduct(productId: ProductId): SKProduct? {
        val productIdString = productId.productId

        // Check cache first
        val cached =
            productCacheLock.withLock {
                productCache[productIdString]
            }
        if (cached != null) {
            return cached
        }

        // Not in cache, fetch it
        val fetchResult = fetchProducts(listOf(productId))
        return if (fetchResult.isFailure) {
            null
        } else {
            // Should be in cache now
            productCacheLock.withLock {
                productCache[productIdString]
            }
        }
    }
}
