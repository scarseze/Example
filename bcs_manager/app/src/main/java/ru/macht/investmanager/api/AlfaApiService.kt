package ru.macht.investmanager.api

import ru.macht.investmanager.data.AlfaPortfolioResponse
import retrofit2.http.GET
import retrofit2.http.Header

interface AlfaApiService {
    @GET("v1/portfolio") // Placeholder endpoint
    suspend fun getPortfolio(
        @Header("Authorization") token: String
    ): AlfaPortfolioResponse
}
