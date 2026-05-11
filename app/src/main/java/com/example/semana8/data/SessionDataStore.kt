package com.example.semana8.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val SESSION_PREFS = "session_prefs"
private val Context.dataStore by preferencesDataStore(name = SESSION_PREFS)

class SessionDataStore(private val context: Context) {

    private companion object {
        val KEY_LOGGED_IN = booleanPreferencesKey("logged_in")
    }

    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { prefs: Preferences ->
        prefs[KEY_LOGGED_IN] ?: false
    }

    suspend fun setLoggedIn(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LOGGED_IN] = value
        }
    }
}

