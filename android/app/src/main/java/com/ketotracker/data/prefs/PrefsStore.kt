package com.ketotracker.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme")
    }
}
