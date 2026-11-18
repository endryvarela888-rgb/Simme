import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseApiBaseUrl = providers.gradleProperty("simmerApiBaseUrl")
    .getOrElse("https://simmer-api.duckdns.org/api/v1/")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
val admobAppId = providers.gradleProperty("simmerAdmobAppId").getOrElse(testAdmobAppId)
val admobInterstitialId = providers.gradleProperty("simmerAdmobInterstitialId").getOrElse("REPLACE_WITH_ADMOB_INTERSTITIAL_UNIT_ID")
val releaseAdsConfigured = providers.gradleProperty("simmerAdmobAppId").isPresent &&
    providers.gradleProperty("simmerAdmobInterstitialId").isPresent
val localGoogleWebClientId = Properties().let { properties ->
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.isFile) localPropertiesFile.inputStream().use { properties.load(it) }
    properties.getProperty("simmerGoogleWebClientId")
}
val googleWebClientIdInput = providers.gradleProperty("simmerGoogleWebClientId")
    .orElse(providers.environmentVariable("SIMMER_GOOGLE_WEB_CLIENT_ID"))
    .orElse(providers.provider { localGoogleWebClientId })
    .orNull
    .orEmpty()
    .trim()
val googleSignInConfigured = googleWebClientIdInput.endsWith(".apps.googleusercontent.com") &&
    !googleWebClientIdInput.startsWith("REPLACE", ignoreCase = true)
val googleWebClientId = (if (googleSignInConfigured) googleWebClientIdInput else "REPLACE_WITH_GOOGLE_WEB_CLIENT_ID")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
val uploadStoreFile = providers.environmentVariable("SIMMER_UPLOAD_KEYSTORE_PATH").orNull.orEmpty()
val uploadStorePassword = providers.environmentVariable("SIMMER_UPLOAD_STORE_PASSWORD").orNull.orEmpty()
val uploadKeyAlias = providers.environmentVariable("SIMMER_UPLOAD_KEY_ALIAS").orNull.orEmpty()
val uploadKeyPassword = providers.environmentVariable("SIMMER_UPLOAD_KEY_PASSWORD").orNull.orEmpty()
val releaseSigningConfigured = listOf(uploadStoreFile, uploadStorePassword, uploadKeyAlias, uploadKeyPassword).all(String::isNotBlank)

android {
    namespace = "com.simmer.app"
    compileSdk = 36
    signingConfigs {
        create("release") {
            if (releaseSigningConfigured) {
                storeFile = file(uploadStoreFile)
                storePassword = uploadStorePassword
                keyAlias = uploadKeyAlias
                keyPassword = uploadKeyPassword
            }
        }
    }
    defaultConfig {
        applicationId = "com.simmer.app"
        minSdk = 26
        targetSdk = 36
        versionCode = providers.gradleProperty("releaseVersionCode").orElse("1").get().toInt()
        versionName = providers.gradleProperty("releaseVersionName").orElse("0.1.0").get()
    }
    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
            buildConfigField("String", "ADMOB_APP_ID", "\"$testAdmobAppId\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("Boolean", "ADMOB_ENABLED", "true")
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
            buildConfigField("Boolean", "GOOGLE_SIGN_IN_ENABLED", googleSignInConfigured.toString())
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            manifestPlaceholders["admobAppId"] = testAdmobAppId
        }
        release {
            if (releaseSigningConfigured) signingConfig = signingConfigs.getByName("release")
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
            buildConfigField("String", "ADMOB_APP_ID", "\"$admobAppId\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$admobInterstitialId\"")
            buildConfigField("Boolean", "ADMOB_ENABLED", releaseAdsConfigured.toString())
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
            buildConfigField("Boolean", "GOOGLE_SIGN_IN_ENABLED", googleSignInConfigured.toString())
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            manifestPlaceholders["admobAppId"] = admobAppId
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.04.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.9.6")
    implementation("androidx.datastore:datastore-preferences:1.2.0")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:5.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.squareup.okhttp3:logging-interceptor:5.3.0")
    implementation("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.6.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.security:security-crypto:1.1.0")
    implementation("com.android.billingclient:billing-ktx:9.1.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

val validateReleaseSigning = tasks.register("validateReleaseSigning") {
    doLast {
        check(releaseSigningConfigured) {
            "Release signing is not configured. Set the SIMMER_UPLOAD_* environment variables before building a Play Store bundle."
        }
    }
}

tasks.configureEach {
    if (name == "bundleRelease" || name == "assembleRelease") dependsOn(validateReleaseSigning)
}
