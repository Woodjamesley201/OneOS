package com.oneos.launcher

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView

class BrowserTab(val id: Long, val view: WebView) { var title by mutableStateOf("New Tab"); var url by mutableStateOf("") }

fun normalizeUrl(t: String): String {
    val x = t.trim()
    return if (x.startsWith("http://") || x.startsWith("https://")) x
    else if (x.contains('.') && !x.contains(' ')) "https://$x" else "https://duckduckgo.com/?q=" + java.net.URLEncoder.encode(x, "UTF-8")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserApp(s: OsState, n: Nav, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val tabs = remember { mutableStateListOf<BrowserTab>() }
    var cur by remember { mutableStateOf(0) }
    var showTabs by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(100) }
    fun newTab(url: String) {
        val t = BrowserTab(System.nanoTime(), WebView(ctx))
        t.view.settings.javaScriptEnabled = true; t.view.settings.domStorageEnabled = true
        t.view.webViewClient = object : WebViewClient() {
            override fun onPageStarted(v: WebView?, u: String?, f: Bitmap?) { t.url = u ?: ""; if (tabs.getOrNull(cur) == t) address = t.url }
            override fun onPageFinished(v: WebView?, u: String?) { t.title = v?.title ?: u ?: ""; t.url = u ?: "" }
        }
        t.view.webChromeClient = object : WebChromeClient() { override fun onProgressChanged(v: WebView?, p: Int) { progress = p } }
        t.view.loadUrl(url); tabs.add(t); cur = tabs.lastIndex; address = url
    }
    LaunchedEffect(Unit) { newTab(n.browserUrl ?: "https://duckduckgo.com"); n.browserUrl = null }
    DisposableEffect(Unit) { onDispose { tabs.forEach { it.view.destroy() } } }
    val tab = tabs.getOrNull(cur)
    Column(Modifier.fillMaxSize().background(Color(0xFF0E0E10)).statusBarsPadding().navigationBarsPadding()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (showTabs) Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
                tabs.forEachIndexed { i, t -> Row(Modifier.fillMaxWidth().padding(4.dp).glass(18.dp).clickable { cur = i; address = t.url; showTabs = false }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(t.title, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.clickable {
                        t.view.destroy(); tabs.removeAt(i); if (tabs.isEmpty()) newTab("https://duckduckgo.com"); cur = cur.coerceIn(0, tabs.lastIndex) }) } }
                GlassButton("+ New Tab", s, Modifier.padding(8.dp)) { newTab("https://duckduckgo.com"); showTabs = false }
            } else if (tab != null) key(tab.id) {
                AndroidView(factory = { (tab.view.parent as? ViewGroup)?.removeView(tab.view); tab.view }, modifier = Modifier.fillMaxSize())
            }
            if (progress < 100) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(2.dp))
        }
        Column(Modifier.fillMaxWidth().padding(8.dp).glass(26.dp, .14f).padding(8.dp)) {
            TextField(address, { address = it }, Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)), singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) }, placeholder = { Text("Search or enter website") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { val u = normalizeUrl(address); address = u; tab?.view?.loadUrl(u); showTabs = false }),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.White.copy(.08f), unfocusedContainerColor = Color.White.copy(.08f),
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.clickable { tab?.view?.let { if (it.canGoBack()) it.goBack() } })
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Forward", tint = Color.White, modifier = Modifier.clickable { tab?.view?.let { if (it.canGoForward()) it.goForward() } })
                Icon(Icons.Filled.Refresh, "Reload", tint = Color.White, modifier = Modifier.clickable { tab?.view?.reload() })
                Box(Modifier.clickable { showTabs = !showTabs }) { Text("${tabs.size}", color = Color.White, modifier = Modifier.border(1.5.dp, Color.White, RoundedCornerShape(5.dp)).padding(horizontal = 6.dp)) }
                Icon(Icons.Filled.Home, "Home", tint = Color.White, modifier = Modifier.clickable(onClick = onClose))
            }
        }
    }
}
