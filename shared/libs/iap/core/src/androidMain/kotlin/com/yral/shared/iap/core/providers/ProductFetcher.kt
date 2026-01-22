package com.yral.shared.iap.core.providers

import co.touchlab.kermit.Logger
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.queryProductDetails
import com.yral.shared.iap.core.IAPError
import com.yral.shared.iap.core.model.Product
import com.yral.shared.iap.core.model.ProductId
import com.yral.shared.iap.core.model.ProductType
import com.yral.shared.iap.core.util.handleIAPResultOperation

internal class ProductFetcher(
    private val connectionManager: BillingClientConnectionManager,
) {
    @Suppress("CyclomaticComplexMethod")
    suspend fun fetchProducts(productIds: List<ProductId>): Result<List<Product>> =
        handleIAPResultOperation {
            Logger.d("SubscriptionX") { "Fetching products " }
            val client = connectionManager.ensureReady()
            val productList = mutableListOf<Product>()
            val (oneTimeProductIds, subscriptionProductIds) =
                productIds.partition { it.getProductType() == ProductType.ONE_TIME }
            val oneTimeProductIdStrings = oneTimeProductIds.map { it.productId }
            val subscriptionProductIdStrings = subscriptionProductIds.map { it.productId }
            if (oneTimeProductIdStrings.isNotEmpty()) {
                val inAppProducts = fetchInAppProducts(client, oneTimeProductIdStrings)
                inAppProducts.fold(
                    onSuccess = {
                        productList.addAll(it)
                    },
                    onFailure = { Logger.e("SubscriptionX", it) { "Failed to fetch inApp products" } },
                )
            }
            if (subscriptionProductIdStrings.isNotEmpty()) {
                val subscriptionProducts = fetchSubscriptionProducts(client, subscriptionProductIdStrings)
                subscriptionProducts.fold(
                    onSuccess = {
                        productList.addAll(it)
                    },
                    onFailure = { Logger.e("SubscriptionX", it) { "Failed to fetch subscription products" } },
                )
            }
            Logger.d("SubscriptionX") { "Fetched products $productList" }
            if (productList.isNotEmpty()) {
                Result.success(productList)
            } else {
                Result.failure(
                    IAPError.UnknownError(
                        Exception("Failed to fetch any products"),
                    ),
                )
            }
        }

    suspend fun queryProductDetailsForPurchase(productId: ProductId): ProductDetails? {
        val client = connectionManager.ensureReady()
        return if (productId.getProductType() == ProductType.SUBS) {
            queryProductDetailsByType(client, productId.productId, BillingClient.ProductType.SUBS)
        } else {
            queryProductDetailsByType(client, productId.productId, BillingClient.ProductType.INAPP)
        }
    }

    private fun getQueryProductParams(
        productIds: List<String>,
        productType: String,
    ) = QueryProductDetailsParams
        .newBuilder()
        .setProductList(
            productIds.map { productId ->
                QueryProductDetailsParams.Product
                    .newBuilder()
                    .setProductId(productId)
                    .setProductType(productType)
                    .build()
            },
        ).build()

    private suspend fun queryProductDetailsByType(
        client: BillingClient,
        productId: String,
        productType: String,
    ): ProductDetails? {
        val params = getQueryProductParams(listOf(productId), productType)
        val result = client.queryProductDetails(params)
        Logger.d("SubscriptionX") { "queryProductDetailsByType $result" }
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
        val params = getQueryProductParams(productIds, BillingClient.ProductType.INAPP)

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
                val oneTimeOffers = productDetails.oneTimePurchaseOfferDetailsList
                val offer =
                    oneTimeOffers?.firstOrNull { !it.offerId.isNullOrEmpty() } // Promotional offer
                        ?: oneTimeOffers?.firstOrNull { it.offerId.isNullOrEmpty() } // Base plan fallback
                offer?.let { offerDetails ->
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
        val params = getQueryProductParams(productIds, BillingClient.ProductType.SUBS)

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
        val subscriptionOffers = productDetails.subscriptionOfferDetails ?: return null
        val offer =
            subscriptionOffers.firstOrNull { !it.offerId.isNullOrEmpty() } // Promotional offer
                ?: subscriptionOffers.firstOrNull { it.offerId.isNullOrEmpty() } // Base plan fallback
                ?: return null
        val pricingPhaseList = offer.pricingPhases.pricingPhaseList
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
