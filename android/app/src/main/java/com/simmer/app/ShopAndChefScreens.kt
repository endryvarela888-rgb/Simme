package com.simmer.app

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val ShopInk = Color(0xFF302339)
private val ShopPink = Color(0xFF9C1BB1)
private val ShopGreen = Color(0xFF397B52)
private val ShopCream = Color(0xFFFFFCFA)
private val ShopCards = listOf(Color(0xFFFFE0F4), Color(0xFFE8DDFF), Color(0xFFDDF8FF), Color(0xFFFFEACD))

data class ChefChatMessage(val role: String, val text: String, val id: Int = 0)
data class ChefChatHistory(val id: Int, val title: String, val persona: String, val recipeId: Int?, val recipeTitle: String, val expiresAt: String)

@Composable
fun CoinBalancePill(balance: Int, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color(0xFFFFE69A), shape = RoundedCornerShape(30.dp), shadowElevation = 3.dp) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Image(painterResource(R.drawable.simmer_coin), contentDescription = "Simmer coins", modifier = Modifier.size(23.dp))
            Text(balance.toString(), color = ShopInk, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
    }
}

@Composable
fun ShopScreen(
    balance: Int,
    purchasesEnabled: Boolean,
    storePrices: Map<String, String>,
    onBuy: (String, Boolean) -> Unit,
    onRefreshBalance: () -> Unit,
) {
    var tab by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Your kitchen wallet", fontWeight = FontWeight.Bold, color = ShopInk, fontSize = 22.sp)
                Text("Coins are used for one Chef AI reply each.", color = ShopInk.copy(alpha = .72f), fontSize = 13.sp)
            }
            CoinBalancePill(balance, onRefreshBalance)
        }
        Spacer(Modifier.height(12.dp))
        TabRow(selectedTabIndex = tab, containerColor = Color.Transparent, contentColor = ShopPink) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Subscriptions", fontWeight = FontWeight.Bold) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Coin packs", fontWeight = FontWeight.Bold) })
        }
        if (!purchasesEnabled || storePrices.isEmpty()) {
            Text("Purchases will be available when the products are active in Google Play.", Modifier.padding(top = 9.dp), color = ShopInk.copy(alpha = .7f), fontSize = 12.sp)
        }
        if (tab == 0) {
            LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    SubscriptionCard(R.drawable.simmer_tier_basic, "BASIC", "Ad-free cooking", "No ads anywhere in Simmer.", "simmer_basic_monthly", storePrices["simmer_basic_monthly"] ?: "$1.98 / month", false, purchasesEnabled && "simmer_basic_monthly" in storePrices, onBuy)
                }
                item {
                    SubscriptionCard(R.drawable.simmer_tier_premium, "PREMIUM", "Your daily chef coins", "No ads + 10 coins each day you open Simmer. Up to 300 coins per 30-day cycle.", "simmer_premium_monthly", storePrices["simmer_premium_monthly"] ?: "$5.99 / month", false, purchasesEnabled && "simmer_premium_monthly" in storePrices, onBuy)
                }
                item {
                    SubscriptionCard(R.drawable.simmer_tier_chef_elite, "CHEF ELITE", "The full chef experience", "No ads + 20 coins each day you visit, plus 100 coins with every successful monthly payment.", "simmer_chef_elite_monthly", storePrices["simmer_chef_elite_monthly"] ?: "$11.99 / month", true, purchasesEnabled && "simmer_chef_elite_monthly" in storePrices, onBuy)
                }
                item {
                    Text("Daily coins are claimed once per calendar day. Unclaimed coins do not roll over. Elite's 100-coin bonus is credited after each verified payment.", Modifier.padding(horizontal = 4.dp, vertical = 4.dp), color = ShopInk.copy(alpha = .68f), fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                items(listOf(
                    CoinPack("coins_098", 20, "$0.98", R.drawable.simmer_pack_20),
                    CoinPack("coins_199", 45, "$1.99", R.drawable.simmer_pack_45),
                    CoinPack("coins_399", 100, "$3.99", R.drawable.simmer_pack_100),
                    CoinPack("coins_999", 260, "$9.99", R.drawable.simmer_pack_260),
                    CoinPack("coins_1999", 550, "$19.99", R.drawable.simmer_pack_550),
                )) { pack ->
                    Card(colors = CardDefaults.cardColors(containerColor = ShopCards[(pack.coins / 5) % ShopCards.size]), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(3.dp)) {
                        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Image(painterResource(pack.imageRes), contentDescription = "${pack.coins} Simmer coins", modifier = Modifier.size(82.dp), contentScale = ContentScale.Fit)
                            Column(Modifier.weight(1f)) {
                                Text("${pack.coins} coins", color = ShopInk, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                Text("${pack.coins} Chef AI replies", color = ShopInk.copy(alpha = .65f), fontSize = 13.sp)
                                Text(storePrices[pack.productId] ?: pack.defaultPrice, color = ShopPink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            val available = purchasesEnabled && pack.productId in storePrices
                            Button(onClick = { onBuy(pack.productId, false) }, enabled = available, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = ShopPink)) { Text(if (available) "Get" else "Soon") }
                        }
                    }
                }
            }
        }
    }
}

private data class CoinPack(val productId: String, val coins: Int, val defaultPrice: String, val imageRes: Int)

@Composable
private fun SubscriptionCard(
    imageRes: Int,
    label: String,
    title: String,
    description: String,
    productId: String,
    price: String,
    featured: Boolean,
    enabled: Boolean,
    onBuy: (String, Boolean) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = if (featured) Color(0xFFFFE9A6) else ShopCream), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(3.dp)) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(imageRes), contentDescription = "$label subscription", modifier = Modifier.size(width = 100.dp, height = 112.dp), contentScale = ContentScale.Fit)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(label, color = if (featured) ShopPink else ShopGreen, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.5.sp)
                        if (featured) Text("✦ BEST VALUE", color = ShopPink, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                    Text(title, color = ShopInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(description, color = ShopInk.copy(alpha = .76f), fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(price, color = ShopInk, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Button(onClick = { onBuy(productId, true) }, enabled = enabled, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = if (featured) ShopPink else ShopGreen)) { Text(if (enabled) "Subscribe" else "Coming soon") }
            }
        }
    }
}

@Composable
fun ChefScreen(
    token: String,
    recipes: List<Meal>,
    initialRecipe: Meal?,
    balance: Int,
    onBalanceChanged: (Int) -> Unit,
    onShop: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var persona by remember { mutableStateOf("female") }
    var selectedRecipe by remember { mutableStateOf<Meal?>(initialRecipe) }
    var recipeMenu by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf<List<ChefChatHistory>>(emptyList()) }
    var conversationId by remember { mutableStateOf<Int?>(null) }
    var messages by remember { mutableStateOf<List<ChefChatMessage>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var reportMessage by remember { mutableStateOf<ChefChatMessage?>(null) }
    var error by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    val avatar = if (persona == "female") R.drawable.chef_avatar_female else R.drawable.chef_avatar_male
    val avatarDescription = if (persona == "female") "Chef Sazón, the sweet Simmer chef" else "Chef Mateo, the professional Simmer chef"

    suspend fun refreshHistory() {
        val json = chefApi("conversations/", token, JSONObject(), "GET")
        val array = json.optJSONArray("results") ?: JSONArray()
        history = (0 until array.length()).map { array.getJSONObject(it).let { row ->
            ChefChatHistory(row.optInt("id"), row.optString("title"), row.optString("persona", "female"), row.optInt("recipe_id").takeIf { id -> id > 0 }, row.optString("recipe_title"), row.optString("expires_at"))
        } }
    }
    LaunchedEffect(token) { runCatching { refreshHistory() }.onFailure { error = it.message ?: "Couldn't load chat history." } }
    LaunchedEffect(initialRecipe?.id) {
        if (initialRecipe != null) {
            selectedRecipe = initialRecipe
            conversationId = null
            messages = emptyList()
            showHistory = false
            info = "Ask anything about ${initialRecipe.title}. This chat will remember its recipe context."
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Image(painterResource(avatar), contentDescription = avatarDescription, modifier = Modifier.size(58.dp).clip(CircleShape))
                Column {
                    Text(if (persona == "female") "Chef Sazón" else "Chef Mateo", color = ShopInk, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(if (persona == "female") "Sweet, cozy & encouraging" else "Professional & reassuring", color = ShopGreen, fontSize = 12.sp)
                }
            }
            CoinBalancePill(balance, onShop)
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = persona == "female", onClick = { persona = "female" }, label = { Text("Sazón ♡") })
            FilterChip(selected = persona == "male", onClick = { persona = "male" }, label = { Text("Mateo ✦") })
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { showHistory = !showHistory }) { Text(if (showHistory) "Back to chat" else "History", color = ShopPink) }
        }
        if (selectedRecipe != null || conversationId == null) {
            Box {
                OutlinedButton(onClick = { recipeMenu = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) {
                    Text(selectedRecipe?.let { "Recipe: ${it.title}" } ?: "Choose a recipe (optional)", color = ShopInk, maxLines = 1)
                }
                DropdownMenu(expanded = recipeMenu, onDismissRequest = { recipeMenu = false }) {
                    DropdownMenuItem(text = { Text("General cooking chat") }, onClick = { selectedRecipe = null; conversationId = null; messages = emptyList(); recipeMenu = false })
                    recipes.forEach { meal -> DropdownMenuItem(text = { Text(meal.title, maxLines = 1) }, onClick = { selectedRecipe = meal; conversationId = null; messages = emptyList(); recipeMenu = false }) }
                }
            }
        }
        if (info.isNotBlank()) Text(info, Modifier.padding(top = 4.dp), color = ShopGreen, fontSize = 12.sp)
        if (error.isNotBlank()) Text(error, Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.error, fontSize = 13.sp)

        if (showHistory) {
            LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                item { Text("Your recent cooking chats · kept for 3 days", color = ShopInk, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                if (history.isEmpty()) item { Text("Your saved chats will appear here.", color = ShopInk.copy(alpha = .65f)) }
                items(history, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth().clickable {
                        scope.launch {
                            busy = true; error = ""
                            runCatching {
                                val data = chefApi("conversations/${item.id}/", token, JSONObject(), "GET")
                                conversationId = item.id
                                persona = data.optString("persona", "female")
                                selectedRecipe = recipes.firstOrNull { it.id == item.recipeId }
                                val rows = data.optJSONArray("messages") ?: JSONArray()
                                messages = (0 until rows.length()).map { i -> rows.getJSONObject(i).let { ChefChatMessage(it.optString("role"), it.optString("text"), it.optInt("id")) } }
                                showHistory = false
                                info = "This chat is available for 3 days after its last message."
                            }.onFailure { error = it.message ?: "Couldn't open this chat." }
                            busy = false
                        }
                    }, colors = CardDefaults.cardColors(containerColor = ShopCards[item.id % ShopCards.size]), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(item.title, color = ShopInk, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(item.recipeTitle.ifBlank { "General cooking chat" } + " · kept for 3 days", color = ShopInk.copy(alpha = .67f), fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                if (messages.isEmpty()) item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE4FF)), shape = RoundedCornerShape(22.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Image(painterResource(avatar), contentDescription = avatarDescription, modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)))
                            Column {
                                Text("Hi, I'm ${if (persona == "female") "Chef Sazón" else "Chef Mateo"}!", color = ShopInk, fontWeight = FontWeight.Bold)
                                Text("Tell me what's cooking. I'll keep our conversation for 3 days and use just one coin per reply.", color = ShopInk.copy(alpha = .72f), fontSize = 13.sp, lineHeight = 17.sp)
                            }
                        }
                    }
                }
                items(messages) { msg ->
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (msg.role == "user") Alignment.End else Alignment.Start) {
                        Surface(color = if (msg.role == "user") Color(0xFFFFDDF2) else Color(0xFFE8E0FF), shape = RoundedCornerShape(18.dp), shadowElevation = 1.dp) {
                            Text(msg.text, Modifier.fillMaxWidth(.88f).padding(horizontal = 13.dp, vertical = 10.dp), color = ShopInk, fontSize = 14.sp, lineHeight = 20.sp)
                        }
                        if (msg.role == "assistant") TextButton(onClick = { reportMessage = msg }) { Text("Report this response", color = ShopGreen, fontSize = 11.sp) }
                    }
                }
                if (busy) item { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { CircularProgressIndicator(Modifier.size(18.dp), color = ShopPink, strokeWidth = 2.dp); Text("Your chef is thinking…", color = ShopGreen) } }
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = draft, onValueChange = { draft = it.take(2000) }, modifier = Modifier.weight(1f), placeholder = { Text("Ask a cooking question…") }, maxLines = 4, shape = RoundedCornerShape(18.dp), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { /* use the send button */ }))
            Button(enabled = !busy && draft.isNotBlank(), onClick = {
                val question = draft.trim()
                draft = ""
                scope.launch {
                    busy = true; error = ""; info = ""
                    try {
                        val body = JSONObject().put("text", question).put("persona", persona)
                        conversationId?.let { body.put("conversation_id", it) }
                        selectedRecipe?.let { body.put("recipe_id", it.id) }
                        val result = chefApi("messages/", token, body, "POST")
                        val userMessage = result.getJSONObject("user_message")
                        val chefMessage = result.getJSONObject("assistant_message")
                        messages = messages + ChefChatMessage("user", userMessage.optString("text"), userMessage.optInt("id")) + ChefChatMessage("assistant", chefMessage.optString("text"), chefMessage.optInt("id"))
                        conversationId = result.getJSONObject("conversation").optInt("id").takeIf { it > 0 }
                        onBalanceChanged(result.optInt("balance", balance - 1))
                        refreshHistory()
                    } catch (failure: Exception) {
                        draft = question
                        error = failure.message ?: "The chef couldn't answer. Try again."
                        runCatching { onBalanceChanged(simmerRequest("coins/balance/", token, null, "GET").let(::JSONObject).optInt("balance", balance)) }
                    } finally { busy = false }
                }
            }, shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = ShopPink)) { Text("Ask") }
        }
        Text("The Simmer chef is AI and may make mistakes. Check allergens and food safety. One coin per successful reply.", Modifier.padding(bottom = 7.dp), color = ShopInk.copy(alpha = .55f), fontSize = 10.sp, lineHeight = 13.sp)
    }
    reportMessage?.let { reported ->
        AlertDialog(
            onDismissRequest = { reportMessage = null },
            title = { Text("Report AI response") },
            text = { Text("Send this response to the Simmer team for review? Choose this for offensive or unsafe content.") },
            confirmButton = { TextButton(onClick = {
                val message = reported
                reportMessage = null
                scope.launch {
                    runCatching {
                        chefApi("messages/${message.id}/report/", token, JSONObject().put("reason", "inappropriate"), "POST")
                    }.onSuccess { info = "Thanks. Your report was sent for review." }
                        .onFailure { error = it.message ?: "Could not send the report." }
                }
            }) { Text("Send report", color = ShopPink) } },
            dismissButton = { TextButton(onClick = { reportMessage = null }) { Text("Cancel") } },
        )
    }
}

private suspend fun chefApi(path: String, token: String, body: JSONObject, method: String): JSONObject = withContext(Dispatchers.IO) {
    val response = simmerRequest("chef/$path", token, body, method)
    JSONObject(response.ifBlank { "{}" })
}
