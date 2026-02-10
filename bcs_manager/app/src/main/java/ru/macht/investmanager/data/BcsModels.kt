package ru.macht.investmanager.data

import com.google.gson.annotations.SerializedName

data class BcsPortfolioResponse(
    @SerializedName("depoLimit")
    val data: List<BcsPosition>
)

data class BcsPosition(
    @SerializedName("ticker")
    val secId: String,
    
    @SerializedName("instrumentType")
    val instrumentType: String?,
    
    @SerializedName("classCode")
    val classCode: String?,

    @SerializedName("quantity")
    val quantity: Double,
    
    @SerializedName("balancePrice")
    val balancePrice: Double?, // Цена покупки (средняя)
    
    @SerializedName("currentPrice")
    val currentPrice: Double?, // Текущая цена от брокера
    
    @SerializedName("displayName")
    val name: String? = null
)

data class QuantityWrapper(
    @SerializedName("value")
    val value: Double
)

data class BcsCandlesResponse(
    val ticker: String,
    val classCode: String,
    val bars: List<BcsBar> = emptyList()
)

data class BcsBar(
    val close: Double
)
