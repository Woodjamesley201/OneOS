package com.oneos.launcher

import android.content.Context
import androidx.compose.runtime.*
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

val Context.ds by preferencesDataStore("oneos")

data class Reminder(val text: String, val done: Boolean)

class OsState(private val ctx: Context, private val scope: CoroutineScope) {
    var loaded by mutableStateOf(false)
    var setupDone by mutableStateOf(false)
    var userName by mutableStateOf("")
    var language by mutableStateOf("English")
    var darkMode by mutableStateOf(true)
    var accent by mutableStateOf(0xFF0A84FF.toInt())
    var blur by mutableStateOf(24f)
    var iconSize by mutableStateOf(60f)
    var dockStyle by mutableStateOf("Glass")
    var clockStyle by mutableStateOf("Classic")
    var wallpaper by mutableStateOf(0)
    var apiKey by mutableStateOf("")
    var apiEndpoint by mutableStateOf("https://api.openai.com/v1/chat/completions")
    var apiModel by mutableStateOf("gpt-4o-mini")
    var notes by mutableStateOf(listOf<String>())
    var reminders by mutableStateOf(listOf<Reminder>())

    fun load() = scope.launch {
        val p = ctx.ds.data.first()
        setupDone = p[booleanPreferencesKey("setup")] ?: false
        userName = p[stringPreferencesKey("name")] ?: ""
        language = p[stringPreferencesKey("lang")] ?: "English"
        darkMode = p[booleanPreferencesKey("dark")] ?: true
        accent = p[intPreferencesKey("accent")] ?: accent
        blur = p[floatPreferencesKey("blur")] ?: 24f
        iconSize = p[floatPreferencesKey("icon")] ?: 60f
        dockStyle = p[stringPreferencesKey("dock")] ?: "Glass"
        clockStyle = p[stringPreferencesKey("clock")] ?: "Classic"
        wallpaper = p[intPreferencesKey("wall")] ?: 0
        apiKey = p[stringPreferencesKey("apikey")] ?: ""
        apiEndpoint = p[stringPreferencesKey("apiep")] ?: apiEndpoint
        apiModel = p[stringPreferencesKey("apimodel")] ?: apiModel
        notes = (p[stringPreferencesKey("notes")] ?: "[]").let { s -> JSONArray(s).let { a -> List(a.length()) { a.getString(it) } } }
        reminders = (p[stringPreferencesKey("rem")] ?: "[]").let { s ->
            JSONArray(s).let { a -> List(a.length()) { val o = a.getJSONObject(it); Reminder(o.getString("t"), o.getBoolean("d")) } }
        }
        loaded = true
    }

    fun save() = scope.launch {
        ctx.ds.edit { p ->
            p[booleanPreferencesKey("setup")] = setupDone
            p[stringPreferencesKey("name")] = userName
            p[stringPreferencesKey("lang")] = language
            p[booleanPreferencesKey("dark")] = darkMode
            p[intPreferencesKey("accent")] = accent
            p[floatPreferencesKey("blur")] = blur
            p[floatPreferencesKey("icon")] = iconSize
            p[stringPreferencesKey("dock")] = dockStyle
            p[stringPreferencesKey("clock")] = clockStyle
            p[intPreferencesKey("wall")] = wallpaper
            p[stringPreferencesKey("apikey")] = apiKey
            p[stringPreferencesKey("apiep")] = apiEndpoint
            p[stringPreferencesKey("apimodel")] = apiModel
            p[stringPreferencesKey("notes")] = JSONArray(notes).toString()
            p[stringPreferencesKey("rem")] = JSONArray().also { a ->
                reminders.forEach { a.put(JSONObject().put("t", it.text).put("d", it.done)) }
            }.toString()
        }
    }
}
