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
                productIds.partition { it.productType == ProductType.ONE_TIME }
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
        return if (productId.productType == ProductType.SUBS) {
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
                val (baseOffer, promoOffer) = resolveBaseAndPromo(oneTimeOffers) { it.offerId }
                val effectiveOffer = promoOffer ?: baseOffer

                effectiveOffer?.let { offerDetails ->
                    val (basePriceMicros, offerPriceMicros) =
                        resolveBaseAndOfferPrices(
                            baseOffer = baseOffer,
                            promoOffer = promoOffer,
                            priceSelector = { it.priceAmountMicros },
                            default = 0L,
                        )
                    val (basePrice, offerPrice) =
                        resolveBaseAndOfferPrices(
                            baseOffer = baseOffer,
                            promoOffer = promoOffer,
                            priceSelector = { it.formattedPrice },
                            default = "",
                        )

                    Product(
                        id = productDetails.productId,
                        price = basePrice,
                        priceAmountMicros = basePriceMicros,
                        offerPrice = offerPrice,
                        offerPriceAmountMicros = offerPriceMicros,
                        currencyCode = offerDetails.priceCurrencyCode,
                        title = productDetails.title,
                        description = productDetails.description,
                        type = ProductType.ONE_TIME,
                        billingPeriodMillis = null,
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
        val (baseOffer, promoOffer) = resolveBaseAndPromo(subscriptionOffers) { it.offerId }

        val basePhase = (baseOffer ?: promoOffer)?.pricingPhases?.pricingPhaseList?.lastOrNull() ?: return null
        val promoPhase = promoOffer?.pricingPhases?.pricingPhaseList?.firstOrNull()

        val (basePriceMicros, offerPriceMicros) =
            resolveBaseAndOfferPrices(
                baseOffer = basePhase,
                promoOffer = promoPhase,
                priceSelector = { it.priceAmountMicros },
                default = 0L,
            )
        if (basePriceMicros <= 0) return null

        val (basePrice, offerPrice) =
            resolveBaseAndOfferPrices(
                baseOffer = basePhase,
                promoOffer = promoPhase,
                priceSelector = { it.formattedPrice },
                default = "",
            )

        val billingPeriodMillis = billingPeriodToDurationMillis(basePhase.billingPeriod)

        return Product(
            id = productDetails.productId,
            price = basePrice,
            priceAmountMicros = basePriceMicros,
            offerPrice = offerPrice,
            offerPriceAmountMicros = offerPriceMicros,
            currencyCode = basePhase.priceCurrencyCode,
            title = productDetails.title,
            description = productDetails.description,
            type = ProductType.SUBS,
            billingPeriodMillis = billingPeriodMillis,
        )
    }
}
