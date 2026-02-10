package ru.macht.investmanager.api

import ru.macht.investmanager.data.BcsPosition
import retrofit2.http.GET

interface BcsApiService {
    // Пример эндпоинта. В реальности замените на актуальный путь API BCS
    @GET("portfolio")
    suspend fun getPortfolio(): List<BcsPosition>
}
