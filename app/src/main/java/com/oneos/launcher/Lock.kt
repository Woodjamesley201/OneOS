package com.oneos.launcher

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LockScreen(s: OsState, onUnlock: () -> Unit) {
    val now = rememberNow()
    var drag by remember { mutableStateOf(0f) }
    Box(Modifier.fillMaxSize().pointerInput(Unit) {
        detectVerticalDragGestures(onDragEnd = { if (drag < -150f) onUnlock(); drag = 0f }) { _, d -> drag += d }
    }.clickable { onUnlock() }) {
        Wallpaper(s)
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Lock, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(10.dp))
            Text(SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now), color = Color.White.copy(.85f), fontSize = 20.sp, fontWeight = FontWeight.Medium)
            val (w, f) = when (s.clockStyle) {
                "Bold" -> FontWeight.Black to FontFamily.Default
                "Thin" -> FontWeight.Thin to FontFamily.Default
                "Mono" -> FontWeight.Light to FontFamily.Monospace
                else -> FontWeight.SemiBold to FontFamily.SansSerif
            }
            Text(SimpleDateFormat("h:mm", Locale.getDefault()).format(now), color = Color.White, fontSize = 110.sp, fontWeight = w, fontFamily = f)
            Spacer(Modifier.weight(1f))
            GlassCard(Modifier.padding(16.dp).widthIn(max = 420.dp).fillMaxWidth()) {
                Text("OneOS", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(if (s.userName.isNotBlank()) "Welcome back, ${s.userName}" else "Welcome back", color = Color.White.copy(.7f))
            }
            Text("Swipe up or tap to unlock", color = Color.White.copy(.7f), modifier = Modifier.navigationBarsPadding().padding(bottom = 24.dp))
        }
    }
}
