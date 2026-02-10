package ru.macht.investmanager.api

import retrofit2.http.GET
import retrofit2.http.Query

interface MdsApiService {
    @GET("candles")
    suspend fun getCandles(
        @Query("ticker") ticker: String,
        @Query("class_code") classCode: String,
        @Query("from") startDate: String,
        @Query("to") endDate: String
    ): MdsCandlesResponse
}

data class MdsCandlesResponse(
    val bars: List<MdsBar>
)

data class MdsBar(
    val close: Double
)
