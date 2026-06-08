package com.ketotracker.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "keto_prefs")

class PrefsStore(context: Context) {

    private val ds: DataStore<Preferences> = context.dataStore

    val theme: Flow<String> = ds.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> prefs[THEME_KEY] ?: "midnight" }

    suspend fun setTheme(id: String) {
        ds.edit { prefs -> prefs[THEME_KEY] = id }
    }

    /**
     * Auto-theme preferences — native counterpart to the web app's
     * `kt_theme_auto`/`kt_theme_dark_auto`/`kt_theme_light_auto` (CLAUDE.md
     * "Theme System"). When enabled, the active theme tracks the system's
     * dark/light setting via [com.ketotracker.ui.theme.resolveAutoTheme]
     * instead of the single manually-picked [theme].
     */
    val autoThemeEnabled: Flow<Boolean> = ds.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> prefs[AUTO_ENABLED_KEY] ?: false }

    val darkAutoTheme: Flow<String> = ds.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> prefs[DARK_AUTO_KEY] ?: "midnight" }

    val lightAutoTheme: Flow<String> = ds.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> prefs[LIGHT_AUTO_KEY] ?: "pearl" }

    suspend fun setAutoThemeEnabled(enabled: Boolean) {
        ds.edit { prefs -> prefs[AUTO_ENABLED_KEY] = enabled }
    }

    suspend fun setDarkAutoTheme(id: String) {
        ds.edit { prefs -> prefs[DARK_AUTO_KEY] = id }
    }

    suspend fun setLightAutoTheme(id: String) {
        ds.edit { prefs -> prefs[LIGHT_AUTO_KEY] = id }
    }

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme")
        private val AUTO_ENABLED_KEY = booleanPreferencesKey("theme_auto")
        private val DARK_AUTO_KEY = stringPreferencesKey("theme_dark_auto")
        private val LIGHT_AUTO_KEY = stringPreferencesKey("theme_light_auto")
    }
}

