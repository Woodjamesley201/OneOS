package com.oneos.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    private var nav: Nav? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ctx = LocalContext.current
            val scope = rememberCoroutineScope()
            val s = remember { OsState(ctx.applicationContext, scope).also { it.load() } }
            val n = remember { Nav(ctx).also { nav = it } }
            OneTheme(s) { if (s.loaded) Root(s, n) }
        }
    }
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // Home button pressed while OneOS is foreground -> go home
        nav?.let { if (it.screen != "lock" && it.screen != "setup") it.home() }
    }
}

@Composable
fun Root(s: OsState, n: Nav) {
    LaunchedEffect(Unit) { if (!s.setupDone) n.screen = "setup" }
    BackHandler(enabled = n.screen != "home" || n.aiOpen) {
        if (n.aiOpen) n.aiOpen = false else if (n.screen.startsWith("app:") || n.screen == "desk") n.home()
    }
    Box(Modifier.fillMaxSize()) {
        Crossfade(n.screen, label = "nav") { sc ->
            when {
                sc == "setup" -> SetupScreen(s) { n.screen = "home" }
                sc == "lock" -> LockScreen(s) { n.home() }
                sc == "home" -> HomeScreen(s, n)
                sc == "desk" -> DeskScreen(s, n)
                sc.startsWith("app:") -> Box(Modifier.fillMaxSize()) {
                    Wallpaper(s, s.blur)
                    AppContent(sc.removePrefix("app:"), s, n) { n.home() }
                }
            }
        }
        if (n.aiOpen) AiOverlay(s, n)
    }
}
