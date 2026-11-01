package com.oneos.launcher

import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AppContent(id: String, s: OsState, n: Nav, onClose: () -> Unit) {
    val title = appById(id)?.name ?: id
    when (id) {
        "settings" -> SettingsApp(s, n, onClose)
        "browser" -> BrowserApp(s, n, onClose)
        else -> AppScaffold(title, onClose) {
            when (id) {
                "clock" -> ClockApp(s)
                "calculator" -> CalculatorApp()
                "notes" -> NotesApp(s)
                "reminders" -> RemindersApp(s)
                "calendar" -> CalendarApp(s)
                "files" -> FilesApp()
                "photos" -> PhotosApp(s)
                "weather" -> WeatherApp()
                "music" -> MusicApp(s)
                else -> Text("Unknown app", color = Color.White)
            }
        }
    }
}

@Composable
fun Tabs(options: List<String>, sel: Int, onSel: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().glass(20.dp).padding(4.dp)) {
        options.forEachIndexed { i, o ->
            Box(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (i == sel) Color.White.copy(.2f) else Color.Transparent)
                .clickable { onSel(i) }.padding(10.dp), contentAlignment = Alignment.Center) { Text(o, color = Color.White) }
        }
    }
}

fun fmt(ms: Long): String { val t = ms / 10; return "%02d:%02d.%02d".format(t / 6000, (t / 100) % 60, t % 100) }

@Composable
fun ClockApp(s: OsState) {
    var tab by remember { mutableStateOf(0) }
    Tabs(listOf("World Clock", "Stopwatch", "Timer"), tab) { tab = it }
    Spacer(Modifier.height(20.dp))
    when (tab) {
        0 -> { val now = rememberNow()
            listOf("America/New_York", "Europe/London", "Asia/Seoul", "Asia/Tokyo", "America/Los_Angeles").forEach { z ->
                val f = java.text.SimpleDateFormat("h:mm:ss a", Locale.getDefault()).apply { timeZone = java.util.TimeZone.getTimeZone(z) }
                GlassCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row { Text(z.substringAfter('/').replace('_', ' '), color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f))
                        Text(f.format(now), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Light) } } } }
        1 -> { var running by remember { mutableStateOf(false) }; var elapsed by remember { mutableStateOf(0L) }
            val laps = remember { mutableStateListOf<Long>() }
            LaunchedEffect(running) { var last = System.currentTimeMillis(); while (running) { delay(16); val t = System.currentTimeMillis(); elapsed += t - last; last = t } }
            Text(fmt(elapsed), color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Thin, modifier = Modifier.align(Alignment.CenterHorizontally))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                GlassButton(if (running) "Lap" else "Reset", s) { if (running) laps.add(0, elapsed) else { elapsed = 0; laps.clear() } }
                GlassButton(if (running) "Stop" else "Start", s) { running = !running } }
            LazyColumn { items(laps.size) { i -> Text("Lap ${laps.size - i}   ${fmt(laps[i])}", color = Color.White, modifier = Modifier.padding(8.dp)) } } }
        else -> { var total by remember { mutableStateOf(300) }; var left by remember { mutableStateOf(300) }; var running by remember { mutableStateOf(false) }
            LaunchedEffect(running) { while (running && left > 0) { delay(1000); left-- }; if (left == 0) running = false }
            Text("%02d:%02d".format(left / 60, left % 60), color = if (left == 0) Color(0xFFFF453A) else Color.White, fontSize = 72.sp, fontWeight = FontWeight.Thin,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf(1, 5, 10, 30).forEach { m -> GlassButton("${m}m", s) { total = m * 60; left = total; running = false } } }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                GlassButton("Reset", s) { left = total; running = false }
                GlassButton(if (running) "Pause" else "Start", s) { running = !running } } }
    }
}

@Composable
fun CalculatorApp() {
    var display by remember { mutableStateOf("0") }
    var acc by remember { mutableStateOf<Double?>(null) }
    var op by remember { mutableStateOf<String?>(null) }
    var fresh by remember { mutableStateOf(true) }
    fun show(d: Double) = if (d.isNaN() || d.isInfinite()) "Error" else if (d == Math.floor(d) && Math.abs(d) < 1e15) d.toLong().toString() else d.toString().take(14)
    fun apply(a: Double, b: Double, o: String?) = when (o) { "+" -> a + b; "−" -> a - b; "×" -> a * b; "÷" -> a / b; else -> b }
    fun press(k: String) {
        val cur = display.toDoubleOrNull() ?: 0.0
        when (k) {
            "AC" -> { display = "0"; acc = null; op = null; fresh = true }
            "±" -> display = show(-cur)
            "%" -> display = show(cur / 100)
            "+", "−", "×", "÷" -> { acc = if (acc != null && !fresh) apply(acc!!, cur, op) else cur; display = show(acc!!); op = k; fresh = true }
            "=" -> { if (acc != null) { display = show(apply(acc!!, cur, op)); acc = null; op = null; fresh = true } }
            "." -> { if (fresh) { display = "0."; fresh = false } else if (!display.contains('.')) display += "." }
            else -> { display = if (fresh || display == "0") k else display + k; fresh = false }
        }
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
        Text(display, color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Light, maxLines = 1, modifier = Modifier.align(Alignment.End).padding(12.dp))
        listOf(listOf("AC", "±", "%", "÷"), listOf("7", "8", "9", "×"), listOf("4", "5", "6", "−"), listOf("1", "2", "3", "+"), listOf("0", ".", "=")).forEach { row ->
            Row(Modifier.fillMaxWidth()) { row.forEach { k ->
                val c = when { k in listOf("÷", "×", "−", "+", "=") -> Color(0xFFFF9F0A); k in listOf("AC", "±", "%") -> Color(0xFFA5A5A5); else -> Color(0xFF333333) }
                Box(Modifier.weight(if (k == "0") 2f else 1f).padding(6.dp).height(72.dp).clip(RoundedCornerShape(36.dp)).background(c.copy(.85f))
                    .clickable { press(k) }, contentAlignment = Alignment.Center) { Text(k, color = Color.White, fontSize = 28.sp) } } }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun NotesApp(s: OsState) {
    var editing by remember { mutableStateOf<Int?>(null) }
    var text by remember { mutableStateOf("") }
    val e = editing
    if (e != null) {
        Row { Text("Done", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable {
            s.notes = s.notes.toMutableList().also { if (e < it.size) it[e] = text else it.add(text) }.filter { it.isNotBlank() }; s.save(); editing = null }.padding(8.dp))
            Spacer(Modifier.weight(1f))
            if (e < s.notes.size) Text("Delete", color = Color(0xFFFF453A), modifier = Modifier.clickable {
                s.notes = s.notes.toMutableList().also { it.removeAt(e) }; s.save(); editing = null }.padding(8.dp)) }
        TextField(text, { text = it }, Modifier.fillMaxSize(), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
            placeholder = { Text("Start typing…") })
    } else {
        GlassButton("+ New Note", s) { text = ""; editing = s.notes.size }
        Spacer(Modifier.height(10.dp))
        if (s.notes.isEmpty()) Text("No notes yet", color = Color.White.copy(.6f))
        LazyColumn { items(s.notes.size) { i -> GlassCard(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { text = s.notes[i]; editing = i }) {
            Text(s.notes[i].lineSequence().first().take(60), color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(s.notes[i].lineSequence().drop(1).joinToString(" ").take(80), color = Color.White.copy(.6f), maxLines = 1) } } }
    }
}

@Composable
fun RemindersApp(s: OsState) {
    var input by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(input, { input = it }, Modifier.weight(1f), singleLine = true, placeholder = { Text("New reminder") })
        Spacer(Modifier.width(8.dp))
        GlassButton("Add", s) { if (input.isNotBlank()) { s.reminders = s.reminders + Reminder(input, false); s.save(); input = "" } }
    }
    Spacer(Modifier.height(10.dp))
    LazyColumn { items(s.reminders.size) { i -> val r = s.reminders[i]
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).glass(16.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (r.done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null, tint = Color(s.accent), modifier = Modifier.clickable {
                s.reminders = s.reminders.toMutableList().also { it[i] = r.copy(done = !r.done) }; s.save() })
            Text(r.text, color = Color.White.copy(if (r.done) .4f else 1f), modifier = Modifier.weight(1f).padding(start = 10.dp))
            Icon(Icons.Filled.Delete, null, tint = Color.White.copy(.5f), modifier = Modifier.clickable { s.reminders = s.reminders.filterIndexed { j, _ -> j != i }; s.save() })
        } } }
}

@Composable
fun CalendarApp(s: OsState) {
    var ym by remember { mutableStateOf(YearMonth.now()) }
    var sel by remember { mutableStateOf(LocalDate.now()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("‹", color = Color.White, fontSize = 30.sp, modifier = Modifier.clickable { ym = ym.minusMonths(1) }.padding(10.dp))
        Text("${ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${ym.year}", color = Color.White, fontSize = 24.sp, modifier = Modifier.weight(1f))
        Text("›", color = Color.White, fontSize = 30.sp, modifier = Modifier.clickable { ym = ym.plusMonths(1) }.padding(10.dp))
    }
    val offset = ym.atDay(1).dayOfWeek.value % 7
    val cells = List(offset) { null } + (1..ym.lengthOfMonth()).map { ym.atDay(it) }
    Row { listOf("S", "M", "T", "W", "T", "F", "S").forEach { Text(it, color = Color.White.copy(.5f), modifier = Modifier.weight(1f).padding(6.dp)) } }
    LazyVerticalGrid(GridCells.Fixed(7), Modifier.heightIn(max = 360.dp)) { items(cells) { d ->
        Box(Modifier.aspectRatio(1f).padding(3.dp).clip(CircleShape).background(when { d == sel -> Color(s.accent); d == LocalDate.now() -> Color.White.copy(.2f); else -> Color.Transparent })
            .clickable(enabled = d != null) { d?.let { sel = it } }, contentAlignment = Alignment.Center) { if (d != null) Text("${d.dayOfMonth}", color = Color.White) } } }
    GlassCard(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Text(sel.toString(), color = Color.White, fontWeight = FontWeight.SemiBold)
        val r = s.reminders.filter { !it.done }
        Text(if (r.isEmpty()) "No events" else "Open reminders: " + r.joinToString { it.text }, color = Color.White.copy(.7f))
    }
}

@Composable
fun FilesApp() {
    val ctx = LocalContext.current
    val root = ctx.getExternalFilesDir(null) ?: ctx.filesDir
    var dir by remember { mutableStateOf(root) }
    var refresh by remember { mutableStateOf(0) }
    var viewing by remember { mutableStateOf<File?>(null) }
    val files = remember(dir, refresh) { dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList() }
    Text(dir.absolutePath.removePrefix(root.parent ?: ""), color = Color.White.copy(.6f), fontSize = 12.sp)
    Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (dir != root) GlassButton("Up") { dir = dir.parentFile ?: root }
        GlassButton("New Folder") { File(dir, "Folder ${files.size + 1}").mkdirs(); refresh++ }
        GlassButton("New Text") { File(dir, "Note ${files.size + 1}.txt").writeText("Created by OneOS Files"); refresh++ }
    }
    viewing?.let { f -> GlassCard(Modifier.fillMaxWidth()) {
        Text(f.name, color = Color.White, fontWeight = FontWeight.Bold)
        Text(runCatching { f.readText().take(2000) }.getOrDefault("(binary)"), color = Color.White.copy(.8f))
        Row { Text("Close", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { viewing = null }.padding(8.dp))
            Text("Delete", color = Color(0xFFFF453A), modifier = Modifier.clickable { f.delete(); viewing = null; refresh++ }.padding(8.dp)) } } }
    LazyColumn { items(files) { f ->
        Row(Modifier.fillMaxWidth().clickable { if (f.isDirectory) dir = f else viewing = f }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (f.isDirectory) Icons.Filled.Folder else Icons.Filled.Description, null, tint = Color(0xFF0A84FF))
            Text(f.name, color = Color.White, modifier = Modifier.weight(1f).padding(start = 10.dp))
            Text(if (f.isDirectory) "" else "${f.length()} B", color = Color.White.copy(.5f), fontSize = 12.sp)
        } } }
}

@Composable
fun PhotosApp(s: OsState) {
    val photos = remember { mutableStateListOf<Uri>() }
    var big by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) { photos.addAll(0, it) }
    GlassButton("Choose Photos", s) { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }
    Spacer(Modifier.height(10.dp))
    big?.let { AsyncImage(it, null, Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(20.dp)).clickable { big = null }, contentScale = ContentScale.Fit) }
    LazyVerticalGrid(GridCells.Adaptive(110.dp)) { items(photos) { u ->
        AsyncImage(u, null, Modifier.aspectRatio(1f).padding(2.dp).clip(RoundedCornerShape(8.dp)).clickable { big = u }, contentScale = ContentScale.Crop) } }
}

data class Weather(val city: String, val temp: Double, val desc: String, val wind: Double, val daily: List<Triple<String, Double, Double>>)

fun wmo(c: Int) = when (c) { 0 -> "Clear"; 1, 2 -> "Partly Cloudy"; 3 -> "Cloudy"; 45, 48 -> "Fog"; in 51..67 -> "Rain"; in 71..77 -> "Snow"; in 80..82 -> "Showers"; in 95..99 -> "Thunderstorm"; else -> "—" }

suspend fun fetchWeather(lat: Double = 40.71, lon: Double = -74.01, city: String = "New York"): Weather? = withContext(Dispatchers.IO) {
    runCatching {
        val j = JSONObject(URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true&daily=temperature_2m_max,temperature_2m_min&temperature_unit=fahrenheit&timezone=auto").readText())
        val c = j.getJSONObject("current_weather"); val d = j.getJSONObject("daily")
        val t = d.getJSONArray("time"); val mx = d.getJSONArray("temperature_2m_max"); val mn = d.getJSONArray("temperature_2m_min")
        Weather(city, c.getDouble("temperature"), wmo(c.getInt("weathercode")), c.getDouble("windspeed"), List(t.length()) { Triple(t.getString(it), mx.getDouble(it), mn.getDouble(it)) })
    }.getOrNull()
}

@Composable
fun rememberWeather(): Weather? { var w by remember { mutableStateOf<Weather?>(null) }; LaunchedEffect(Unit) { w = fetchWeather() }; return w }

@Composable
fun WeatherApp() {
    val cities = listOf(Triple("New York", 40.71, -74.01), Triple("Miami", 25.76, -80.19), Triple("Port-au-Prince", 18.59, -72.31), Triple("Seoul", 37.57, 126.98), Triple("London", 51.51, -0.13))
    var sel by remember { mutableStateOf(0) }
    var w by remember { mutableStateOf<Weather?>(null) }
    LaunchedEffect(sel) { w = null; val c = cities[sel]; w = fetchWeather(c.second, c.third, c.first) }
    Row(Modifier.horizontalScroll(rememberScrollState())) { cities.forEachIndexed { i, c -> Choice2(c.first, i == sel) { sel = i } } }
    val x = w
    if (x == null) CircularProgressIndicator(Modifier.padding(30.dp)) else Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(x.city, color = Color.White, fontSize = 32.sp)
        Text("${x.temp.toInt()}°", color = Color.White, fontSize = 96.sp, fontWeight = FontWeight.Thin)
        Text("${x.desc} · Wind ${x.wind} km/h", color = Color.White.copy(.8f))
        GlassCard(Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("7-DAY FORECAST", color = Color.White.copy(.6f), fontSize = 12.sp)
            x.daily.forEach { (d, mx, mn) -> Row(Modifier.padding(vertical = 6.dp)) {
                Text(LocalDate.parse(d).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), color = Color.White, modifier = Modifier.weight(1f))
                Text("${mn.toInt()}° / ${mx.toInt()}°", color = Color.White) } } }
    }
}

@Composable
fun Choice2(t: String, sel: Boolean, onClick: () -> Unit) = Box(Modifier.padding(4.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(if (sel) .25f else .08f))
    .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp)) { Text(t, color = Color.White) }

@Composable
fun MusicApp(s: OsState) {
    val ctx = LocalContext.current
    val tracks = remember { mutableStateListOf<Uri>() }
    var idx by remember { mutableStateOf(-1) }
    var playing by remember { mutableStateOf(false) }
    val player = remember { arrayOfNulls<MediaPlayer>(1) }
    DisposableEffect(Unit) { onDispose { player[0]?.release() } }
    fun name(u: Uri) = u.lastPathSegment?.substringAfterLast('/') ?: "Track"
    fun play(i: Int) {
        if (i !in tracks.indices) return
        player[0]?.release()
        player[0] = runCatching { MediaPlayer.create(ctx, tracks[i]) }.getOrNull()?.apply { setOnCompletionListener { if (idx + 1 < tracks.size) play(idx + 1) else playing = false }; start() }
        idx = i; playing = player[0] != null
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { tracks.addAll(it); if (idx < 0 && it.isNotEmpty()) play(0) }
    GlassButton("Add Music Files", s) { pick.launch(arrayOf("audio/*")) }
    GlassCard(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Box(Modifier.size(140.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFFFF2D55).copy(.6f)).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.MusicNote, null, tint = Color.White, modifier = Modifier.size(64.dp)) }
        Text(if (idx >= 0) name(tracks[idx]) else "Not Playing", color = Color.White, fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(10.dp), maxLines = 1)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(44.dp).clickable { play(idx - 1) })
            Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(54.dp).clickable {
                player[0]?.let { if (it.isPlaying) { it.pause(); playing = false } else { it.start(); playing = true } } ?: play(0) })
            Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(44.dp).clickable { play(idx + 1) })
        }
    }
    LazyColumn { items(tracks.size) { i -> Text(name(tracks[i]), color = if (i == idx) Color(s.accent) else Color.White, modifier = Modifier.fillMaxWidth().clickable { play(i) }.padding(10.dp)) } }
}
