package com.simmer.app

import android.os.Bundle
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import org.json.JSONArray
import org.json.JSONObject

private val Cream = Color(0xFFFFF8F0)
private val Tomato = Color(0xFFE8590C)
private val Ink = Color(0xFF2B2118)
private val Basil = Color(0xFF2F7D45)

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

data class Meal(val id: Int, val title: String, val category: String, val area: String?, val image: String, val steps: List<String>, val ingredients: List<String>, val video: String = "", val favorite: Boolean = false)
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
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = Tomato, background = Cream, surface = Color.White, onSurface = Ink), content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimmerApp() {
    val context = LocalContext.current
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
    var deletePassword by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Meal?>(null) }
    var cooking by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var searchJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var areas by remember { mutableStateOf<List<Area>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedArea by remember { mutableStateOf<String?>(null) }
    var ingredientFilter by remember { mutableStateOf("") }
    var nextRecipes by remember { mutableStateOf<String?>(null) }
    var recipesPerAd by remember { mutableStateOf(5) }
    var adsEnabled by remember { mutableStateOf(BuildConfig.DEBUG) }
    var pendingInterstitial by remember { mutableStateOf(false) }
    val meals = remember { mutableStateListOf<Meal>() }
    val savedMeals = remember { mutableStateListOf<Meal>() }
    val favorites = remember { mutableStateListOf<Int>() }
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
        meals.addAll(parseMeals(result.optJSONArray("results") ?: JSONArray()))
        nextRecipes = result.optString("next").takeIf { it.startsWith("http") }
    }
    suspend fun loadMoreRecipes() {
        val next = nextRecipes ?: return
        val result = api(next, token = token)
        meals.addAll(parseMeals(result.optJSONArray("results") ?: JSONArray()))
        nextRecipes = result.optString("next").takeIf { it.startsWith("http") }
    }
    fun openRecipe(meal: Meal) {
        selected = meal
        if (BuildConfig.ADMOB_ENABLED) AdsManager.prepareInterstitial()
        scope.launch {
            if (adsEnabled && BuildConfig.ADMOB_ENABLED && !pendingInterstitial) pendingInterstitial = RecipeAdCounter.recordOpenedRecipe(context, recipesPerAd)
        }
    }
    LaunchedEffect(token) {
        if (token != null) {
            contentLoading = true
            contentError = ""
            try {
                val appConfig = api("config/", token)
                recipesPerAd = appConfig.optInt("ad_interval", 5).coerceAtLeast(1)
                adsEnabled = BuildConfig.DEBUG || (BuildConfig.ADMOB_ENABLED && appConfig.optBoolean("ads_enabled", false))
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

    Surface(Modifier.fillMaxSize(), color = Cream) {
        AnimatedContent(targetState = if (token == null) "auth" else if (cooking) "cook" else if (selected != null) "detail" else screen,
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
                                } catch (e: Exception) { authError = e.message ?: "Google sign-in failed." }
                                busy = false
                            }
                        }
                    },
                    onCancelAuthFlow = { verificationEmail = null; passwordResetEmail = null; authError = "" })
                "cook" -> selected?.let { CookScreen(it) { cooking = false } }
                "detail" -> selected?.let { meal -> DetailScreen(meal, favorites.contains(meal.id), onBack = {
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
                }, onCook = { cooking = true }) }
                else -> Scaffold(containerColor = Cream, bottomBar = {
                    NavigationBar(containerColor = Color.White) {
                        listOf("Home" to "home", "Search" to "search", "Saved" to "favorites", "Profile" to "profile").forEach { (label, destination) ->
                            NavigationBarItem(selected = screen == destination, onClick = { screen = destination }, icon = { Text(when (destination) { "home" -> "⌂"; "search" -> "⌕"; "favorites" -> "♡"; else -> "◉" }, fontSize = 21.sp) }, label = { Text(label) })
                        }
                    }
                }) { padding ->
                    Column(Modifier.padding(padding).fillMaxSize()) {
                        TopAppBar(title = { Text(if (screen == "home") "Simmer" else screen.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold, color = Ink) },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream))
                        if (contentLoading) CircularProgressIndicator(Modifier.padding(18.dp), color = Tomato)
                        if (contentError.isNotBlank()) TextButton(onClick = { scope.launch { contentLoading = true; contentError = ""; try { loadRecipes(query) } catch (e: Exception) { contentError = e.message ?: "Could not load recipes." }; contentLoading = false } }) {
                            Text("$contentError  ·  Tap to retry", color = MaterialTheme.colorScheme.error)
                        }
                        when (screen) {
                            "home" -> HomeScreen(categories, meals, hasMore = nextRecipes != null, onLoadMore = { scope.launch { try { loadMoreRecipes() } catch (e: Exception) { contentError = e.message ?: "Could not load more recipes." } } }, onCategory = { category ->
                                selectedCategory = category.slug; selectedArea = null; scope.launch { try { loadRecipes(category = category.slug, area = null); contentError = "" } catch (e: Exception) { contentError = e.message ?: "Could not load that category." } }
                            }, onMeal = { openRecipe(it) })
                            "search" -> Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                                OutlinedTextField(query, { value -> query = value; searchJob?.cancel(); searchJob = scope.launch { kotlinx.coroutines.delay(350); try { loadRecipes(search = value); contentError = "" } catch (e: Exception) { contentError = e.message ?: "Search failed. Tap to retry." } } }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search recipes") }, shape = RoundedCornerShape(18.dp))
                                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item { androidx.compose.material3.FilterChip(selectedCategory == null, { selectedCategory = null; scope.launch { loadRecipes(query, category = null) } }, label = { Text("All categories") }) }
                                    items(categories) { category -> androidx.compose.material3.FilterChip(selectedCategory == category.slug, { selectedCategory = category.slug; scope.launch { loadRecipes(query, category = category.slug) } }, label = { Text(category.name) }) }
                                }
                                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item { androidx.compose.material3.FilterChip(selectedArea == null, { selectedArea = null; scope.launch { loadRecipes(query, area = null) } }, label = { Text("All regions") }) }
                                    items(areas) { area -> androidx.compose.material3.FilterChip(selectedArea == area.slug, { selectedArea = area.slug; scope.launch { loadRecipes(query, area = area.slug) } }, label = { Text(area.name) }) }
                                }
                                OutlinedTextField(ingredientFilter, { ingredientFilter = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Ingredient") })
                                TextButton(onClick = { scope.launch { loadRecipes(query) } }) { Text("Apply filters", color = Basil) }
                                MealList(meals, onMeal = { openRecipe(it) }, hasMore = nextRecipes != null, onLoadMore = { scope.launch { runCatching { loadMoreRecipes() }.onFailure { contentError = it.message ?: "Could not load more recipes." } } })
                            }
                            "favorites" -> MealList(savedMeals, onMeal = { openRecipe(it) })
                            else -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text("Your Simmer account", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
    Column(Modifier.fillMaxSize().padding(horizontal = 26.dp), verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(68.dp).clip(RoundedCornerShape(22.dp)).background(Tomato), contentAlignment = Alignment.Center) { Text("S", color = Color.White, fontWeight = FontWeight.Black, fontSize = 38.sp) }
        Spacer(Modifier.height(22.dp))
        val verifying = verificationEmail != null
        val resetting = passwordResetEmail != null
        Text(when { verifying -> "Check your inbox."; resetting -> "Reset your password."; register -> "Join the table."; else -> "Good food,\nmade simple." }, fontSize = 34.sp, lineHeight = 39.sp, fontWeight = FontWeight.Bold, color = Ink)
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
private fun HomeScreen(categories: List<Category>, meals: List<Meal>, hasMore: Boolean, onLoadMore: () -> Unit, onCategory: (Category) -> Unit, onMeal: (Meal) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp)) {
        item { Column(Modifier.padding(horizontal = 18.dp)) { Text("WHAT'S COOKING?", color = Basil, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text("A little inspiration\nfor your table.", fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, color = Ink) } }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(150.dp), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE0C7))) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Text("THE JOY OF COOKING", color = Tomato, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Recipes worth\ncoming home to.", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { Text("Browse by mood", Modifier.padding(start = 18.dp, top = 6.dp), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink) }
        item { androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp)) {
            items(categories) { category -> Surface(Modifier.clip(CircleShape).clickable { onCategory(category) }, color = Color.White, shape = CircleShape) { Text(category.name, Modifier.padding(horizontal = 18.dp, vertical = 12.dp), color = Ink, fontWeight = FontWeight.SemiBold) } }
        } }
        item { Text("Recipes for today", Modifier.padding(start = 18.dp, top = 4.dp), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink) }
        items(meals) { meal -> MealCard(meal, onClick = { onMeal(meal) }) }
        if (hasMore) item { TextButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) { Text("Load more recipes", color = Basil) } }
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
    Card(Modifier.fillMaxWidth().padding(horizontal = 18.dp).clickable(onClick = onClick), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(96.dp).background(Color(0xFFF0E6DA), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                AsyncImage(model = imageUrl(meal.image), contentDescription = meal.title, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Crop)
            }
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp).weight(1f)) {
                Text(meal.category.uppercase(), color = Basil, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp)); Text(meal.title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2)
                Spacer(Modifier.height(4.dp)); Text(meal.area ?: "A Simmer favorite", color = Ink.copy(alpha = .58f), fontSize = 12.sp)
            }
            Spacer(Modifier.width(12.dp))
        }
    }
}

@Composable
private fun DetailScreen(meal: Meal, isFavorite: Boolean, onBack: () -> Unit, onFavorite: () -> Unit, onCook: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("‹  Back", color = Tomato) }
            IconButton(onClick = onFavorite) { Text(if (isFavorite) "♥" else "♡", color = Tomato, fontSize = 26.sp) }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp)) {
            item { Box(Modifier.fillMaxWidth().height(210.dp).padding(horizontal = 18.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xFFFFE0C7)), contentAlignment = Alignment.Center) {
                AsyncImage(model = imageUrl(meal.image), contentDescription = meal.title, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)), contentScale = ContentScale.Crop)
            } }
            item { Column(Modifier.padding(22.dp)) { Text(meal.category.uppercase(), color = Basil, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text(meal.title, color = Ink, fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold); Text(meal.area ?: "Cook something lovely", color = Ink.copy(alpha = .6f)) } }
            item { Text("Ingredients", Modifier.padding(horizontal = 22.dp), color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
            items(meal.ingredients) { Text("•  $it", Modifier.padding(horizontal = 24.dp, vertical = 6.dp), color = Ink) }
            if (meal.video.startsWith("https://")) item { TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(meal.video))) }) { Text("Watch recipe video", color = Basil) } }
            item { Text("Method", Modifier.padding(horizontal = 22.dp, vertical = 16.dp), color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
            items(meal.steps) { step -> Text("${meal.steps.indexOf(step) + 1}.  $step", Modifier.padding(horizontal = 24.dp, vertical = 7.dp), color = Ink) }
            item { Text("Recipe data by TheMealDB", Modifier.padding(22.dp), color = Ink.copy(alpha = .55f), fontSize = 12.sp) }
        }
        Button(onClick = onCook, modifier = Modifier.fillMaxWidth().padding(18.dp).height(54.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Tomato)) { Text("Start cooking  →", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun CookScreen(meal: Meal, onClose: () -> Unit) {
    var step by remember(meal.id) { mutableStateOf(0) }
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("COOK MODE", color = Basil, fontWeight = FontWeight.Bold); TextButton(onClick = onClose) { Text("Close", color = Tomato) } }
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(meal.title, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink)
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

private suspend fun getGoogleIdToken(activity: Activity): String {
    val option = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        .setAutoSelectEnabled(false)
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
    Meal(obj.optInt("id"), obj.optString("title"), obj.optString("category"), obj.optString("area").takeIf(String::isNotBlank), obj.optString("image").ifBlank { obj.optString("thumbnail") },
        (0 until steps.length()).map { steps.optString(it) }, (0 until ingredients.length()).map { j -> ingredients.getJSONObject(j).let { listOf(it.optString("measure"), it.optString("name")).filter(String::isNotBlank).joinToString(" ") } }, obj.optString("youtube_url"))
} }
