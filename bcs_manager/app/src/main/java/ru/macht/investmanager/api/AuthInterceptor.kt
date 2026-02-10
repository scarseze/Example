package ru.macht.investmanager.api

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import ru.macht.investmanager.data.SettingsManager
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val settingsManager: SettingsManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // 1. Получаем текущий токен
        var accessToken = runBlocking { settingsManager.bcsKeyFlow.first() }?.trim() ?: ""
        if (accessToken.startsWith("Bearer ", ignoreCase = true)) {
            accessToken = accessToken.substring(7).trim()
        }

        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
            .header("Accept", "application/json")

        if (accessToken.isNotEmpty()) {
            requestBuilder.header("Authorization", "Bearer $accessToken")
        }

        val response = chain.proceed(requestBuilder.build())

        // 2. Если 401 Unauthorized, пробуем обновить токен
        if (response.code == 401) {
            android.util.Log.d("AuthInterceptor", "Got 401. Attempting refresh...")
            synchronized(this) {
                // Проверяем, не обновил ли кто-то токен уже (в другом потоке)
                val currentToken = runBlocking { settingsManager.bcsKeyFlow.first() }?.trim() ?: ""
                if (currentToken != accessToken && currentToken.isNotEmpty()) {
                    android.util.Log.d("AuthInterceptor", "Token already refreshed by another thread. Retrying...")
                    response.close() // Закрываем старый ответ только перед повтором
                    // Токен уже обновился, просто повторяем запрос с новым
                    return chain.proceed(originalRequest.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .header("Accept", "application/json")
                        .build())
                }

                // Пробуем рефреш
                val refreshToken = runBlocking { settingsManager.bcsRefreshTokenFlow.first() }?.trim() ?: ""
                android.util.Log.d("AuthInterceptor", "Refresh token available: ${refreshToken.isNotEmpty()}")
                
                if (refreshToken.isNotEmpty()) {
                    val newTokens = refreshTokens(refreshToken)
                    if (newTokens != null) {
                        val (newAccess, newRefresh) = newTokens
                        android.util.Log.d("AuthInterceptor", "Refresh successful. Saving new tokens...")
                        runBlocking {
                            settingsManager.updateBcsTokens(newAccess, newRefresh)
                        }
                        response.close() // Закрываем старый ответ только перед повтором
                        // Повторяем исходный запрос с новым токеном
                        return chain.proceed(originalRequest.newBuilder()
                            .header("Authorization", "Bearer $newAccess")
                            .header("Accept", "application/json")
                            .build())
                    } else {
                        android.util.Log.e("AuthInterceptor", "Refresh failed (newTokens is null)")
                    }
                }
            }
        }
        android.util.Log.d("AuthInterceptor", "Returning original response: ${response.code}")
        return response
    }

    private fun refreshTokens(refreshToken: String): Pair<String, String>? {
        return try {
            // Используем "чистый" клиент для рефреша, чтобы избежать циклов
            val client = OkHttpClient()
            val url = "https://be.broker.ru/trade-api-keycloak/realms/tradeapi/protocol/openid-connect/token"
            
            // Очищаем токен от "Bearer " если есть
            val cleanToken = if (refreshToken.startsWith("Bearer ", ignoreCase = true)) {
                refreshToken.substring(7).trim()
            } else {
                refreshToken
            }

            // Формируем тело запроса x-www-form-urlencoded
            val formBody = "grant_type=refresh_token&client_id=trade-api-read&refresh_token=$cleanToken"
            
            val request = Request.Builder()
                .url(url)
                .post(formBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .addHeader("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                android.util.Log.e("AuthInterceptor", "Refresh failed: Code=${response.code}, Body=$responseBody")
                return null
            }

            val json = JSONObject(responseBody ?: "")
            val newAccess = json.optString("access_token") // Проверить ключи в ответе
            val newRefresh = json.optString("refresh_token")
            
            if (newAccess.isNotEmpty()) Pair(newAccess, newRefresh.ifEmpty { refreshToken }) else null
        } catch (e: Exception) {
            android.util.Log.e("AuthInterceptor", "Refresh error", e)
            null
        }
    }
}