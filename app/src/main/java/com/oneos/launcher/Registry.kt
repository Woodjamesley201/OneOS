package com.oneos.launcher

import android.content.*
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class OneApp(val id: String, val name: String, val icon: ImageVector, val color: Color, val external: ((Context) -> Unit)? = null)

fun safeStart(ctx: Context, vararg intents: Intent) {
    for (i in intents) {
        try { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return } catch (_: Exception) {}
    }
    Toast.makeText(ctx, "No app available for this action", Toast.LENGTH_SHORT).show()
}

fun cat(c: String) = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, c)

val builtInApps = listOf(
    OneApp("settings", "Settings", Icons.Filled.Settings, Color(0xFF8E8E93)),
    OneApp("browser", "One Browser", Icons.Filled.Public, Color(0xFF0A84FF)),
    OneApp("ai", "One AI", Icons.Filled.AutoAwesome, Color(0xFFBF5AF2)),
    OneApp("desk", "OneDesk", Icons.Filled.DesktopWindows, Color(0xFF5E5CE6)),
    OneApp("clock", "Clock", Icons.Filled.Schedule, Color(0xFF1C1C1E)),
    OneApp("calculator", "Calculator", Icons.Filled.Calculate, Color(0xFFFF9F0A)),
    OneApp("notes", "Notes", Icons.Filled.EditNote, Color(0xFFFFD60A)),
    OneApp("reminders", "Reminders", Icons.Filled.Checklist, Color(0xFFFF453A)),
    OneApp("calendar", "Calendar", Icons.Filled.CalendarMonth, Color(0xFFFF375F)),
    OneApp("files", "Files", Icons.Filled.Folder, Color(0xFF0A84FF)),
    OneApp("photos", "Photos", Icons.Filled.PhotoLibrary, Color(0xFFFF9F0A)),
    OneApp("weather", "Weather", Icons.Filled.WbSunny, Color(0xFF64D2FF)),
    OneApp("music", "Music", Icons.Filled.MusicNote, Color(0xFFFF2D55)),
    OneApp("camera", "Camera", Icons.Filled.PhotoCamera, Color(0xFF636366)) {
        safeStart(it, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), Intent(MediaStore.ACTION_IMAGE_CAPTURE))
    },
    OneApp("maps", "Maps", Icons.Filled.Map, Color(0xFF30D158)) {
        safeStart(it, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")), Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps")))
    },
    OneApp("contacts", "Contacts", Icons.Filled.Contacts, Color(0xFF8E8E93)) {
        safeStart(it, Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI), cat(Intent.CATEGORY_APP_CONTACTS))
    },
    OneApp("messages", "Messages", Icons.Filled.Sms, Color(0xFF30D158)) {
        safeStart(it, cat(Intent.CATEGORY_APP_MESSAGING), Intent(Intent.ACTION_VIEW, Uri.parse("sms:")))
    },
    OneApp("mail", "Mail", Icons.Filled.Mail, Color(0xFF0A84FF)) {
        safeStart(it, cat(Intent.CATEGORY_APP_EMAIL), Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")))
    },
    OneApp("facetime", "FaceTime", Icons.Filled.Videocam, Color(0xFF30D158)) {
        safeStart(it, it.packageManager.getLaunchIntentForPackage("com.google.android.apps.tachyon") ?: Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_DIAL))
    },
    OneApp("playstore", "Play Store", Icons.Filled.Shop, Color(0xFF00C853)) {
        safeStart(it, it.packageManager.getLaunchIntentForPackage("com.android.vending") ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=")),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store")))
    },
)

fun appById(id: String) = builtInApps.firstOrNull { it.id == id }

class Nav(private val ctx: Context) {
    var screen by mutableStateOf("lock")
    var aiOpen by mutableStateOf(false)
    var browserUrl by mutableStateOf<String?>(null)
    fun open(id: String) {
        if (id == "ai") { aiOpen = true; return }
        val app = appById(id)
        if (app?.external != null) app.external.invoke(ctx)
        else if (app != null) screen = if (id == "desk") "desk" else "app:$id"
    }
    fun home() { screen = "home" }
}
