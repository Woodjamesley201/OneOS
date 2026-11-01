package com.oneos.launcher

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class InstalledApp(val label: String, val pkg: String, val icon: ImageBitmap)

suspend fun loadInstalledApps(ctx: Context): List<InstalledApp> = withContext(Dispatchers.IO) {
    val pm = ctx.packageManager
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    pm.queryIntentActivities(i, 0).filter { it.activityInfo.packageName != ctx.packageName }.map {
        InstalledApp(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm).toBitmap(96, 96).asImageBitmap())
    }.distinctBy { it.pkg }.sortedBy { it.label.lowercase() }
}

@Composable
fun rememberNow(periodMs: Long = 1000): Date {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(periodMs) } }
    return now
}

fun batteryLevel(ctx: Context): Pair<Int, Boolean> {
    val b = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val lvl = b?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val sc = b?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val st = b?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    return (if (lvl >= 0) lvl * 100 / sc else 0) to (st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL)
}

@Composable
fun StatusBar() {
    val ctx = LocalContext.current
    val now = rememberNow(10000)
    val (lvl, chg) = remember(now) { batteryLevel(ctx) }
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(SimpleDateFormat("h:mm  EEE MMM d", Locale.getDefault()).format(now), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Filled.Wifi, null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("$lvl%", color = Color.White, fontSize = 13.sp)
        Icon(if (chg) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryFull, null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun AppIcon(app: OneApp, size: Dp, showLabel: Boolean = true, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(size + 24.dp).clickable(onClick = onClick).padding(vertical = 6.dp)) {
        Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.26f))
            .background(Brush.verticalGradient(listOf(app.color.copy(alpha = .95f), app.color.copy(alpha = .65f))))
            .border(1.dp, Color.White.copy(alpha = .25f), RoundedCornerShape(size * 0.26f)), contentAlignment = Alignment.Center) {
            Icon(app.icon, app.name, tint = Color.White, modifier = Modifier.size(size * 0.55f))
        }
        if (showLabel) Text(app.name, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun WidgetsRow(s: OsState) {
    val ctx = LocalContext.current
    val now = rememberNow()
    val w = rememberWeather()
    val (lvl, chg) = remember(now.time / 30000) { batteryLevel(ctx) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard(Modifier.weight(1f).height(150.dp)) {
            Text(SimpleDateFormat("EEEE", Locale.getDefault()).format(now).uppercase(), color = Color(s.accent), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(SimpleDateFormat("h:mm", Locale.getDefault()).format(now), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Light)
            Text(SimpleDateFormat("MMMM d", Locale.getDefault()).format(now), color = Color.White.copy(.7f), fontSize = 13.sp)
        }
        GlassCard(Modifier.weight(1f).height(150.dp)) {
            Text(w?.city ?: "Weather", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(w?.let { "${it.temp.toInt()}°" } ?: "--°", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Light)
            Text(w?.desc ?: "Loading…", color = Color.White.copy(.7f), fontSize = 13.sp)
        }
        GlassCard(Modifier.weight(1f).height(150.dp)) {
            Text("Battery", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(70.dp)) {
                CircularProgressIndicator(progress = { lvl / 100f }, modifier = Modifier.fillMaxSize(), color = if (lvl < 20) Color(0xFFFF453A) else Color(0xFF30D158),
                    trackColor = Color.White.copy(.15f), strokeWidth = 7.dp)
                Text("$lvl%${if (chg) "⚡" else ""}", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun Dock(s: OsState, apps: List<OneApp>, onOpen: (String) -> Unit) {
    val mod = when (s.dockStyle) {
        "Solid" -> Modifier.clip(RoundedCornerShape(32.dp)).background(Color(0xFF1C1C1E))
        "Floating" -> Modifier.glass(radius = 40.dp, alpha = .06f)
        "Minimal" -> Modifier
        else -> Modifier.glass(radius = 32.dp, alpha = .16f)
    }
    Row(mod.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        apps.forEach { a -> AppIcon(a, s.iconSize.dp, showLabel = false) { onOpen(a.id) } }
    }
}

@Composable
fun HomeScreen(s: OsState, n: Nav) {
    val ctx = LocalContext.current
    var installed by remember { mutableStateOf(listOf<InstalledApp>()) }
    LaunchedEffect(Unit) { installed = loadInstalledApps(ctx) }
    val gridApps = builtInApps.filter { it.id !in listOf("browser", "settings", "ai", "playstore") }
    val pager = rememberPagerState { 2 }
    Box(Modifier.fillMaxSize()) {
        Wallpaper(s)
        Column(Modifier.fillMaxSize()) {
            StatusBar()
            HorizontalPager(pager, Modifier.weight(1f)) { page ->
                if (page == 0) Column(Modifier.fillMaxSize().padding(top = 12.dp)) {
                    WidgetsRow(s)
                    Spacer(Modifier.height(12.dp))
                    LazyVerticalGrid(GridCells.Adaptive(s.iconSize.dp + 30.dp), Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                        items(gridApps) { a -> Box(contentAlignment = Alignment.Center) { AppIcon(a, s.iconSize.dp) { n.open(a.id) } } }
                    }
                } else Column(Modifier.fillMaxSize().padding(12.dp)) {
                    Text("All Apps", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
                    LazyVerticalGrid(GridCells.Adaptive(s.iconSize.dp + 30.dp), Modifier.fillMaxSize()) {
                        items(installed) { a ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                                ctx.packageManager.getLaunchIntentForPackage(a.pkg)?.let { safeStart(ctx, it) }
                            }.padding(6.dp)) {
                                Image(BitmapPainter(a.icon), a.label, Modifier.size(s.iconSize.dp).clip(RoundedCornerShape(s.iconSize.dp * .26f)))
                                Text(a.label, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.Center) {
                repeat(2) { i -> Box(Modifier.padding(3.dp).size(7.dp).clip(CircleShape).background(Color.White.copy(if (pager.currentPage == i) 1f else .35f))) }
            }
            Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 10.dp), contentAlignment = Alignment.Center) {
                Dock(s, listOf("browser", "notes", "playstore", "ai", "settings", "desk").mapNotNull { appById(it) }) { n.open(it) }
            }
        }
        Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 30.dp, end = 16.dp).size(40.dp).glass(20.dp)
            .clickable { n.screen = "lock" }, contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Lock, "Lock", tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}
