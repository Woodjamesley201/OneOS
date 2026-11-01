package com.oneos.launcher

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

const val REPO = "Woodjamesley201/OneOS"

@Composable
fun SettingsApp(s: OsState, n: Nav, onClose: () -> Unit) {
    var page by remember { mutableStateOf("main") }
    val ctx = LocalContext.current
    AppScaffold(if (page == "main") "Settings" else page, { if (page == "main") onClose() else page = "main" }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            when (page) {
                "main" -> {
                    GlassCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(52.dp).clip(CircleShape).background(Color(s.accent)), contentAlignment = Alignment.Center) {
                                Text(s.userName.take(1).uppercase().ifEmpty { "O" }, color = Color.White, fontSize = 22.sp) }
                            Column(Modifier.padding(start = 12.dp)) { Text(s.userName.ifBlank { "OneOS User" }, color = Color.White, fontSize = 18.sp)
                                Text("OneOS ${BuildConfig.VERSION_NAME}", color = Color.White.copy(.6f)) } } }
                    Group { Row2(Icons.Filled.SystemUpdate, "Software Update", Color(0xFF8E8E93)) { page = "Software Update" }
                        Row2(Icons.Filled.Memory, "System Specs", Color(0xFF0A84FF)) { page = "System Specs" } }
                    Group { Row2(Icons.Filled.Extension, "Good Lock", Color(0xFFBF5AF2)) { page = "Good Lock" }
                        Row2(Icons.Filled.Wallpaper, "Display & Wallpaper", Color(0xFF64D2FF)) { page = "Display & Wallpaper" }
                        Row2(Icons.Filled.AutoAwesome, "One AI", Color(0xFFFF375F)) { page = "One AI" }
                        Row2(Icons.Filled.DesktopWindows, "OneDesk", Color(0xFF5E5CE6)) { n.screen = "desk" } }
                    Group { Row2(Icons.Filled.Home, "Set as Default Home App", Color(0xFF30D158)) { page = "Default" }
                        Row2(Icons.Filled.Android, "Android System Settings", Color(0xFF30D158)) { safeStart(ctx, Intent(Settings.ACTION_SETTINGS)) }
                        Row2(Icons.Filled.Info, "About OneOS", Color(0xFF8E8E93)) { page = "About OneOS" } }
                }
                "System Specs" -> SpecsPage()
                "Software Update" -> UpdatePage(s)
                "Good Lock" -> GoodLockPage(s)
                "Display & Wallpaper" -> DisplayPage(s)
                "One AI" -> AiSettingsPage(s)
                "Default" -> {
                    Text("To make OneOS your launcher, choose it as the Home app. Pressing Home will then open OneOS.", color = Color.White.copy(.8f))
                    Spacer(Modifier.height(12.dp))
                    GlassButton("Open Home App Settings", s) { safeStart(ctx, Intent(Settings.ACTION_HOME_SETTINGS), Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS), Intent(Settings.ACTION_SETTINGS)) } }
                else -> GlassCard(Modifier.fillMaxWidth()) {
                    Text("OneOS", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", color = Color.White.copy(.7f))
                    Text("A liquid-glass launcher inspired by One UI and iPadOS. Includes OneDesk, One AI, One Browser and Good Lock.", color = Color.White.copy(.8f), modifier = Modifier.padding(top = 8.dp))
                    Text("github.com/$REPO", color = Color(s.accent), modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}


@Composable fun Group(content: @Composable ColumnScope.() -> Unit) = Column(Modifier.fillMaxWidth().padding(vertical = 8.dp).glass(20.dp), content = content)

@Composable
fun Row2(icon: ImageVector, label: String, color: Color, value: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(color), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        Text(label, color = Color.White, modifier = Modifier.weight(1f).padding(start = 12.dp))
        if (value != null) Text(value, color = Color.White.copy(.6f))
        Icon(Icons.Filled.ChevronRight, null, tint = Color.White.copy(.4f))
    }
}

@Composable fun Spec(k: String, v: String) = Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
    Text(k, color = Color.White.copy(.7f), modifier = Modifier.weight(1f)); Text(v, color = Color.White, fontWeight = FontWeight.Medium) }

fun gb(b: Long) = "%.1f GB".format(b / 1_073_741_824.0)

@Composable
fun SpecsPage() {
    val ctx = LocalContext.current
    val mi = remember { ActivityManager.MemoryInfo().also { (ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(it) } }
    val st = remember { StatFs(Environment.getDataDirectory().path) }
    val dm = ctx.resources.displayMetrics
    GlassCard(Modifier.fillMaxWidth()) {
        Spec("Model", "${Build.MANUFACTURER} ${Build.MODEL}")
        Spec("Device", Build.DEVICE)
        Spec("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        Spec("Security patch", Build.VERSION.SECURITY_PATCH)
        Spec("CPU ABI", Build.SUPPORTED_ABIS.joinToString())
        Spec("CPU cores", "${Runtime.getRuntime().availableProcessors()}")
        Spec("Hardware", Build.HARDWARE)
        Spec("RAM", "${gb(mi.availMem)} free / ${gb(mi.totalMem)}")
        Spec("Storage", "${gb(st.availableBytes)} free / ${gb(st.totalBytes)}")
        Spec("Screen", "${dm.widthPixels}×${dm.heightPixels} @ ${dm.densityDpi} dpi")
        Spec("OneOS", BuildConfig.VERSION_NAME)
    }
}

@Composable
fun UpdatePage(s: OsState) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Tap Check for Updates") }
    var dl by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    GlassCard(Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.SystemUpdate, null, tint = Color(s.accent), modifier = Modifier.size(48.dp))
        Text("OneOS ${BuildConfig.VERSION_NAME}", color = Color.White, fontSize = 20.sp)
        Text(status, color = Color.White.copy(.8f), modifier = Modifier.padding(vertical = 8.dp))
        if (notes.isNotBlank()) Text(notes.take(600), color = Color.White.copy(.6f), fontSize = 13.sp)
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        GlassButton("Check for Updates", s) {
            busy = true; status = "Checking…"
            scope.launch {
                val r = withContext(Dispatchers.IO) { runCatching { JSONObject(URL("https://api.github.com/repos/$REPO/releases/latest").readText()) }.getOrNull() }
                busy = false
                if (r == null) { status = "Couldn't reach GitHub."; return@launch }
                val tag = r.optString("tag_name").removePrefix("v")
                notes = r.optString("body")
                val assets = r.optJSONArray("assets")
                dl = (0 until (assets?.length() ?: 0)).map { assets!!.getJSONObject(it) }.firstOrNull { it.getString("name").endsWith(".apk") }
                    ?.getString("browser_download_url") ?: r.optString("html_url")
                val newer = tag.split('.').mapNotNull { it.toIntOrNull() }.let { a ->
                    val b = BuildConfig.VERSION_NAME.split('.').mapNotNull { it.toIntOrNull() }
                    (0 until maxOf(a.size, b.size)).firstOrNull { (a.getOrElse(it) { 0 }) != (b.getOrElse(it) { 0 }) }?.let { a.getOrElse(it) { 0 } > b.getOrElse(it) { 0 } } ?: false }
                status = if (newer) "Update available: OneOS $tag" else "OneOS is up to date (latest: $tag)"
                if (!newer) dl = null
            }
        }
        dl?.let { u -> Spacer(Modifier.height(8.dp)); GlassButton("Download & Install", s) { safeStart(ctx, Intent(Intent.ACTION_VIEW, Uri.parse(u))) } }
    }
}

@Composable
fun <T> Picker(label: String, options: List<T>, sel: T, name: (T) -> String = { it.toString() }, onSel: (T) -> Unit) {
    Text(label, color = Color.White.copy(.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
    Row(Modifier.horizontalScroll(rememberScrollState())) { options.forEach { o -> Choice2(name(o), o == sel) { onSel(o) } } }
}

@Composable
fun GoodLockPage(s: OsState) {
    Text("Customize OneOS with Good Lock modules.", color = Color.White.copy(.7f))
    GlassCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) { Text("DockStyle", color = Color.White, fontWeight = FontWeight.Bold)
        Picker("Dock style", listOf("Glass", "Solid", "Floating", "Minimal"), s.dockStyle) { s.dockStyle = it; s.save() } }
    GlassCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) { Text("Home Up", color = Color.White, fontWeight = FontWeight.Bold)
        Text("Icon size: ${s.iconSize.toInt()}dp", color = Color.White.copy(.7f))
        Slider(s.iconSize, { s.iconSize = it }, valueRange = 44f..84f, onValueChangeFinished = { s.save() }) }
    GlassCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) { Text("Theme Park", color = Color.White, fontWeight = FontWeight.Bold)
        Row { accents.forEach { c -> Box(Modifier.padding(6.dp).size(34.dp).clip(CircleShape).background(Color(c))
            .border(if (s.accent == c) 3.dp else 0.dp, Color.White, CircleShape).clickable { s.accent = c; s.save() }) } } }
    GlassCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) { Text("Liquid Glass", color = Color.White, fontWeight = FontWeight.Bold)
        Text("Blur strength: ${s.blur.toInt()} (Android 12+)", color = Color.White.copy(.7f))
        Slider(s.blur, { s.blur = it }, valueRange = 0f..60f, onValueChangeFinished = { s.save() }) }
    GlassCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) { Text("LockStar", color = Color.White, fontWeight = FontWeight.Bold)
        Picker("Lock screen clock", listOf("Classic", "Bold", "Thin", "Mono"), s.clockStyle) { s.clockStyle = it; s.save() } }
}

@Composable
fun DisplayPage(s: OsState) {
    GlassCard(Modifier.fillMaxWidth()) {
        Picker("Appearance", listOf(true, false), s.darkMode, { if (it) "Dark" else "Light tint" }) { s.darkMode = it; s.save() }
        Text("Wallpaper", color = Color.White.copy(.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState())) { wallpapers.forEachIndexed { i, c ->
            Box(Modifier.padding(6.dp).size(70.dp, 120.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(c))
                .border(if (s.wallpaper == i) 3.dp else 0.dp, Color.White, RoundedCornerShape(14.dp)).clickable { s.wallpaper = i; s.save() }) } }
    }
}

@Composable
fun AiSettingsPage(s: OsState) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text("One AI works offline for device commands. Add an OpenAI-compatible API key for open-ended chat.", color = Color.White.copy(.8f))
        OutlinedTextField(s.apiKey, { s.apiKey = it; s.save() }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("API key") }, singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
        OutlinedTextField(s.apiEndpoint, { s.apiEndpoint = it; s.save() }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Endpoint") }, singleLine = true)
        OutlinedTextField(s.apiModel, { s.apiModel = it; s.save() }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Model") }, singleLine = true)
    }
}
