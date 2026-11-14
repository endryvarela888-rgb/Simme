package com.simmer.app

import android.app.Activity
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.ConsentInformation
import com.google.android.ump.UserMessagingPlatform
private val Context.simmerPreferences by preferencesDataStore(name = "simmer_preferences")

object RecipeAdCounter {
    private val openedRecipeCount = intPreferencesKey("opened_recipes_since_interstitial")
    private val pendingAd = booleanPreferencesKey("interstitial_is_due")

    suspend fun recordOpenedRecipe(context: Context, interval: Int): Boolean {
        var reached = false
        context.applicationContext.simmerPreferences.edit { preferences ->
            if (preferences[pendingAd] == true) {
                reached = true
            } else {
                val next = (preferences[openedRecipeCount] ?: 0) + 1
                if (next >= interval.coerceAtLeast(1)) {
                    preferences[openedRecipeCount] = 0
                    preferences[pendingAd] = true
                    reached = true
                } else {
                    preferences[openedRecipeCount] = next
                }
            }
        }
        return reached
    }

    suspend fun clearPendingAd(context: Context) {
        context.applicationContext.simmerPreferences.edit { preferences ->
            preferences[pendingAd] = false
        }
    }
}

object AdsManager {
    @Volatile private var initialized = false
    @Volatile private var sdkReady = false
    @Volatile private var loading = false
    @Volatile private var interstitial: InterstitialAd? = null
    @Volatile private var consentInformation: ConsentInformation? = null

    fun requestConsentAndInitialize(activity: Activity) {
        if (!BuildConfig.ADMOB_ENABLED || BuildConfig.ADMOB_APP_ID.contains("REPLACE") ||
            BuildConfig.ADMOB_INTERSTITIAL_ID.contains("REPLACE") || BuildConfig.ADMOB_INTERSTITIAL_ID.isBlank()) return

        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation = consent
        val parameters = ConsentRequestParameters.Builder().build()
        consent.requestConsentInfoUpdate(activity, parameters, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                if (consent.canRequestAds()) initialize(activity.applicationContext)
            }
        }, {
            if (consent.canRequestAds()) initialize(activity.applicationContext)
        })
    }

    @Synchronized
    private fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        MobileAds.initialize(context, InitializationConfig.Builder(BuildConfig.ADMOB_APP_ID).build()) {
            sdkReady = true
            loadInterstitial()
        }
    }

    fun prepareInterstitial() {
        if (sdkReady) loadInterstitial()
    }

    fun privacyOptionsRequired(): Boolean = consentInformation?.privacyOptionsRequirementStatus ==
        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { }
    }

    @Synchronized
    private fun loadInterstitial() {
        if (!sdkReady || loading || interstitial != null) return
        loading = true
        InterstitialAd.load(AdRequest.Builder(BuildConfig.ADMOB_INTERSTITIAL_ID).build(),
            object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad; loading = false }
                override fun onAdFailedToLoad(adError: LoadAdError) { interstitial = null; loading = false }
            })
    }

    fun showIfReady(activity: Activity): Boolean {
        val ad = interstitial ?: return false
        interstitial = null
        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdDismissedFullScreenContent() { loadInterstitial() }
            override fun onAdFailedToShowFullScreenContent(error: com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError) {
                loadInterstitial()
            }
        }
        ad.show(activity)
        return true
    }
}
