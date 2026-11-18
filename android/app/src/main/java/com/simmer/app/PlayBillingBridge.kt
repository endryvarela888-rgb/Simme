package com.simmer.app

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

class PlayBillingBridge(
    context: Context,
    private val onProducts: (Map<String, String>) -> Unit,
    private val onPurchase: (String, String, Boolean) -> Unit,
    private val onMessage: (String) -> Unit,
) : PurchasesUpdatedListener {
    private val activity = context as? Activity
    private val details = mutableMapOf<String, ProductDetails>()
    private val productTypes = mutableMapOf<String, Boolean>()
    private val billing = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    fun connect() {
        if (billing.isReady) { loadProducts(); return }
        billing.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) loadProducts()
                else onMessage("Google Play billing is unavailable (${result.debugMessage}).")
            }
            override fun onBillingServiceDisconnected() = Unit
        })
    }

    private fun loadProducts() {
        query(COIN_PRODUCTS, BillingClient.ProductType.INAPP, false)
        query(SUBSCRIPTION_PRODUCTS, BillingClient.ProductType.SUBS, true)
        billing.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) processPurchases(purchases)
        }
        billing.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) processPurchases(purchases)
        }
    }

    private fun query(ids: List<String>, type: String, subscription: Boolean) {
        val params = QueryProductDetailsParams.newBuilder().setProductList(ids.map { id ->
            QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(type).build()
        }).build()
        billing.queryProductDetailsAsync(params) { result, response ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            response.productDetailsList.forEach { product ->
                details[product.productId] = product
                productTypes[product.productId] = subscription
            }
            val prices = details.mapNotNull { (id, product) ->
                val price = if (productTypes[id] == true) product.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
                    else product.oneTimePurchaseOfferDetailsList?.firstOrNull()?.formattedPrice
                price?.let { id to it }
            }.toMap()
            onProducts(prices)
        }
    }

    fun buy(productId: String) {
        val host = activity
        val product = details[productId]
        if (host == null || product == null) {
            onMessage("This product is not available in Google Play yet. Check that it is active in Play Console.")
            return
        }
        val subscription = productTypes[productId] == true
        val offerToken = if (subscription) product.subscriptionOfferDetails?.firstOrNull()?.offerToken
            else product.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).apply {
            offerToken?.let(::setOfferToken)
        }.build()
        val flow = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(productParams)).build()
        val result = billing.launchBillingFlow(host, flow)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) onMessage("Google Play couldn't start checkout: ${result.debugMessage}")
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> processPurchases(purchases.orEmpty())
            BillingClient.BillingResponseCode.USER_CANCELED -> onMessage("Purchase canceled.")
            else -> onMessage("Google Play purchase failed: ${result.debugMessage}")
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach { purchase ->
            purchase.products.forEach { productId ->
                val subscription = productTypes[productId] ?: productId in SUBSCRIPTION_PRODUCTS
                onPurchase(productId, purchase.purchaseToken, subscription)
            }
        }
    }

    fun close() { if (billing.isReady) billing.endConnection() }

    companion object {
        val COIN_PRODUCTS = listOf("coins_098", "coins_199", "coins_399", "coins_999", "coins_1999")
        val SUBSCRIPTION_PRODUCTS = listOf("simmer_basic_monthly", "simmer_premium_monthly", "simmer_chef_elite_monthly")
    }
}
