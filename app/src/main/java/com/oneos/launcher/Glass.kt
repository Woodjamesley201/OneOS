package com.oneos.launcher

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*

val wallpapers = listOf(
    listOf(Color(0xFF0B0B1E), Color(0xFF1B2A6B), Color(0xFF6A1B9A)),
    listOf(Color(0xFF000000), Color(0xFF0D3B2E), Color(0xFF00897B)),
    listOf(Color(0xFF120A0A), Color(0xFF6B1B1B), Color(0xFFFF6F00)),
    listOf(Color(0xFF050505), Color(0xFF1C1C1E), Color(0xFF3A3A3C)),
    listOf(Color(0xFF0A1020), Color(0xFF0A84FF), Color(0xFF5E5CE6)),
)

@Composable
fun Wallpaper(s: OsState, extraBlur: Float = 0f) {
    val c = wallpapers[s.wallpaper.coerceIn(0, wallpapers.lastIndex)]
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Box(Modifier.fillMaxSize().blur(extraBlur.dp).background(Brush.linearGradient(c)))
        Box(Modifier.size(320.dp).offset(40.dp, 120.dp).blur(80.dp).background(c[2].copy(alpha = .7f), RoundedCornerShape(50)))
        Box(Modifier.size(260.dp).align(Alignment.BottomEnd).offset((-30).dp, (-160).dp).blur(90.dp).background(c[1], RoundedCornerShape(50)))
        if (!s.darkMode) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = .25f)))
    }
}

fun Modifier.glass(radius: Dp = 28.dp, alpha: Float = 0.12f): Modifier {
    val shape = RoundedCornerShape(radius)
    return this.clip(shape)
        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = alpha + 0.06f), Color.White.copy(alpha = alpha * 0.6f))))
        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = .40f), Color.White.copy(alpha = .06f))), shape)
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, radius: Dp = 28.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.glass(radius).padding(16.dp), content = content)
}

@Composable
fun GlassButton(text: String, s: OsState? = null, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val accent = s?.let { Color(it.accent) } ?: Color(0xFF0A84FF)
    Box(modifier.clip(RoundedCornerShape(50)).background(accent.copy(alpha = .85f)).clickable(onClick = onClick)
        .padding(horizontal = 22.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontSize = 16.sp)
    }
}

@Composable
fun OneTheme(s: OsState, content: @Composable () -> Unit) {
    val accent = Color(s.accent)
    MaterialTheme(colorScheme = darkColorScheme(primary = accent, secondary = accent, background = Color.Black,
        surface = Color(0xFF1C1C1E), onSurface = Color.White, onBackground = Color.White), content = content)
}

@Composable
fun AppScaffold(title: String, onClose: () -> Unit, actions: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xF0101012)).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹ Home", color = MaterialTheme.colorScheme.primary, fontSize = 17.sp, modifier = Modifier.clickable(onClick = onClose).padding(6.dp))
            Spacer(Modifier.weight(1f))
            Text(title, color = Color.White, fontSize = 18.sp)
            Spacer(Modifier.weight(1f))
            Row(content = actions)
            Spacer(Modifier.width(56.dp))
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp), content = content)
    }
}
