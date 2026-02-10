package ru.macht.investmanager.api

import retrofit2.http.GET
import retrofit2.http.Query

interface MoexApiService {
    // ISS MOEX JSON API для Акций (ищем по всему рынку shares)
    @GET("iss/engines/stock/markets/shares/securities.json")
    suspend fun getSharesMarketData(
        @Query("securities") tickers: String,
        @Query("iss.meta") meta: String = "off",
        @Query("iss.only") only: String = "marketdata,securities"
    ): MoexResponse

    // ISS MOEX JSON API для Облигаций (ищем по всему рынку bonds)
    @GET("iss/engines/stock/markets/bonds/securities.json")
    suspend fun getBondsMarketData(
        @Query("securities") tickers: String,
        @Query("iss.meta") meta: String = "off",
        @Query("iss.only") only: String = "marketdata,securities"
    ): MoexResponse

    // Кривая бескупонной доходности (ZCYC)
    @GET("iss/engines/stock/zcyc.json")
    suspend fun getYieldCurve(
        @Query("iss.meta") meta: String = "off"
    ): MoexZcycResponse

    // Валютный рынок (инструменты)
    @GET("iss/engines/currency/markets/selt/securities.json")
    suspend fun getCurrencyMarketData(
        @Query("securities") tickers: String,
        @Query("iss.meta") meta: String = "off",
        @Query("iss.only") only: String = "marketdata,securities"
    ): MoexResponse
}

data class MoexZcycResponse(
    @com.google.gson.annotations.SerializedName("yearyields")
    val yearYields: MoexBlock?
)

data class MoexResponse(
    @com.google.gson.annotations.SerializedName("marketdata")
    val marketData: MoexBlock?,
    val securities: MoexBlock?
)

data class MoexBlock(
    val columns: List<String>,
    val data: List<List<Any?>>
)
