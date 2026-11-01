package com.oneos.launcher

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ChatMsg(val fromUser: Boolean, val text: String)

/** Local intent parser. Returns a reply if the command was handled, or null. */
fun handleCommand(raw: String, s: OsState, n: Nav, ctx: Context): String? {
    val t = raw.lowercase().trim()
    val aliases = mapOf("browser" to "browser", "internet" to "browser", "web" to "browser", "settings" to "settings", "calculator" to "calculator",
        "notes" to "notes", "reminders" to "reminders", "calendar" to "calendar", "files" to "files", "photos" to "photos", "gallery" to "photos",
        "camera" to "camera", "weather" to "weather", "maps" to "maps", "music" to "music", "contacts" to "contacts", "messages" to "messages",
        "mail" to "mail", "email" to "mail", "facetime" to "facetime", "play store" to "playstore", "playstore" to "playstore", "clock" to "clock",
        "timer" to "clock", "stopwatch" to "clock", "onedesk" to "desk", "one desk" to "desk", "dex" to "desk", "desktop" to "desk")
    if (Regex("(turn on|enable|start|open|launch) (onedesk|one desk|dex|desktop)").containsMatchIn(t)) { n.aiOpen = false; n.screen = "desk"; return "OneDesk is on." }
    if (Regex("(turn off|disable|exit) (onedesk|one desk|dex|desktop)").containsMatchIn(t)) { n.aiOpen = false; n.home(); return "OneDesk is off." }
    if (t.contains("lock")) { n.aiOpen = false; n.screen = "lock"; return "Locked." }
    if (t == "home" || t.contains("go home")) { n.aiOpen = false; n.home(); return "Going home." }
    if (t.startsWith("search ") || t.startsWith("google ")) { n.browserUrl = normalizeUrl(raw.substringAfter(' ')); n.aiOpen = false; n.screen = "app:browser"; return "Searching…" }
    Regex("(dark mode|light mode)").find(t)?.let { s.darkMode = it.value == "dark mode"; s.save(); return "Switched to ${it.value}." }
    Regex("^(open|launch|start|go to|show)\\s+(.+)$").find(t)?.let { m ->
        val target = m.groupValues[2].removePrefix("the ").removeSuffix(" app").trim()
        aliases[target]?.let { id -> n.aiOpen = false; n.open(id); return "Opening ${appById(id)?.name}." }
        val pm = ctx.packageManager
        val hit = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .firstOrNull { it.loadLabel(pm).toString().lowercase().contains(target) }
        if (hit != null) { pm.getLaunchIntentForPackage(hit.activityInfo.packageName)?.let { safeStart(ctx, it) }; return "Opening ${hit.loadLabel(pm)}." }
        return "I couldn't find an app called \"$target\"."
    }
    if (t in listOf("help", "what can you do", "?")) return "Try: \"open settings\", \"open browser\", \"turn on OneDesk\", \"search cats\", \"open YouTube\", \"dark mode\", \"lock\"."
    return null
}

suspend fun askLlm(s: OsState, history: List<ChatMsg>): String = withContext(Dispatchers.IO) {
    runCatching {
        val c = URL(s.apiEndpoint).openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.doOutput = true; c.connectTimeout = 15000; c.readTimeout = 60000
        c.setRequestProperty("Content-Type", "application/json"); c.setRequestProperty("Authorization", "Bearer ${s.apiKey}")
        val msgs = JSONArray().put(JSONObject().put("role", "system").put("content", "You are One AI, the helpful assistant built into OneOS. Be brief."))
        history.takeLast(12).forEach { msgs.put(JSONObject().put("role", if (it.fromUser) "user" else "assistant").put("content", it.text)) }
        c.outputStream.use { it.write(JSONObject().put("model", s.apiModel).put("messages", msgs).toString().toByteArray()) }
        val body = (if (c.responseCode in 200..299) c.inputStream else c.errorStream).bufferedReader().readText()
        if (c.responseCode !in 200..299) "API error ${c.responseCode}: ${body.take(200)}"
        else JSONObject(body).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
    }.getOrElse { "Network error: ${it.message}" }
}

@Composable
fun AiOverlay(s: OsState, n: Nav) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val msgs = remember { mutableStateListOf(ChatMsg(false, "Hi${if (s.userName.isNotBlank()) " ${s.userName}" else ""}! I'm One AI. Ask me to open apps, turn on OneDesk, or search the web.")) }
    var input by remember { mutableStateOf("") }
    val list = rememberLazyListState()
    LaunchedEffect(msgs.size) { list.animateScrollToItem(msgs.lastIndex) }
    fun send() {
        val q = input.trim(); if (q.isEmpty()) return; input = ""; msgs.add(ChatMsg(true, q))
        val r = handleCommand(q, s, n, ctx)
        if (r != null) msgs.add(ChatMsg(false, r))
        else if (s.apiKey.isNotBlank()) scope.launch { msgs.add(ChatMsg(false, askLlm(s, msgs.toList()))) }
        else msgs.add(ChatMsg(false, "I can handle device commands locally (say \"help\"). Add an API key in Settings › One AI for open-ended chat."))
    }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(.45f)).clickable(interactionSource = null, indication = null) { n.aiOpen = false }) {
        Column(Modifier.align(Alignment.BottomCenter).widthIn(max = 560.dp).fillMaxWidth().fillMaxHeight(.65f).navigationBarsPadding().imePadding().padding(12.dp)
            .clickable(interactionSource = null, indication = null) {}
            .clip(RoundedCornerShape(32.dp)).background(Color(0xCC1C1C1E))
            .border(1.5.dp, Brush.linearGradient(listOf(Color(0xFFBF5AF2), Color(0xFF0A84FF), Color(0xFFFF375F))), RoundedCornerShape(32.dp)).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.AutoAwesome, null, tint = Color(0xFFBF5AF2)); Text("  One AI", color = Color.White, fontSize = 18.sp) }
            LazyColumn(Modifier.weight(1f).padding(vertical = 8.dp), state = list) { items(msgs) { m ->
                Box(Modifier.fillMaxWidth(), contentAlignment = if (m.fromUser) Alignment.CenterEnd else Alignment.CenterStart) {
                    Text(m.text, color = Color.White, modifier = Modifier.padding(4.dp).widthIn(max = 380.dp).clip(RoundedCornerShape(18.dp))
                        .background(if (m.fromUser) Color(s.accent) else Color.White.copy(.1f)).padding(12.dp)) } } }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(input, { input = it }, Modifier.weight(1f), singleLine = true, placeholder = { Text("Ask One AI…") },
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { send() }))
                IconButton(onClick = { send() }) { Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color(s.accent)) }
            }
        }
    }
}
