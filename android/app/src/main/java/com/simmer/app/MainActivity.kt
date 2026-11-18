package com.simmer.app

import android.os.Bundle
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import org.json.JSONArray
import org.json.JSONObject

private val Cream = Color(0xFFFFF8F3)
private val Tomato = Color(0xFF9C1BB1)
private val Ink = Color(0xFF302339)
private val Basil = Color(0xFF397B52)
private val Petal = Color(0xFFF8D9F4)
private val Champagne = Color(0xFFFFE7A3)
private val Lilac = Color(0xFFE2D6FF)
private val NeonMint = Color(0xFFD5FF00)
private val NeonPink = Color(0xFFFF38C7)
private val NeonBlue = Color(0xFF00DFFF)
private val NeonPeach = Color(0xFFFF8A00)
private val NeonViolet = Color(0xFFAA5CFF)
private val BackgroundBlush = Color(0xFFFFB8E8)
private val BackgroundSky = Color(0xFF9EECFF)
private val BackgroundApricot = Color(0xFFFFD09B)

private object AppSession {
    var context: android.content.Context? = null
    var access: String? = null
    var refresh: String? = null
    fun initialize(context: android.content.Context) {
        this.context = context.applicationContext
        access = SessionStore.loadAccess(context)
        refresh = SessionStore.loadRefresh(context)
    }
    fun save(access: String, refresh: String?) {
        this.access = access
        this.refresh = refresh
        AppSessionRefresh.value = refresh
        context?.let { SessionStore.save(it, access, refresh) }
    }
    fun clear() {
        access = null
        refresh = null
        AppSessionRefresh.value = null
        context?.let { SessionStore.clear(it) }
    }
}

data class Meal(val id: Int, val title: String, val category: String, val area: String?, val image: String, val steps: List<String>, val ingredients: List<String>, val video: String = "", val favorite: Boolean = false, val author: String = "TheMealDB", val authorId: Int = 0, val likes: Int = 0, val dislikes: Int = 0, val myVote: Int = 0)
data class RecipeComment(val id: Int, val author: String, val text: String, val avatar: String = "", val authorId: Int = 0, val replies: List<RecipeComment> = emptyList())
data class Category(val name: String, val slug: String, val image: String)
data class Area(val name: String, val slug: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdsManager.requestConsentAndInitialize(this)
        setContent { SimmerTheme { SimmerApp() } }
    }
}

@Composable
private fun SimmerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Tomato, onPrimary = Color.White, secondary = Basil, onSecondary = Color.White,
            tertiary = Color(0xFFB78542), background = Cream, onBackground = Ink,
            surface = Color(0xFFFFFCFA), onSurface = Ink, surfaceVariant = Petal,
            onSurfaceVariant = Color(0xFF68596B), outline = Color(0xFFD7C8D4)
        ),
        typography = MaterialTheme.typography.copy(
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Serif),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif)
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimmerApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    remember { AppSession.initialize(context) }
    val scope = rememberCoroutineScope()
    var token by remember { mutableStateOf(AppSession.access) }
    var screen by remember { mutableStateOf("auth") }
    var authError by remember { mutableStateOf("") }
    var contentError by remember { mutableStateOf("") }
    var contentLoading by remember { mutableStateOf(false) }
    var verificationEmail by remember { mutableStateOf<String?>(null) }
    var passwordResetEmail by remember { mutableStateOf<String?>(null) }
    var showDeleteAccount by remember { mutableStateOf(false) }
    var showBlockedUsers by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Meal?>(null) }
    var cooking by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var categorySearch by remember { mutableStateOf("") }
    var areaSearch by remember { mutableStateOf("") }
    var searchJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var areas by remember { mutableStateOf<List<Area>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedArea by remember { mutableStateOf<String?>(null) }
    var ingredientFilter by remember { mutableStateOf("") }
    var selectedRecipeImage by remember { mutableStateOf<Uri?>(null) }
    val recipeImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedRecipeImage = it }
    var selectedAvatarImage by remember { mutableStateOf<Uri?>(null) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedAvatarImage = it }
    var profileName by remember { mutableStateOf("") }
    var profileEmail by remember { mutableStateOf("") }
    var profileAvatar by remember { mutableStateOf("") }
    var ugcTermsAccepted by remember { mutableStateOf(false) }
    val blockedUsers = remember { mutableStateListOf<Pair<Int, String>>() }
    var fullScreenAvatar by remember { mutableStateOf<String?>(null) }
    var nextRecipes by remember { mutableStateOf<String?>(null) }
    var loadingMore by remember { mutableStateOf(false) }
    var recipesPerAd by remember { mutableStateOf(5) }
    var adsEnabled by remember { mutableStateOf(BuildConfig.DEBUG) }
    var coinBalance by remember { mutableStateOf(0) }
    var purchasesEnabled by remember { mutableStateOf(false) }
    var storePrices by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var storeMessage by remember { mutableStateOf("") }
    var subscriptionTier by remember { mutableStateOf("") }
    var chefInitialRecipe by remember { mutableStateOf<Meal?>(null) }
    var billingBridge by remember { mutableStateOf<PlayBillingBridge?>(null) }
    var pendingInterstitial by remember { mutableStateOf(false) }
    val meals = remember { mutableStateListOf<Meal>() }
    val savedMeals = remember { mutableStateListOf<Meal>() }
    val favorites = remember { mutableStateListOf<Int>() }
    DisposableEffect(token) {
        if (token == null) {
            billingBridge = null
            onDispose { }
        } else {
            val bridge = PlayBillingBridge(context,
                onProducts = { prices -> storePrices = prices },
                onPurchase = { productId, purchaseToken, isSubscription ->
                    scope.launch {
                        storeMessage = "Verifying purchase securely…"
                        try {
                            val path = if (isSubscription) "coins/subscriptions/verify/" else "coins/purchase/verify/"
                            val result = api(path, token, JSONObject().put("product_id", productId).put("purchase_token", purchaseToken))
                            coinBalance = result.optInt("balance", coinBalance)
                            if (isSubscription) subscriptionTier = result.optString("tier", subscriptionTier)
                            adsEnabled = !isSubscription && adsEnabled
                            storeMessage = if (isSubscription) "Subscription active. Enjoy Simmer!" else "Coins added to your wallet!"
                        } catch (e: Exception) { storeMessage = e.message ?: "Purchase verification is pending. Reopen the app to retry." }
                    }
                },
                onMessage = { storeMessage = it })
            billingBridge = bridge
            bridge.connect()
            onDispose { bridge.close(); if (billingBridge === bridge) billingBridge = null }
        }
    }
    DisposableEffect(token, lifecycleOwner) {
        if (token == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    scope.launch {
                        runCatching {
                            val wallet = api("coins/balance/", token)
                            coinBalance = wallet.optInt("balance", coinBalance)
                            val subscription = wallet.optJSONObject("subscription")
                            subscriptionTier = subscription?.optString("tier").orEmpty()
                            if (subscription?.optBoolean("active", false) == true && subscriptionTier in listOf("premium", "chef_elite")) {
                                val claim = runCatching { api("coins/claim-daily/", token, JSONObject()) }.getOrNull()
                                if (claim != null) coinBalance = claim.optInt("balance", coinBalance)
                            }
                        }
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }
    DisposableEffect(cooking) {
        val window = (context as? Activity)?.window
        if (cooking) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { if (cooking) window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    suspend fun loadRecipes(search: String = "", category: String? = selectedCategory, area: String? = selectedArea, ingredient: String = ingredientFilter) {
        val params = buildList {
            if (search.isNotBlank()) add("search=${java.net.URLEncoder.encode(search, "UTF-8")}")
            if (category != null) add("category=${java.net.URLEncoder.encode(category, "UTF-8")}")
            if (area != null) add("area=${java.net.URLEncoder.encode(area, "UTF-8")}")
            if (ingredient.isNotBlank()) add("ingredient=${java.net.URLEncoder.encode(ingredient.trim(), "UTF-8")}")
        }.joinToString("&")
        val result = api("recipes/${if (params.isNotEmpty()) "?$params" else ""}", token = token)
        meals.clear()
        meals.addAll(parseMeals(result.optJSONArray("results") ?: JSONArray()).shuffled())
        nextRecipes = result.optString("next").takeIf { it.startsWith("http") }
    }
    suspend fun loadMoreRecipes() {
        if (loadingMore) return
        val next = nextRecipes ?: return
        loadingMore = true
        try {
            val result = api(next, token = token)
            val knownIds = meals.mapTo(mutableSetOf()) { it.id }
            meals.addAll(parseMeals(result.optJSONArray("results") ?: JSONArray()).filterNot { it.id in knownIds }.shuffled())
            nextRecipes = result.optString("next").takeIf { it.startsWith("http") }
        } finally { loadingMore = false }
    }
    fun openRecipe(meal: Meal) {
        selected = meal
        if (BuildConfig.ADMOB_ENABLED) AdsManager.prepareInterstitial()
        scope.launch {
            if (adsEnabled && BuildConfig.ADMOB_ENABLED && !pendingInterstitial) pendingInterstitial = RecipeAdCounter.recordOpenedRecipe(context, recipesPerAd)
        }
    }
    fun updateRecipeRating(recipeId: Int, likes: Int, dislikes: Int, myVote: Int) {
        selected = selected?.takeIf { it.id == recipeId }?.copy(likes = likes, dislikes = dislikes, myVote = myVote)
        meals.indices.firstOrNull { meals[it].id == recipeId }?.let { index -> meals[index] = meals[index].copy(likes = likes, dislikes = dislikes, myVote = myVote) }
        savedMeals.indices.firstOrNull { savedMeals[it].id == recipeId }?.let { index -> savedMeals[index] = savedMeals[index].copy(likes = likes, dislikes = dislikes, myVote = myVote) }
    }
    LaunchedEffect(token) {
        if (token != null) {
            contentLoading = true
            contentError = ""
            try {
                val me = api("auth/me/", token)
                profileName = me.optString("display_name")
                profileEmail = me.optString("email")
                profileAvatar = me.optString("avatar")
                ugcTermsAccepted = me.optBoolean("ugc_terms_accepted", false)
                val appConfig = api("config/", token)
                recipesPerAd = appConfig.optInt("ad_interval", 5).coerceAtLeast(1)
                val wallet = api("coins/balance/", token)
                coinBalance = wallet.optInt("balance", 0)
                val subscription = wallet.optJSONObject("subscription")
                subscriptionTier = subscription?.optString("tier").orEmpty()
                val activeSubscription = subscription?.optBoolean("active", false) == true
                adsEnabled = !activeSubscription && (BuildConfig.DEBUG || (BuildConfig.ADMOB_ENABLED && appConfig.optBoolean("ads_enabled", false)))
                val products = api("coins/products/", token)
                purchasesEnabled = products.optBoolean("enabled", false)
                val data = api("categories/", token)
                categories = parseCategories(data.optJSONArray("results") ?: JSONArray())
                val areaData = api("areas/", token)
                areas = parseAreas(areaData.optJSONArray("results") ?: JSONArray())
                loadRecipes()
                val saved = api("favorites/", token)
                savedMeals.clear()
                savedMeals.addAll(parseMeals(saved.optJSONArray("results") ?: JSONArray()))
                favorites.clear()
                favorites.addAll(savedMeals.map { it.id })
            } catch (error: Exception) { contentError = error.message ?: "Couldn't load Simmer. Check your connection and retry." }
            finally { contentLoading = false }
        }
    }

    Surface(Modifier.fillMaxSize(), color = Color.Transparent) {
        AnimatedContent(targetState = if (token == null) "auth" else if (fullScreenAvatar != null) "avatar" else if (cooking) "cook" else if (selected != null) "detail" else screen,
            transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "simmer-screen") { page ->
            when (page) {
                "auth" -> AuthScreen(busy, authError, verificationEmail, passwordResetEmail,
                    onSubmit = { email, password, name, create ->
                    busy = true; authError = ""
                    scope.launch {
                        try {
                            if (create) {
                                val result = api("auth/register/", body = JSONObject().put("email", email).put("password", password).put("display_name", name))
                                verificationEmail = result.optString("email", email)
                                authError = result.optString("detail", "Check your inbox for a verification code.")
                            } else {
                                val session = api("auth/login/", body = JSONObject().put("email", email).put("password", password))
                                token = session.getString("access")
                                AppSession.save(token!!, session.optString("refresh"))
                                screen = "home"
                            }
                        } catch (e: Exception) { authError = e.message ?: "Sign in failed. Check your connection and details." }
                        busy = false
                    }
                },
                    onVerifyEmail = { email, code ->
                        busy = true; authError = ""
                        scope.launch {
                            try {
                                val session = api("auth/verify-email/", body = JSONObject().put("email", email).put("code", code))
                                token = session.getString("access"); AppSession.save(token!!, session.optString("refresh"))
                                verificationEmail = null; screen = "home"
                            } catch (e: Exception) { authError = e.message ?: "That code could not be verified." }
                            busy = false
                        }
                    },
                    onResendVerification = { email ->
                        busy = true; authError = ""
                        scope.launch { try { authError = api("auth/resend-verification/", body = JSONObject().put("email", email)).optString("detail", "Check your inbox."); verificationEmail = email }
                            catch (e: Exception) { authError = e.message ?: "Could not resend the code." }; busy = false }
                    },
                    onForgotPassword = { email ->
                        busy = true; authError = ""
                        scope.launch { try { api("auth/password/forgot/", body = JSONObject().put("email", email)); passwordResetEmail = email; authError = "If the account exists, a reset code will be emailed." }
                            catch (e: Exception) { authError = e.message ?: "Could not request a reset code." }; busy = false }
                    },
                    onResetPassword = { email, code, newPassword ->
                        busy = true; authError = ""
                        scope.launch { try { authError = api("auth/password/reset/", body = JSONObject().put("email", email).put("code", code).put("new_password", newPassword)).optString("detail", "Password changed."); passwordResetEmail = null }
                            catch (e: Exception) { authError = e.message ?: "Could not reset the password." }; busy = false }
                    },
                    onGoogleSignIn = {
                        if (!BuildConfig.GOOGLE_SIGN_IN_ENABLED) {
                            authError = "Google sign-in setup is pending. Add simmerGoogleWebClientId to Gradle properties."
                        } else {
                            busy = true; authError = ""
                            scope.launch {
                                try {
                                    val activity = context as Activity
                                    val googleToken = getGoogleIdToken(activity)
                                    val session = api("auth/google/", body = JSONObject().put("id_token", googleToken))
                                    token = session.getString("access"); AppSession.save(token!!, session.optString("refresh")); screen = "home"
                                } catch (e: NoCredentialException) {
                                    authError = "Google could not find an account. Add or reauthenticate a Google account on this device, then try again."
                                } catch (e: Exception) { authError = e.message ?: "Google sign-in failed." }
                                busy = false
                            }
                        }
                    },
                    onCancelAuthFlow = { verificationEmail = null; passwordResetEmail = null; authError = "" })
                "cook" -> selected?.let { CookScreen(it) { cooking = false } }
                "avatar" -> AvatarViewer(fullScreenAvatar.orEmpty()) { fullScreenAvatar = null }
                "detail" -> selected?.let { meal -> DetailScreen(meal, favorites.contains(meal.id), token.orEmpty(), ugcTermsAccepted, onTermsAccepted = { ugcTermsAccepted = true }, onAvatarClick = { fullScreenAvatar = it }, onRatingUpdated = { likes, dislikes, vote -> updateRecipeRating(meal.id, likes, dislikes, vote) }, onBack = {
                    val activity = context as? Activity
                    if (!pendingInterstitial || activity == null || !AdsManager.showIfReady(activity)) selected = null
                    else { pendingInterstitial = false; selected = null; scope.launch { RecipeAdCounter.clearPendingAd(context) } }
                }, onFavorite = {
                    if (token == null) return@DetailScreen
                    scope.launch {
                        try {
                            if (favorites.contains(meal.id)) { api("favorites/${meal.id}/", token, method = "DELETE"); favorites.remove(meal.id); savedMeals.removeAll { it.id == meal.id } }
                            else { api("favorites/", token, JSONObject().put("recipe_id", meal.id)); favorites.add(meal.id); savedMeals.add(meal) }
                        } catch (e: Exception) { contentError = e.message ?: "Couldn't update favorites. Try again." }
                    }
                }, onCook = { cooking = true }, onAskChef = { chefInitialRecipe = meal; selected = null; cooking = false; screen = "chef" }) }
                else -> Scaffold(containerColor = Color.Transparent, bottomBar = {
                    NavigationBar(containerColor = Color(0xFFF1D7FF), contentColor = Tomato) {
                        listOf("Home" to "home", "Search" to "search", "Saved" to "favorites", "Chef" to "chef", "Shop" to "shop", "Profile" to "profile").forEach { (label, destination) ->
                            NavigationBarItem(selected = screen == destination, onClick = { screen = destination; if (destination != "chef") chefInitialRecipe = null }, icon = { Text(when (destination) { "home" -> "⌂"; "search" -> "⌕"; "favorites" -> "♡"; "chef" -> "✦"; "shop" -> "◉"; else -> "☺" }, fontSize = 19.sp) }, label = { Text(label, fontSize = 10.sp, fontWeight = if (screen == destination) FontWeight.Bold else FontWeight.Medium, maxLines = 1) }, colors = androidx.compose.material3.NavigationBarItemDefaults.colors(selectedIconColor = Tomato, selectedTextColor = Tomato, indicatorColor = Petal, unselectedIconColor = Ink.copy(alpha = .55f), unselectedTextColor = Ink.copy(alpha = .55f)))
                        }
                    }
                }) { padding ->
                    Column(Modifier.padding(padding).fillMaxSize().background(Brush.linearGradient(listOf(BackgroundBlush, Lilac, BackgroundSky, BackgroundApricot)))) {
                        TopAppBar(title = {
            if (screen == "home") Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Simmer", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = Ink, fontSize = 25.sp)
                Text("✦", color = NeonPink, fontSize = 25.sp)
            } else Text(if (screen == "create_recipe") "New recipe" else screen.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = Ink)
        }, actions = {
            if (token != null && screen !in listOf("create_recipe", "chef", "shop")) CoinBalancePill(coinBalance) { screen = "shop"; chefInitialRecipe = null }
            if (screen == "home") TextButton(onClick = { selectedRecipeImage = null; screen = "create_recipe" }) { Text("＋ Recipe", color = Tomato, fontWeight = FontWeight.Bold) }
        },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
                        if (contentLoading) CircularProgressIndicator(Modifier.padding(18.dp), color = Tomato)
                        if (contentError.isNotBlank()) TextButton(onClick = { scope.launch { contentLoading = true; contentError = ""; try { loadRecipes(query) } catch (e: Exception) { contentError = e.message ?: "Could not load recipes." }; contentLoading = false } }) {
                            Text("$contentError  ·  Tap to retry", color = MaterialTheme.colorScheme.error)
                        }
                        when (screen) {
                            "home" -> HomeScreen(meals, hasMore = nextRecipes != null, isLoadingMore = loadingMore, onLoadMore = { scope.launch { try { loadMoreRecipes(); contentError = "" } catch (e: Exception) { contentError = e.message ?: "Could not load more recipes." } } }, onMeal = { openRecipe(it) })
                            "search" -> Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                                OutlinedTextField(query, { value -> query = value; searchJob?.cancel(); searchJob = scope.launch { kotlinx.coroutines.delay(350); try { loadRecipes(search = value); contentError = "" } catch (e: Exception) { contentError = e.message ?: "Search failed. Tap to retry." } } }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search recipes") }, shape = RoundedCornerShape(18.dp))
                                OutlinedTextField(categorySearch, { categorySearch = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Type a food category") }, shape = RoundedCornerShape(16.dp))
                                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item { androidx.compose.material3.FilterChip(selectedCategory == null, { selectedCategory = null; scope.launch { loadRecipes(query, category = null) } }, label = { Text("All categories") }) }
                                    items(categories.filter { it.name.contains(categorySearch.trim(), ignoreCase = true) }) { category -> androidx.compose.material3.FilterChip(selectedCategory == category.slug, { selectedCategory = category.slug; scope.launch { loadRecipes(query, category = category.slug) } }, label = { Text(category.name) }) }
                                }
                                OutlinedTextField(areaSearch, { areaSearch = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Type a region or cuisine") }, shape = RoundedCornerShape(16.dp))
                                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item { androidx.compose.material3.FilterChip(selectedArea == null, { selectedArea = null; scope.launch { loadRecipes(query, area = null) } }, label = { Text("All regions") }) }
                                    items(areas.filter { it.name.contains(areaSearch.trim(), ignoreCase = true) }) { area -> androidx.compose.material3.FilterChip(selectedArea == area.slug, { selectedArea = area.slug; scope.launch { loadRecipes(query, area = area.slug) } }, label = { Text(area.name) }) }
                                }
                                OutlinedTextField(ingredientFilter, { ingredientFilter = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Ingredient") })
                                TextButton(onClick = { scope.launch { loadRecipes(query) } }) { Text("Apply filters", color = Basil) }
                                MealList(meals, onMeal = { openRecipe(it) }, hasMore = nextRecipes != null, onLoadMore = { scope.launch { runCatching { loadMoreRecipes() }.onFailure { contentError = it.message ?: "Could not load more recipes." } } })
                            }
                            "create_recipe" -> RecipeCreateScreen(categories, areas, selectedRecipeImage, ugcTermsAccepted, onPickImage = { recipeImagePicker.launch("image/*") }, onCancel = { screen = "home" }, onSubmit = { title, ingredients, steps, category, area, image, accepted ->
                                busy = true; contentError = ""
                                scope.launch {
                                    try {
                                        val created = uploadRecipe(context, token.orEmpty(), title, ingredients, steps, category, area, image, accepted)
                                        ugcTermsAccepted = ugcTermsAccepted || accepted
                                        val uploaded = parseMeals(JSONArray().put(created)).first()
                                        meals.add(0, uploaded)
                                        screen = "home"
                                        selectedRecipeImage = null
                                    } catch (e: Exception) { contentError = e.message ?: "Could not publish your recipe." }
                                    finally { busy = false }
                                }
                            }, busy = busy, error = contentError)
                            "favorites" -> MealList(savedMeals, onMeal = { openRecipe(it) })
                            "shop" -> Column(Modifier.fillMaxSize()) {
                                if (storeMessage.isNotBlank()) TextButton(onClick = { storeMessage = "" }) { Text(storeMessage, color = if (storeMessage.contains("fail", true) || storeMessage.contains("not", true)) MaterialTheme.colorScheme.error else Basil, fontSize = 12.sp) }
                                ShopScreen(coinBalance, purchasesEnabled, storePrices,
                                    onBuy = { productId, _ -> billingBridge?.buy(productId) ?: run { storeMessage = "Google Play billing is starting. Please try again in a moment." } },
                                    onRefreshBalance = { scope.launch { runCatching { coinBalance = api("coins/balance/", token).optInt("balance", coinBalance) } } })
                            }
                            "chef" -> ChefScreen(token.orEmpty(), meals, chefInitialRecipe, coinBalance, onBalanceChanged = { coinBalance = it }, onShop = { screen = "shop"; chefInitialRecipe = null })
                            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Your Simmer account", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
                                if (selectedAvatarImage != null) AsyncImage(model = selectedAvatarImage, contentDescription = "Selected avatar", modifier = Modifier.size(112.dp).clip(CircleShape).clickable { fullScreenAvatar = selectedAvatarImage.toString() }, contentScale = ContentScale.Crop)
                                else ProfileAvatar(profileAvatar, profileName.ifBlank { profileEmail }, Modifier.size(112.dp).clickable { if (profileAvatar.isNotBlank()) fullScreenAvatar = imageUrl(profileAvatar) })
                                TextButton(onClick = { avatarPicker.launch("image/*") }) { Text(if (profileAvatar.isBlank() && selectedAvatarImage == null) "＋ Add profile photo" else "Change profile photo", color = Tomato) }
                                OutlinedTextField(profileName, { profileName = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("Display name") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                                Text(profileEmail, color = Ink.copy(alpha = .65f))
                                Button(onClick = {
                                    if (!busy) {
                                        busy = true
                                        scope.launch {
                                            try {
                                                val updated = updateProfile(context, token.orEmpty(), profileName, selectedAvatarImage)
                                                profileName = updated.optString("display_name")
                                                profileAvatar = updated.optString("avatar")
                                                selectedAvatarImage = null
                                                contentError = "Profile updated."
                                            } catch (e: Exception) { contentError = e.message ?: "Could not update your profile." }
                                            finally { busy = false }
                                        }
                                    }
                                }, enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = Tomato), modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Saving…" else "Save profile") }
                                if (profileAvatar.isNotBlank() || selectedAvatarImage != null) TextButton(onClick = {
                                    scope.launch {
                                        try {
                                            val updated = api("auth/me/", token, JSONObject().put("display_name", profileName).put("remove_avatar", true), method = "PATCH")
                                            profileAvatar = updated.optString("avatar"); selectedAvatarImage = null
                                        } catch (e: Exception) { contentError = e.message ?: "Could not remove your photo." }
                                    }
                                }) { Text("Remove photo", color = Ink.copy(alpha = .65f)) }
                                if (contentError.isNotBlank()) Text(contentError, color = Basil, fontSize = 13.sp)
                                OutlinedButton(onClick = { selectedRecipeImage = null; screen = "create_recipe" }) { Text("＋  Share a recipe", color = Tomato) }
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        try {
                                            val page = api("auth/blocks/", token)
                                            val rows = page.optJSONArray("results") ?: JSONArray()
                                            blockedUsers.clear()
                                            for (i in 0 until rows.length()) rows.optJSONObject(i)?.let { blockedUsers.add(it.optInt("id") to it.optString("display_name")) }
                                            showBlockedUsers = true
                                        } catch (e: Exception) { contentError = e.message ?: "Could not load blocked cooks." }
                                    }
                                }) { Text("Blocked cooks", color = Tomato) }
                                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://simmer-api.duckdns.org/privacy/"))) }) { Text("Privacy policy", color = Basil) }
                                TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://simmer-api.duckdns.org/terms/"))) }) { Text("Terms of use", color = Basil) }
                                if (AdsManager.privacyOptionsRequired()) TextButton(onClick = { AdsManager.showPrivacyOptions(context as Activity) }) { Text("Privacy choices", color = Basil) }
                                TextButton(onClick = {
                                    scope.launch {
                                        runCatching { api("auth/logout/", token, JSONObject().put("refresh", AppSession.refresh)) }
                                        AppSession.clear(); token = null; selected = null; screen = "auth"
                                    }
                                }) { Text("Sign out", color = Tomato) }
                                TextButton(onClick = { deletePassword = ""; showDeleteAccount = true }) { Text("Delete account", color = MaterialTheme.colorScheme.error) }
                                Text("Recipe data by TheMealDB", color = Ink.copy(alpha = .65f))
                            }
                        }
                    }
                }
            }
        }
    }
    if (showDeleteAccount) AlertDialog(
        onDismissRequest = { showDeleteAccount = false },
        title = { Text("Delete your account?") },
        text = { Column { Text("This permanently removes your account and saved recipes."); Spacer(Modifier.height(10.dp)); OutlinedTextField(deletePassword, { deletePassword = it }, label = { Text("Confirm with password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true) } },
        confirmButton = { TextButton(enabled = deletePassword.isNotBlank() && !busy, onClick = {
            busy = true
            scope.launch {
                try { api("auth/me/", token, JSONObject().put("password", deletePassword), method = "DELETE"); AppSession.clear(); token = null; selected = null; screen = "auth"; showDeleteAccount = false }
                catch (e: Exception) { authError = e.message ?: "Account could not be deleted." }
                busy = false
            }
        }) { Text("Delete permanently", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { showDeleteAccount = false }) { Text("Cancel") } }
    )
    if (showBlockedUsers) AlertDialog(
        onDismissRequest = { showBlockedUsers = false },
        title = { Text("Blocked cooks") },
        text = { if (blockedUsers.isEmpty()) Text("You haven't blocked any cooks.") else Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            blockedUsers.toList().forEach { (id, name) -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(name, Modifier.weight(1f), color = Ink)
                TextButton(onClick = { scope.launch {
                    try { api("auth/blocks/$id/", token, method = "DELETE"); blockedUsers.removeAll { it.first == id } }
                    catch (e: Exception) { contentError = e.message ?: "Could not unblock this cook." }
                } }) { Text("Unblock", color = Tomato) }
            } }
        } },
        confirmButton = { TextButton(onClick = { showBlockedUsers = false }) { Text("Done") } }
    )
}

@Composable
private fun RecipeCreateScreen(
    categories: List<Category>, areas: List<Area>, image: Uri?, termsAlreadyAccepted: Boolean, onPickImage: () -> Unit,
    onCancel: () -> Unit,
    onSubmit: (String, List<String>, List<String>, String, String, Uri, Boolean) -> Unit,
    busy: Boolean, error: String,
) {
    var title by remember { mutableStateOf("") }
    var ingredientsText by remember { mutableStateOf("") }
    val steps = remember { mutableStateListOf("") }
    var category by remember { mutableStateOf<Category?>(null) }
    var area by remember { mutableStateOf<Area?>(null) }
    var categoryMenu by remember { mutableStateOf(false) }
    var areaMenu by remember { mutableStateOf(false) }
    var acceptsTerms by remember(termsAlreadyAccepted) { mutableStateOf(termsAlreadyAccepted) }
    val uriHandler = LocalUriHandler.current
    val ingredients = ingredientsText.lineSequence().map(String::trim).filter(String::isNotBlank).toList()
    val ready = title.isNotBlank() && ingredients.isNotEmpty() && steps.all(String::isNotBlank) && image != null && (termsAlreadyAccepted || acceptsTerms)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Share something delicious", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 27.sp, color = Ink)
        Text("Add a photo, ingredients and numbered cooking steps. Your recipe will be credited to you.", color = Ink.copy(alpha = .72f))
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Recipe name *") }, singleLine = true, shape = RoundedCornerShape(16.dp))
        OutlinedButton(onClick = onPickImage, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(if (image == null) "Choose recipe photo *" else "Change recipe photo", color = Tomato) }
        if (image != null) AsyncImage(model = image, contentDescription = "Selected recipe photo", modifier = Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(22.dp)), contentScale = ContentScale.Crop)
        Text("Category (optional)", color = Ink, fontWeight = FontWeight.SemiBold)
        Box {
            OutlinedButton(onClick = { categoryMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(category?.name ?: "Unspecified", color = Ink) }
            DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }, modifier = Modifier.heightIn(max = 280.dp)) {
                DropdownMenuItem(text = { Text("Unspecified") }, onClick = { category = null; categoryMenu = false })
                categories.forEach { item -> DropdownMenuItem(text = { Text(item.name) }, onClick = { category = item; categoryMenu = false }) }
            }
        }
        Text("Region or cuisine (optional)", color = Ink, fontWeight = FontWeight.SemiBold)
        Box {
            OutlinedButton(onClick = { areaMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(area?.name ?: "Not specified", color = Ink) }
            DropdownMenu(expanded = areaMenu, onDismissRequest = { areaMenu = false }, modifier = Modifier.heightIn(max = 280.dp)) {
                DropdownMenuItem(text = { Text("Not specified") }, onClick = { area = null; areaMenu = false })
                areas.forEach { item -> DropdownMenuItem(text = { Text(item.name) }, onClick = { area = item; areaMenu = false }) }
            }
        }
        OutlinedTextField(ingredientsText, { ingredientsText = it }, Modifier.fillMaxWidth().height(150.dp), label = { Text("Ingredients * (one per line)") }, shape = RoundedCornerShape(16.dp))
        Text("Preparation steps *", color = Ink, fontWeight = FontWeight.SemiBold)
        steps.forEachIndexed { index, step ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(step, { steps[index] = it }, Modifier.weight(1f), label = { Text("Step ${index + 1}") }, shape = RoundedCornerShape(16.dp), minLines = 2)
                if (steps.size > 1) TextButton(onClick = { steps.removeAt(index) }) { Text("×", color = Tomato, fontSize = 22.sp) }
            }
        }
        TextButton(onClick = { if (steps.size < 30) steps.add("") }, enabled = steps.size < 30) { Text("＋ Add another step", color = Tomato) }
        if (!termsAlreadyAccepted) Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = acceptsTerms, onCheckedChange = { acceptsTerms = it })
            Text("I agree to Simmer's ", color = Ink, fontSize = 13.sp)
            TextButton(onClick = { uriHandler.openUri("https://simmer-api.duckdns.org/terms/") }) { Text("Terms of Use", color = Tomato) }
        }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        Button(onClick = { image?.let { onSubmit(title.trim(), ingredients, steps.map(String::trim), category?.slug.orEmpty(), area?.slug.orEmpty(), it, acceptsTerms) } }, enabled = ready && !busy, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Tomato)) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text("Publish recipe", fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel", color = Ink.copy(alpha = .7f)) }
    }
}

@Composable
private fun AuthScreen(
    busy: Boolean,
    error: String,
    verificationEmail: String?,
    passwordResetEmail: String?,
    onSubmit: (String, String, String, Boolean) -> Unit,
    onVerifyEmail: (String, String) -> Unit,
    onResendVerification: (String) -> Unit,
    onForgotPassword: (String) -> Unit,
    onResetPassword: (String, String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onCancelAuthFlow: () -> Unit,
) {
    var register by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(BackgroundBlush, Lilac, BackgroundSky, BackgroundApricot))).padding(horizontal = 26.dp), verticalArrangement = Arrangement.Center) {
        AsyncImage(model = R.drawable.simmer_mascot, contentDescription = "Simmer chef mascot", modifier = Modifier.size(104.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.height(22.dp))
        val verifying = verificationEmail != null
        val resetting = passwordResetEmail != null
        Text(when { verifying -> "Check your inbox."; resetting -> "Reset your password."; register -> "Join the table."; else -> "Good food,\nmade simple." }, fontSize = 36.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = Ink)
        Spacer(Modifier.height(8.dp)); Text(when { verifying -> "Enter the 6-digit code sent to $verificationEmail"; resetting -> "Enter the reset code sent to $passwordResetEmail"; else -> "Find your next favorite recipe." }, color = Ink.copy(alpha = .62f))
        Spacer(Modifier.height(28.dp))
        if (verifying) {
            OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), label = { Text("Verification code") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        } else if (resetting) {
            OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), label = { Text("Reset code") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("New password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
        } else {
            if (register) OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Your name") }, singleLine = true)
            OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
        }
        if (error.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        Spacer(Modifier.height(18.dp))
        Button(onClick = { when { verifying -> onVerifyEmail(verificationEmail!!, code); resetting -> onResetPassword(passwordResetEmail!!, code, password); else -> onSubmit(email.trim(), password, name.trim(), register) } }, modifier = Modifier.fillMaxWidth().height(54.dp), enabled = !busy && when { verifying -> code.length == 6; resetting -> code.length == 6 && password.length >= 8; else -> email.contains("@") && password.length >= 8 },
            shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Tomato)) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text(when { verifying -> "Verify email"; resetting -> "Change password"; register -> "Create account"; else -> "Sign in" }, fontWeight = FontWeight.Bold)
        }
        if (!verifying && !resetting) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onGoogleSignIn, modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !busy) { Text("Continue with Google", color = Ink) }
        }
        when {
            verifying -> { TextButton(onClick = { onResendVerification(verificationEmail!!) }, enabled = !busy) { Text("Resend code", color = Basil) }; TextButton(onClick = { register = false; onCancelAuthFlow() }) { Text("Back to sign in", color = Basil) } }
            resetting -> TextButton(onClick = { register = false; onCancelAuthFlow() }) { Text("Back to sign in", color = Basil) }
            else -> {
                if (!register) {
                    TextButton(onClick = { onForgotPassword(email.trim()) }, enabled = !busy && email.contains("@")) { Text("Forgot password?", color = Basil) }
                    TextButton(onClick = { if (email.contains("@")) onResendVerification(email.trim()) }, enabled = !busy && email.contains("@")) { Text("Resend verification code", color = Basil) }
                }
                TextButton(onClick = { register = !register }) { Text(if (register) "Already have an account? Sign in" else "New to Simmer? Create account", color = Basil) }
            }
        }
    }
}

@Composable
private fun HomeScreen(meals: List<Meal>, hasMore: Boolean, isLoadingMore: Boolean, onLoadMore: () -> Unit, onMeal: (Meal) -> Unit) {
    val gridState = rememberLazyGridState()
    val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    LaunchedEffect(lastVisibleIndex, meals.size, hasMore, isLoadingMore) {
        if (hasMore && !isLoadingMore && meals.isNotEmpty() && lastVisibleIndex >= meals.size - 4) onLoadMore()
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), state = gridState,
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp)
    ) {
        gridItems(meals, key = { it.id }) { meal -> RecipeGridCard(meal, onClick = { onMeal(meal) }) }
        if (isLoadingMore) item(span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Tomato) }
        }
        if (meals.isEmpty() && !hasMore) item(span = { GridItemSpan(maxLineSpan) }) {
            Text("No recipes found yet.", Modifier.padding(24.dp), color = Ink.copy(alpha = .7f))
        }
    }
}

@Composable
private fun RecipeGridCard(meal: Meal, onClick: () -> Unit) {
    val labelPalette = listOf(NeonMint, NeonPink, NeonBlue, NeonPeach, NeonViolet)
    val labelColor = labelPalette[(meal.category.hashCode() % labelPalette.size + labelPalette.size) % labelPalette.size]
    val cardPalette = listOf(Color(0xFFFFF0D6), Color(0xFFFCE4F6), Color(0xFFE0FBFF), Color(0xFFEFE6FF), Color(0xFFEDFFD9))
    val cardColor = cardPalette[(meal.id % cardPalette.size + cardPalette.size) % cardPalette.size]
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(25.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(148.dp).background(Lilac)) {
                AsyncImage(model = imageUrl(meal.image), contentDescription = meal.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Surface(
                    Modifier.align(Alignment.BottomStart).padding(9.dp),
                    shape = RoundedCornerShape(50), color = labelColor, shadowElevation = 3.dp
                ) {
                    Text(meal.category.ifBlank { "Recipe" }.uppercase(), Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Ink, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp, maxLines = 1)
                }
                Text("✦", Modifier.align(Alignment.TopEnd).padding(9.dp), color = Color.White, fontSize = 20.sp)
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(meal.title, color = Ink, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, fontSize = 16.sp, lineHeight = 19.sp, maxLines = 2, minLines = 2)
                Text("✧  ${meal.author}", color = Tomato, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("♥ ${meal.likes}     ♡ ${meal.dislikes}", color = Ink.copy(alpha = .68f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MealList(meals: List<Meal>, onMeal: (Meal) -> Unit, hasMore: Boolean = false, onLoadMore: () -> Unit = {}) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp)) {
        items(meals) { meal -> MealCard(meal, onClick = { onMeal(meal) }) }
        if (hasMore) item { TextButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) { Text("Load more recipes", color = Basil) } }
        if (meals.isEmpty()) item { Text("No recipes to show yet. The API needs imported recipes.", Modifier.padding(24.dp), color = Ink.copy(alpha = .65f)) }
    }
}

@Composable
private fun MealCard(meal: Meal, onClick: () -> Unit) {
    val labelPalette = listOf(NeonMint, NeonPink, NeonBlue, NeonPeach, NeonViolet)
    val labelColor = labelPalette[(meal.category.hashCode() % labelPalette.size + labelPalette.size) % labelPalette.size]
    val cardPalette = listOf(Color(0xFFFFF0D6), Color(0xFFFCE4F6), Color(0xFFE0FBFF), Color(0xFFEFE6FF), Color(0xFFEDFFD9))
    val cardColor = cardPalette[(meal.id % cardPalette.size + cardPalette.size) % cardPalette.size]
    Card(Modifier.fillMaxWidth().padding(horizontal = 18.dp).clickable(onClick = onClick), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = cardColor), elevation = CardDefaults.cardElevation(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(96.dp).background(Color(0xFFF0E6DA), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                AsyncImage(model = imageUrl(meal.image), contentDescription = meal.title, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Crop)
            }
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp).weight(1f)) {
                Surface(shape = RoundedCornerShape(50), color = labelColor) {
                    Text(meal.category.ifBlank { "Recipe" }.uppercase(), Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = Ink, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = .6.sp, maxLines = 1)
                }
                Spacer(Modifier.height(4.dp)); Text(meal.title, color = Ink, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, fontSize = 17.sp, maxLines = 2)
                Spacer(Modifier.height(4.dp)); Text("Recipe by ${meal.author}", color = Tomato.copy(alpha = .82f), fontSize = 11.sp, maxLines = 1)
                Text("♥ ${meal.likes}     ♡ ${meal.dislikes}", color = Ink.copy(alpha = .66f), fontSize = 10.sp)
            }
            Spacer(Modifier.width(12.dp))
        }
    }
}

@Composable
private fun DetailScreen(meal: Meal, isFavorite: Boolean, token: String, ugcTermsAccepted: Boolean, onTermsAccepted: () -> Unit, onAvatarClick: (String) -> Unit, onRatingUpdated: (Int, Int, Int) -> Unit, onBack: () -> Unit, onFavorite: () -> Unit, onCook: () -> Unit, onAskChef: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var likes by remember(meal.id) { mutableStateOf(meal.likes) }
    var dislikes by remember(meal.id) { mutableStateOf(meal.dislikes) }
    var myVote by remember(meal.id) { mutableStateOf(meal.myVote) }
    var comments by remember(meal.id) { mutableStateOf<List<RecipeComment>>(emptyList()) }
    var nextCommentsPage by remember(meal.id) { mutableStateOf<String?>(null) }
    var commentText by remember(meal.id) { mutableStateOf("") }
    var replyTarget by remember(meal.id) { mutableStateOf<RecipeComment?>(null) }
    var interactionMessage by remember(meal.id) { mutableStateOf("") }
    var reportTarget by remember(meal.id) { mutableStateOf<Pair<String, Int>?>(null) }
    var reportReason by remember(meal.id) { mutableStateOf("inappropriate") }
    var reportDetails by remember(meal.id) { mutableStateOf("") }
    var interactionBusy by remember(meal.id) { mutableStateOf(false) }
    var blockTarget by remember(meal.id) { mutableStateOf<Int?>(null) }
    var locallyBlocked by remember(meal.id) { mutableStateOf<Set<Int>>(emptySet()) }
    var acceptsTerms by remember(meal.id, ugcTermsAccepted) { mutableStateOf(ugcTermsAccepted) }
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(meal.id) {
        runCatching {
            val detail = api("recipes/${meal.id}/", token)
            likes = detail.optInt("likes_count", likes)
            dislikes = detail.optInt("dislikes_count", dislikes)
            myVote = detail.optInt("my_vote", myVote)
            val page = api("recipes/${meal.id}/comments/", token)
            comments = parseComments(page.optJSONArray("results") ?: JSONArray())
            nextCommentsPage = page.optString("next").takeIf { it.startsWith("http") }
        }.onFailure { interactionMessage = it.message ?: "Could not load comments." }
    }
    fun vote(value: Int) {
        if (interactionBusy) return
        scope.launch {
            interactionBusy = true
            try {
                val result = api("recipes/${meal.id}/vote/", token, JSONObject().put("value", value))
                likes = result.optInt("likes_count", likes); dislikes = result.optInt("dislikes_count", dislikes); myVote = result.optInt("my_vote", value)
                onRatingUpdated(likes, dislikes, myVote)
            } catch (error: Exception) { interactionMessage = error.message ?: "Could not save your vote." }
            finally { interactionBusy = false }
        }
    }
    fun report(kind: String, id: Int, reason: String, details: String) {
        scope.launch {
            try {
                val path = when (kind) { "recipe" -> "recipes/$id/report/"; "user" -> "auth/users/$id/report/"; else -> "comments/$id/report/" }
                val result = api(path, token, JSONObject().put("reason", reason).put("details", details))
                interactionMessage = result.optString("detail", "Thanks for your report.")
                reportTarget = null
            } catch (error: Exception) { interactionMessage = error.message ?: "Could not send the report." }
        }
    }
    Column(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(BackgroundBlush, Lilac, BackgroundSky, BackgroundApricot)))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹  Back", color = Tomato) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { reportTarget = "recipe" to meal.id }) { Text("Report", color = Ink.copy(alpha = .75f)) }
                IconButton(onClick = onFavorite) { Text(if (isFavorite) "♥" else "♡", color = Tomato, fontSize = 26.sp) }
            }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp)) {
            item { Box(Modifier.fillMaxWidth().height(210.dp).padding(horizontal = 18.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xFFFFE0C7)), contentAlignment = Alignment.Center) {
                AsyncImage(model = imageUrl(meal.image), contentDescription = meal.title, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)), contentScale = ContentScale.Crop)
            } }
            item { Column(Modifier.padding(22.dp)) { Text("✦  ${meal.category.uppercase()}", color = Basil, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.2.sp); Text(meal.title, color = Ink, fontSize = 32.sp, lineHeight = 37.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold); Text(meal.area ?: "Cook something lovely", color = Tomato.copy(alpha = .85f)); Text("Recipe by ${meal.author}", Modifier.padding(top = 5.dp), color = Ink.copy(alpha = .7f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (meal.authorId > 0) Row {
                    TextButton(onClick = { reportTarget = "user" to meal.authorId }) { Text("Report cook", color = Tomato, fontSize = 12.sp) }
                    TextButton(onClick = { blockTarget = meal.authorId }) { Text("Block cook", color = Ink.copy(alpha = .7f), fontSize = 12.sp) }
                }
            } }
            item { Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.FilterChip(selected = myVote == 1, onClick = { vote(1) }, label = { Text("♡  Like  $likes") })
                androidx.compose.material3.FilterChip(selected = myVote == -1, onClick = { vote(-1) }, label = { Text("Dislike  $dislikes") })
            } }
            item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp).clickable(onClick = onAskChef), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE5FA)), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(3.dp)) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), horizontalAlignment = Alignment.End) {
                        Text("Questions or trouble with this recipe?", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                        Text("Ask the Master Chef  ✦", color = Tomato, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                }
            }
            item { Text("Ingredients", Modifier.padding(horizontal = 22.dp), color = Ink, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, fontSize = 22.sp) }
            items(meal.ingredients) { Text("•  $it", Modifier.padding(horizontal = 24.dp, vertical = 6.dp), color = Ink) }
            if (meal.video.startsWith("https://")) item { TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(meal.video))) }) { Text("Watch recipe video", color = Basil) } }
            item { Text("Method", Modifier.padding(horizontal = 22.dp, vertical = 16.dp), color = Ink, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, fontSize = 22.sp) }
            items(meal.steps) { step -> Text("${meal.steps.indexOf(step) + 1}.  $step", Modifier.padding(horizontal = 24.dp, vertical = 7.dp), color = Ink) }
            item { Text("Comments  ·  ${comments.size}", Modifier.padding(start = 22.dp, top = 22.dp, bottom = 8.dp), color = Ink, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, fontSize = 22.sp) }
            if (comments.isEmpty()) item { Text("No comments yet. Start the conversation!", Modifier.padding(horizontal = 22.dp, vertical = 8.dp), color = Ink.copy(alpha = .7f)) }
            items(comments, key = { it.id }) { comment ->
                if (comment.authorId !in locallyBlocked) CommentThread(comment, onReply = { replyTarget = comment }, onReport = { reportTarget = "comment" to it }, onReportUser = { reportTarget = "user" to it }, onBlockUser = { blockTarget = it }, onAvatarClick = onAvatarClick)
            }
            if (nextCommentsPage != null) item {
                TextButton(onClick = {
                    val nextPage = nextCommentsPage ?: return@TextButton
                    if (!interactionBusy) scope.launch {
                        interactionBusy = true
                        try {
                            val page = api(nextPage, token)
                            comments = comments + parseComments(page.optJSONArray("results") ?: JSONArray())
                            nextCommentsPage = page.optString("next").takeIf { it.startsWith("http") }
                        } catch (error: Exception) { interactionMessage = error.message ?: "Could not load more comments." }
                        finally { interactionBusy = false }
                    }
                }, enabled = !interactionBusy, modifier = Modifier.fillMaxWidth()) { Text(if (interactionBusy) "Loading comments…" else "Load more comments", color = Tomato) }
            }
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (replyTarget != null) Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Replying to ${replyTarget?.author}", Modifier.weight(1f), color = Tomato, fontSize = 13.sp)
                        TextButton(onClick = { replyTarget = null }) { Text("Cancel", color = Ink) }
                    }
                    OutlinedTextField(commentText, { commentText = it }, Modifier.fillMaxWidth(), label = { Text(if (replyTarget == null) "Add a comment" else "Write a reply") }, minLines = 2, maxLines = 5, shape = RoundedCornerShape(16.dp))
                    if (!ugcTermsAccepted) Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = acceptsTerms, onCheckedChange = { acceptsTerms = it })
                        Text("I agree to Simmer's ", color = Ink, fontSize = 12.sp)
                        TextButton(onClick = { uriHandler.openUri("https://simmer-api.duckdns.org/terms/") }) { Text("Terms of Use", color = Tomato, fontSize = 12.sp) }
                    }
                    Button(onClick = {
                        val message = commentText.trim()
                        if (message.isNotEmpty() && !interactionBusy) scope.launch {
                            interactionBusy = true
                            try {
                                val body = JSONObject().put("text", message).put("accept_terms", acceptsTerms)
                                replyTarget?.let { body.put("parent", it.id) }
                                val created = api("recipes/${meal.id}/comments/", token, body)
                                if (acceptsTerms) onTermsAccepted()
                                val parsed = parseComment(created)
                                val parent = replyTarget
                                comments = if (parent == null) comments + parsed else comments.map { root -> if (root.id == parent.id) root.copy(replies = root.replies + parsed) else root }
                                commentText = ""; replyTarget = null; interactionMessage = "Comment posted."
                            } catch (error: Exception) { interactionMessage = error.message ?: "Could not post your comment." }
                            finally { interactionBusy = false }
                        }
                    }, enabled = commentText.isNotBlank() && !interactionBusy && (ugcTermsAccepted || acceptsTerms), modifier = Modifier.align(Alignment.End), colors = ButtonDefaults.buttonColors(containerColor = Tomato)) { Text("Post") }
                    if (interactionMessage.isNotBlank()) Text(interactionMessage, color = if (interactionMessage.contains("could not", true)) MaterialTheme.colorScheme.error else Basil, fontSize = 13.sp)
                }
            }
        }
        Button(onClick = onCook, modifier = Modifier.fillMaxWidth().padding(18.dp).height(54.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Tomato)) { Text("Start cooking  →", fontWeight = FontWeight.Bold) }
    }
    if (reportTarget != null) AlertDialog(
        onDismissRequest = { reportTarget = null },
        title = { Text("Report content") },
        text = { Column {
            (if (reportTarget?.first == "user") listOf("inappropriate" to "Inappropriate", "spam" to "Spam", "harassment" to "Harassment", "other" to "Other") else listOf("inappropriate" to "Inappropriate", "spam" to "Spam", "misinformation" to "Incorrect or unsafe", "copyright" to "Copyright concern", "other" to "Other")).forEach { (key, label) ->
                Row(Modifier.fillMaxWidth().clickable { reportReason = key }, verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = reportReason == key, onClick = { reportReason = key }); Text(label) }
            }
            OutlinedTextField(reportDetails, { reportDetails = it.take(500) }, label = { Text("Details (optional)") }, maxLines = 3)
        } },
        confirmButton = { TextButton(onClick = { reportTarget?.let { report(it.first, it.second, reportReason, reportDetails) } }) { Text("Send report", color = Tomato) } },
        dismissButton = { TextButton(onClick = { reportTarget = null }) { Text("Cancel") } },
    )
    if (blockTarget != null) AlertDialog(
        onDismissRequest = { blockTarget = null },
        title = { Text("Block this cook?") },
        text = { Text("Their recipes and comments will be hidden from you. You can unblock them from your profile.") },
        confirmButton = { TextButton(onClick = {
            val id = blockTarget ?: return@TextButton
            scope.launch {
                try {
                    api("auth/blocks/$id/", token, JSONObject(), method = "POST")
                    locallyBlocked = locallyBlocked + id
                    comments = comments.filterNot { it.authorId == id }.map { it.copy(replies = it.replies.filterNot { reply -> reply.authorId == id }) }
                    if (id == meal.authorId) onBack()
                    interactionMessage = "Cook blocked."
                } catch (error: Exception) { interactionMessage = error.message ?: "Could not block this cook." }
                blockTarget = null
            }
        }) { Text("Block", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { blockTarget = null }) { Text("Cancel") } }
    )
}

@Composable
private fun CommentThread(comment: RecipeComment, onReply: () -> Unit, onReport: (Int) -> Unit, onReportUser: (Int) -> Unit, onBlockUser: (Int) -> Unit, onAvatarClick: (String) -> Unit, nested: Boolean = false) {
    var expanded by remember(comment.id) { mutableStateOf(false) }
    val cardColors = listOf(Color(0xFFEFE6FF), Color(0xFFFFE3F5), Color(0xFFDDF8FF), Color(0xFFFFEACD))
    val cardColor = cardColors[(comment.id % cardColors.size + cardColors.size) % cardColors.size]
    Column(Modifier.fillMaxWidth().padding(start = if (nested) 34.dp else 18.dp, end = 18.dp, top = 3.dp, bottom = 3.dp)) {
        Surface(shape = RoundedCornerShape(20.dp), color = if (nested) cardColor.copy(alpha = .78f) else cardColor, shadowElevation = 2.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    ProfileAvatar(comment.avatar, comment.author, Modifier.size(34.dp).then(if (comment.avatar.isNotBlank()) Modifier.clickable { onAvatarClick(imageUrl(comment.avatar)) } else Modifier))
                    Text(comment.author, color = Tomato, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1)
                }
                Spacer(Modifier.height(6.dp))
                Text(comment.text, color = Ink, fontSize = 14.sp, lineHeight = 19.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onReply, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text("Reply", color = Basil, fontSize = 12.sp) }
                    TextButton(onClick = { onReport(comment.id) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text("Report", color = Ink.copy(alpha = .65f), fontSize = 12.sp) }
                    if (comment.authorId > 0) {
                        TextButton(onClick = { onReportUser(comment.authorId) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text("Report cook", color = Ink.copy(alpha = .65f), fontSize = 12.sp) }
                        TextButton(onClick = { onBlockUser(comment.authorId) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text("Block", color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    }
                    if (!nested && comment.replies.isNotEmpty()) {
                        TextButton(onClick = { expanded = !expanded }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                            Text(if (expanded) "Hide replies" else "${comment.replies.size} ${if (comment.replies.size == 1) "reply" else "replies"}", color = Tomato, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        if (!nested && expanded) comment.replies.forEach { reply -> CommentThread(reply, onReply = onReply, onReport = onReport, onReportUser = onReportUser, onBlockUser = onBlockUser, onAvatarClick = onAvatarClick, nested = true) }
    }
}

@Composable
private fun ProfileAvatar(url: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = CircleShape, color = Lilac, shadowElevation = 2.dp) {
        Box(contentAlignment = Alignment.Center) {
            if (url.isNotBlank()) AsyncImage(model = imageUrl(url), contentDescription = "Profile photo of $label", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
            else Text(label.trim().firstOrNull()?.uppercase() ?: "S", color = Tomato, fontWeight = FontWeight.Bold, fontSize = 19.sp)
        }
    }
}

@Composable
private fun AvatarViewer(url: String, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(Color(0xEE17111D)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
        AsyncImage(model = url, contentDescription = "Profile photo", modifier = Modifier.fillMaxWidth().padding(20.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Fit)
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) { Text("‹  Back", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun CookScreen(meal: Meal, onClose: () -> Unit) {
    var step by remember(meal.id) { mutableStateOf(0) }
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(BackgroundBlush, Lilac, BackgroundSky, BackgroundApricot))).padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("COOK MODE", color = Basil, fontWeight = FontWeight.Bold); TextButton(onClick = onClose) { Text("Close", color = Tomato) } }
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(meal.title, fontSize = 29.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = Ink)
            Text("STEP ${step + 1} OF ${meal.steps.size}", color = Tomato, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(meal.steps.getOrElse(step) { "You're all done. Enjoy your meal!" }, fontSize = 24.sp, lineHeight = 32.sp, color = Ink)
            Text("Ingredients are in the recipe details.", color = Ink.copy(alpha = .55f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { if (step > 0) step-- }, modifier = Modifier.weight(1f), enabled = step > 0, shape = RoundedCornerShape(16.dp)) { Text("Previous") }
            Button(onClick = { if (step < meal.steps.lastIndex) step++ else onClose() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Tomato)) { Text(if (step < meal.steps.lastIndex) "Next step" else "Finish") }
        }
    }
}

private suspend fun api(path: String, token: String? = null, body: JSONObject? = null, method: String = if (body == null) "GET" else "POST"): JSONObject = withContext(Dispatchers.IO) {
    if (AppSessionRefresh.value != AppSession.refresh) AppSessionRefresh.value = AppSession.refresh
    AppSessionRefresh.onRefreshed = { access, refresh -> AppSession.save(access, refresh ?: AppSession.refresh) }
    val text = simmerRequest(path, token, body, method)
    if (text.isBlank()) JSONObject() else JSONObject(text)
}

private suspend fun uploadRecipe(
    context: android.content.Context, token: String, title: String, ingredients: List<String>, steps: List<String>,
    category: String, area: String, image: Uri, acceptTerms: Boolean,
): JSONObject = withContext(Dispatchers.IO) {
    val mime = context.contentResolver.getType(image) ?: "image/jpeg"
    require(mime.startsWith("image/")) { "Choose an image file." }
    val bytes = ByteArrayOutputStream().use { output ->
        val buffer = ByteArray(16 * 1024)
        var total = 0
        context.contentResolver.openInputStream(image)?.use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                require(total <= 8 * 1024 * 1024) { "The recipe photo must be 8 MB or smaller." }
                output.write(buffer, 0, read)
            }
        } ?: error("Could not open the selected photo.")
        output.toByteArray()
    }
    val body = MultipartBody.Builder().setType(MultipartBody.FORM)
        .addFormDataPart("title", title)
        .addFormDataPart("ingredients", JSONArray().apply { ingredients.forEach { put(JSONObject().put("name", it).put("measure", "")) } }.toString())
        .addFormDataPart("steps", JSONArray().apply { steps.forEach { put(it) } }.toString())
        .addFormDataPart("category", category)
        .addFormDataPart("area", area)
        .addFormDataPart("accept_terms", acceptTerms.toString())
        .addFormDataPart("image", "recipe-upload", bytes.toRequestBody(mime.toMediaType()))
        .build()
    JSONObject(simmerMultipartRequest("recipes/create/", token, body))
}

private suspend fun updateProfile(context: android.content.Context, token: String, displayName: String, avatar: Uri?): JSONObject = withContext(Dispatchers.IO) {
    if (avatar == null) return@withContext api("auth/me/", token, JSONObject().put("display_name", displayName), method = "PATCH")
    val mime = context.contentResolver.getType(avatar) ?: "image/jpeg"
    require(mime.startsWith("image/")) { "Choose an image file." }
    val bytes = ByteArrayOutputStream().use { output ->
        val buffer = ByteArray(16 * 1024)
        var total = 0
        context.contentResolver.openInputStream(avatar)?.use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                require(total <= 8 * 1024 * 1024) { "The profile photo must be 8 MB or smaller." }
                output.write(buffer, 0, read)
            }
        } ?: error("Could not open the selected photo.")
        output.toByteArray()
    }
    val body = MultipartBody.Builder().setType(MultipartBody.FORM)
        .addFormDataPart("display_name", displayName)
        .addFormDataPart("avatar", "profile-photo", bytes.toRequestBody(mime.toMediaType()))
        .build()
    JSONObject(simmerMultipartRequest("auth/me/", token, body, "PATCH"))
}

private suspend fun getGoogleIdToken(activity: Activity): String {
    val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        .build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    val credential = CredentialManager.create(activity).getCredential(activity, request).credential
    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
    error("Google did not return a valid sign-in token.")
}

private fun imageUrl(path: String): String = if (path.startsWith("http")) path else BuildConfig.API_BASE_URL.substringBefore("/api/v1/") + path

private fun parseCategories(array: JSONArray) = (0 until array.length()).map { i -> array.getJSONObject(i).let { Category(it.optString("name"), it.optString("slug"), it.optString("thumbnail")) } }
private fun parseAreas(array: JSONArray) = (0 until array.length()).map { i -> array.getJSONObject(i).let { Area(it.optString("name"), it.optString("slug")) } }
private fun parseMeals(array: JSONArray) = (0 until array.length()).map { i -> array.getJSONObject(i).let { obj ->
    val ingredients = obj.optJSONArray("ingredients") ?: JSONArray()
    val steps = obj.optJSONArray("steps") ?: JSONArray()
    Meal(obj.optInt("id"), obj.optString("title"), obj.optString("category"), obj.optString("area").takeIf { it.isNotBlank() && it != "null" }, obj.optString("image").ifBlank { obj.optString("thumbnail") },
        (0 until steps.length()).map { steps.optString(it) }, (0 until ingredients.length()).map { j -> ingredients.getJSONObject(j).let { listOf(it.optString("measure"), it.optString("name")).filter(String::isNotBlank).joinToString(" ") } },
        obj.optString("youtube_url"), author = obj.optString("author_name").ifBlank { "TheMealDB" }, authorId = obj.optInt("author_id"), likes = obj.optInt("likes_count"), dislikes = obj.optInt("dislikes_count"), myVote = obj.optInt("my_vote"))
} }
private fun parseComments(array: JSONArray) = (0 until array.length()).map { parseComment(array.getJSONObject(it)) }
private fun parseComment(obj: JSONObject): RecipeComment {
    val replies = obj.optJSONArray("replies") ?: JSONArray()
    return RecipeComment(obj.optInt("id"), obj.optString("author_name").ifBlank { "Simmer cook" }, obj.optString("text"), obj.optString("author_avatar"), obj.optInt("author_id"), parseComments(replies))
}
