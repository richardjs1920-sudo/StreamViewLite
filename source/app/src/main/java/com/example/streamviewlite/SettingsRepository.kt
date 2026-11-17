package com.example.streamviewlite

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val rtmpUrl: String = "",
    val streamKey: String = ""
)

class SettingsRepository(private val context: Context) {

    private val urlKey = stringPreferencesKey("rtmp_url")
    private val keyKey = stringPreferencesKey("stream_key")

    val settings: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            rtmpUrl = preferences[urlKey] ?: "",
            streamKey = preferences[keyKey] ?: ""
        )
    }

    suspend fun saveSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[urlKey] = settings.rtmpUrl
            preferences[keyKey] = settings.streamKey
        }
    }
}
