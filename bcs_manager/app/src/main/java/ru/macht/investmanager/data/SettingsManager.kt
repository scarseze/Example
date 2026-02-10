package ru.macht.investmanager.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val BCS_KEY = stringPreferencesKey("bcs_api_key")
        val BCS_REFRESH_KEY = stringPreferencesKey("bcs_refresh_token")
        val DEEPSEEK_KEY = stringPreferencesKey("deepseek_api_key")
    }

    val bcsKeyFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[BCS_KEY]
    }

    val bcsRefreshTokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[BCS_REFRESH_KEY]
    }

    val deepSeekKeyFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        val savedKey = preferences[DEEPSEEK_KEY]
        if (savedKey.isNullOrBlank()) {
             ru.macht.investmanager.BuildConfig.DEEPSEEK_API_KEY
        } else {
             savedKey
        }
    }

    suspend fun saveKeys(bcsKey: String, bcsRefreshToken: String, deepSeekKey: String) {
        context.dataStore.edit { preferences ->
            preferences[BCS_KEY] = bcsKey
            preferences[BCS_REFRESH_KEY] = bcsRefreshToken
            preferences[DEEPSEEK_KEY] = deepSeekKey
        }
    }

    suspend fun updateBcsTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[BCS_KEY] = accessToken
            preferences[BCS_REFRESH_KEY] = refreshToken
        }
    }
}