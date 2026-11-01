package com.oneos.launcher

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*

val accents = listOf(0xFF0A84FF, 0xFFBF5AF2, 0xFF30D158, 0xFFFF9F0A, 0xFFFF375F, 0xFF64D2FF).map { it.toInt() }

@Composable
fun SetupScreen(s: OsState, onDone: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    val steps = 6
    Box(Modifier.fillMaxSize()) {
        Wallpaper(s, 30f)
        GlassCard(Modifier.align(Alignment.Center).widthIn(max = 520.dp).fillMaxWidth().padding(20.dp), radius = 36.dp) {
            AnimatedContent(step, label = "setup") { st ->
                Column(Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    when (st) {
                        0 -> { Text("Hello", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Thin)
                            Text("Welcome to OneOS", color = Color.White.copy(.8f), fontSize = 20.sp) }
                        1 -> { Title("Language")
                            listOf("English", "Español", "Français", "Kreyòl Ayisyen", "Deutsch", "한국어").forEach { l ->
                                Choice(l, s.language == l) { s.language = l } } }
                        2 -> { Title("Choose a Wi‑Fi Network")
                            listOf("Home-5G", "OneNet", "Guest").forEach { Choice(it, false) {} }
                            Text("Wi‑Fi is managed by Android. You can skip this step.", color = Color.White.copy(.6f), fontSize = 13.sp, textAlign = TextAlign.Center) }
                        3 -> { Title("What's your name?")
                            OutlinedTextField(s.userName, { s.userName = it }, singleLine = true, placeholder = { Text("Name") }, modifier = Modifier.fillMaxWidth()) }
                        4 -> { Title("Appearance")
                            Choice("Dark", s.darkMode) { s.darkMode = true }
                            Choice("Light tint", !s.darkMode) { s.darkMode = false }
                            Row(Modifier.padding(top = 12.dp)) { accents.forEach { c ->
                                Box(Modifier.padding(6.dp).size(34.dp).clip(CircleShape).background(Color(c))
                                    .border(if (s.accent == c) 3.dp else 0.dp, Color.White, CircleShape).clickable { s.accent = c }) } } }
                        else -> { Icon(Icons.Filled.CheckCircle, null, tint = Color(s.accent), modifier = Modifier.size(72.dp))
                            Title("You're all set${if (s.userName.isNotBlank()) ", ${s.userName}" else ""}!")
                            Text("Tip: set OneOS as your default Home app in Settings.", color = Color.White.copy(.7f), textAlign = TextAlign.Center) }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) Text("Back", color = Color.White.copy(.8f), modifier = Modifier.clickable { step-- }.padding(10.dp))
                Spacer(Modifier.weight(1f))
                GlassButton(if (step == steps - 1) "Get Started" else "Continue", s) {
                    if (step < steps - 1) step++ else { s.setupDone = true; s.save(); onDone() }
                }
            }
        }
    }
}

@Composable private fun Title(t: String) = Text(t, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp), textAlign = TextAlign.Center)

@Composable
fun Choice(t: String, sel: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(if (sel) .18f else .06f))
        .clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(t, color = Color.White, modifier = Modifier.weight(1f))
        if (sel) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
    }
}
