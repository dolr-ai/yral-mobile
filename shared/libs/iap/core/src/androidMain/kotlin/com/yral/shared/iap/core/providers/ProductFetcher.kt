package com.yral.shared.iap.core.providers

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.queryProductDetails
import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.Product
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.iap.core.model.ProductType
import kotlinx.coroutines.CancellationException

internal class ProductFetcher(
    private val connectionManager: BillingClientConnectionManager,
) {
    suspend fun fetchProducts(productIds: List<ProductId>): Result<List<Product>> =
        try {
            val client = connectionManager.ensureReady()
            val productList = mutableListOf<Product>()
            val productIdStrings = productIds.map { it.productId }
            val (inAppProductIds, subscriptionProductIds) =
                productIdStrings.partition { productId ->
                    !productId.contains("sub_") &&
                        !productId.contains("subscription") &&
                        !productId.contains("monthly") &&
                        !productId.contains("yearly")
                }
            if (inAppProductIds.isNotEmpty()) {
                val inAppProducts = fetchInAppProducts(client, inAppProductIds)
                if (inAppProducts.isSuccess) {
                    productList.addAll(inAppProducts.getOrNull() ?: emptyList())
                }
            }
            if (subscriptionProductIds.isNotEmpty()) {
                val subscriptionProducts = fetchSubscriptionProducts(client, subscriptionProductIds)
                if (subscriptionProducts.isSuccess) {
                    productList.addAll(subscriptionProducts.getOrNull() ?: emptyList())
                }
            }
            if (productList.isNotEmpty()) {
                Result.success(productList)
            } else {
                Result.failure(
                    IAPError.UnknownError(
                        Exception("Failed to fetch any products"),
                    ),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IAPError) {
            Result.failure(e)
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            Result.failure(IAPError.UnknownError(e))
        }

    suspend fun queryProductDetailsForPurchase(productId: String): ProductDetails? {
        val client = connectionManager.ensureReady()
        val inAppProduct = queryProductDetailsByType(client, productId, BillingClient.ProductType.INAPP)
        if (inAppProduct != null) return inAppProduct
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
                        type = ProductType.ONE_TIME,
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

        val validOffer =
            subscriptionOfferDetails.firstOrNull { offerDetails ->
                offerDetails.pricingPhases.pricingPhaseList.isNotEmpty()
            } ?: return null
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
            type = ProductType.SUBS,
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
