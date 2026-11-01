package com.oneos.launcher

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class DeskWindow(val id: Long, val appId: String, x: Float, y: Float) {
    var x by mutableStateOf(x); var y by mutableStateOf(y)
    var w by mutableStateOf(900f); var h by mutableStateOf(1100f)
    var z by mutableStateOf(0); var minimized by mutableStateOf(false); var maximized by mutableStateOf(false)
}

@Composable
fun DeskScreen(s: OsState, n: Nav) {
    val wins = remember { mutableStateListOf<DeskWindow>() }
    var startOpen by remember { mutableStateOf(false) }
    var topZ by remember { mutableStateOf(1) }
    val now = rememberNow(10000)
    fun focus(w: DeskWindow) { w.z = ++topZ; w.minimized = false }
    fun launch(id: String) {
        startOpen = false
        val app = appById(id) ?: return
        when {
            id == "desk" -> {}
            id == "ai" -> n.aiOpen = true
            app.external != null -> n.open(id)
            else -> { val w = DeskWindow(System.nanoTime(), id, 60f + wins.size * 50f, 60f + wins.size * 50f); wins.add(w); focus(w) }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxW = with(density) { maxWidth.toPx() }; val maxH = with(density) { maxHeight.toPx() }
        Wallpaper(s)
        // desktop icons
        Column(Modifier.statusBarsPadding().padding(16.dp)) {
            listOf("browser", "files", "notes", "settings", "playstore").forEach { id -> appById(id)?.let { AppIcon(it, 48.dp) { launch(id) } } }
        }
        wins.filter { !it.minimized }.sortedBy { it.z }.forEach { w ->
            key(w.id) {
                val wPx = if (w.maximized) maxW else w.w.coerceAtMost(maxW); val hPx = if (w.maximized) maxH - with(density) { 64.dp.toPx() } else w.h.coerceAtMost(maxH)
                Box(Modifier.offset { if (w.maximized) IntOffset(0, 0) else IntOffset(w.x.roundToInt(), w.y.roundToInt()) }
                    .size(with(density) { wPx.toDp() }, with(density) { hPx.toDp() })
                    .clip(RoundedCornerShape(18.dp)).background(Color(0xF0151517)).border(1.dp, Color.White.copy(.2f), RoundedCornerShape(18.dp))) {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().background(Color.White.copy(.08f))
                            .pointerInput(w.id) { detectDragGestures(onDragStart = { focus(w) }) { c, d -> c.consume(); w.maximized = false
                                w.x = (w.x + d.x).coerceIn(-w.w / 2, maxW - 100f); w.y = (w.y + d.y).coerceIn(0f, maxH - 150f) } }
                            .clickable { focus(w) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(13.dp).clip(CircleShape).background(Color(0xFFFF5F57)).clickable { wins.remove(w) })
                            Spacer(Modifier.width(7.dp))
                            Box(Modifier.size(13.dp).clip(CircleShape).background(Color(0xFFFEBC2E)).clickable { w.minimized = true })
                            Spacer(Modifier.width(7.dp))
                            Box(Modifier.size(13.dp).clip(CircleShape).background(Color(0xFF28C840)).clickable { w.maximized = !w.maximized })
                            Text("   " + (appById(w.appId)?.name ?: ""), color = Color.White, fontSize = 13.sp)
                        }
                        Box(Modifier.weight(1f).fillMaxWidth()) { AppContent(w.appId, s, n) { wins.remove(w) } }
                    }
                    // resize handle
                    Box(Modifier.align(Alignment.BottomEnd).size(28.dp).pointerInput(w.id) {
                        detectDragGestures(onDragStart = { focus(w) }) { c, d -> c.consume(); w.w = (w.w + d.x).coerceAtLeast(400f); w.h = (w.h + d.y).coerceAtLeast(400f) } }) {
                        Icon(Icons.Filled.OpenInFull, "Resize", tint = Color.White.copy(.5f), modifier = Modifier.size(16.dp).align(Alignment.Center))
                    }
                }
            }
        }
        if (startOpen) Column(Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start = 10.dp, bottom = 70.dp).width(360.dp).height(440.dp)
            .glass(24.dp, .18f).background(Color(0xAA101012)).padding(14.dp)) {
            Text("OneDesk", color = Color.White, fontSize = 18.sp)
            LazyVerticalGrid(GridCells.Fixed(4), Modifier.weight(1f)) { items(builtInApps) { a -> AppIcon(a, 44.dp) { launch(a.id) } } }
            Text("Exit OneDesk", color = Color(0xFFFF453A), modifier = Modifier.clickable { n.home() }.padding(8.dp))
        }
        // taskbar
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().height(56.dp).glass(0.dp, .14f).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Apps, "Start", tint = Color.White, modifier = Modifier.size(32.dp).clickable { startOpen = !startOpen })
            Spacer(Modifier.width(12.dp))
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                wins.forEach { w -> val a = appById(w.appId)
                    Row(Modifier.padding(4.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(if (w.z == topZ && !w.minimized) .22f else .08f))
                        .clickable { if (w.minimized || w.z != topZ) focus(w) else w.minimized = true }.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        a?.let { Icon(it.icon, null, tint = it.color.copy(alpha = 1f), modifier = Modifier.size(18.dp)) }
                        Text(" " + (a?.name ?: ""), color = Color.White, fontSize = 12.sp) } }
            }
            Icon(Icons.Filled.AutoAwesome, "AI", tint = Color(0xFFBF5AF2), modifier = Modifier.clickable { n.aiOpen = true }.padding(8.dp))
            Text(SimpleDateFormat("h:mm a\nMMM d", Locale.getDefault()).format(now), color = Color.White, fontSize = 11.sp)
        }
    }
}
