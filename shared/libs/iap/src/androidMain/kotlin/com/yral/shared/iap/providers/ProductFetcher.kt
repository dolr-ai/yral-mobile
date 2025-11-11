package com.yral.shared.iap.providers

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.queryProductDetails
import com.yral.shared.iap.IAPError
import com.yral.shared.iap.model.Product
import com.yral.shared.iap.model.ProductId
import com.yral.shared.iap.model.ProductType

/**
 * Handles fetching product details from Google Play Store.
 * Supports both in-app products (one-time purchases) and subscriptions.
 */
internal class ProductFetcher(
    private val connectionManager: BillingClientConnectionManager,
) {
    /**
     * Fetches product details from Google Play Store for the given product IDs.
     * Handles both in-app products (one-time purchases) and subscriptions.
     * For subscriptions, extracts recurring pricing from the last pricing phase.
     *
     * @param productIds List of product IDs to fetch
     * @return Result containing list of Product objects, or error if fetch fails
     */
    suspend fun fetchProducts(productIds: List<ProductId>): Result<List<Product>> =
        try {
            val client = connectionManager.ensureReady()
            val productList = mutableListOf<Product>()
            val productIdStrings = productIds.map { it.productId }

            // Separate in-app products and subscriptions based on naming convention
            val (inAppProductIds, subscriptionProductIds) =
                productIdStrings.partition { productId ->
                    !productId.contains("sub_") &&
                        !productId.contains("subscription") &&
                        !productId.contains("monthly") &&
                        !productId.contains("yearly")
                }

            // Fetch in-app products (continue even if this fails)
            if (inAppProductIds.isNotEmpty()) {
                val inAppProducts = fetchInAppProducts(client, inAppProductIds)
                if (inAppProducts.isSuccess) {
                    productList.addAll(inAppProducts.getOrNull() ?: emptyList())
                }
            }

            // Fetch subscription products (continue even if in-app products failed)
            if (subscriptionProductIds.isNotEmpty()) {
                val subscriptionProducts = fetchSubscriptionProducts(client, subscriptionProductIds)
                if (subscriptionProducts.isSuccess) {
                    productList.addAll(subscriptionProducts.getOrNull() ?: emptyList())
                }
            }

            // Return success if we got any products, otherwise return error
            if (productList.isNotEmpty()) {
                Result.success(productList)
            } else {
                Result.failure(
                    IAPError.UnknownError(
                        Exception("Failed to fetch any products"),
                    ),
                )
            }
        } catch (e: IAPError) {
            Result.failure(e)
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            Result.failure(IAPError.UnknownError(e))
        }

    /**
     * Queries product details for a single product, trying both INAPP and SUBS types.
     * Returns the ProductDetails if found, null otherwise.
     */
    suspend fun queryProductDetailsForPurchase(productId: String): ProductDetails? {
        val client = connectionManager.ensureReady()

        // Try INAPP first (one-time purchases)
        val inAppProduct = queryProductDetailsByType(client, productId, BillingClient.ProductType.INAPP)
        if (inAppProduct != null) {
            return inAppProduct
        }

        // Try SUBS (subscriptions)
        return queryProductDetailsByType(client, productId, BillingClient.ProductType.SUBS)
    }

    private suspend fun queryProductDetailsByType(
        client: BillingClient,
        productId: String,
        productType: String,
    ): ProductDetails? {
        val params =
            QueryProductDetailsParams
                .newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product
                            .newBuilder()
                            .setProductId(productId)
                            .setProductType(productType)
                            .build(),
                    ),
                ).build()

        val result = client.queryProductDetails(params)
        return if (
            result.billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
            result.productDetailsList?.isNotEmpty() == true
        ) {
            result.productDetailsList?.first()
        } else {
            null
        }
    }

    private suspend fun fetchInAppProducts(
        client: BillingClient,
        productIds: List<String>,
    ): Result<List<Product>> {
        val params =
            QueryProductDetailsParams
                .newBuilder()
                .setProductList(
                    productIds.map { productId ->
                        QueryProductDetailsParams.Product
                            .newBuilder()
                            .setProductId(productId)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    },
                ).build()

        val result = client.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            return Result.failure(
                IAPError.UnknownError(
                    Exception("Failed to fetch in-app products: ${result.billingResult.debugMessage}"),
                ),
            )
        }

        val products =
            result.productDetailsList?.mapNotNull { productDetails ->
                productDetails.oneTimePurchaseOfferDetails?.let { offerDetails ->
                    Product(
                        id = productDetails.productId,
                        price = offerDetails.formattedPrice,
                        priceAmountMicros = offerDetails.priceAmountMicros,
                        currencyCode = offerDetails.priceCurrencyCode,
                        title = productDetails.title,
                        description = productDetails.description,
                        type = ProductType.NON_CONSUMABLE,
                    )
                }
            } ?: emptyList()

        return Result.success(products)
    }

    private suspend fun fetchSubscriptionProducts(
        client: BillingClient,
        productIds: List<String>,
    ): Result<List<Product>> {
        val params =
            QueryProductDetailsParams
                .newBuilder()
                .setProductList(
                    productIds.map { productId ->
                        QueryProductDetailsParams.Product
                            .newBuilder()
                            .setProductId(productId)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    },
                ).build()

        val result = client.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            return Result.failure(
                IAPError.UnknownError(
                    Exception("Failed to fetch subscription products: ${result.billingResult.debugMessage}"),
                ),
            )
        }

        val products =
            result.productDetailsList?.mapNotNull { productDetails ->
                extractSubscriptionProduct(productDetails)
            } ?: emptyList()

        return Result.success(products)
    }

    @Suppress("ReturnCount")
    private fun extractSubscriptionProduct(productDetails: ProductDetails): Product? {
        val subscriptionOfferDetails = productDetails.subscriptionOfferDetails ?: return null
        if (subscriptionOfferDetails.isEmpty()) {
            return null
        }

        // Find first offer with valid pricing
        val validOffer =
            subscriptionOfferDetails.firstOrNull { offerDetails ->
                val pricingPhaseList = offerDetails.pricingPhases.pricingPhaseList
                pricingPhaseList.isNotEmpty()
            } ?: return null

        // Use last phase (recurring price, after intro/trial phases)
        val pricingPhaseList = validOffer.pricingPhases.pricingPhaseList
        val recurringPhase = pricingPhaseList.last()
        val pricingInfo = extractPricingPhaseInfo(recurringPhase) ?: return null

        return Product(
            id = productDetails.productId,
            price = pricingInfo.formattedPrice,
            priceAmountMicros = pricingInfo.priceAmountMicros,
            currencyCode = pricingInfo.currencyCode,
            title = productDetails.title,
            description = productDetails.description,
            type = ProductType.SUBSCRIPTION,
        )
    }

    private fun extractPricingPhaseInfo(pricingPhase: ProductDetails.PricingPhase): PricingInfo? {
        val formattedPrice = pricingPhase.formattedPrice
        val priceAmountMicros = pricingPhase.priceAmountMicros
        val priceCurrencyCode = pricingPhase.priceCurrencyCode

        return if (formattedPrice.isNotEmpty() && priceAmountMicros > 0 && priceCurrencyCode.isNotEmpty()) {
            PricingInfo(
                formattedPrice = formattedPrice,
                priceAmountMicros = priceAmountMicros,
                currencyCode = priceCurrencyCode,
            )
        } else {
            null
        }
    }

    private data class PricingInfo(
        val formattedPrice: String,
        val priceAmountMicros: Long,
        val currencyCode: String,
    )
}
