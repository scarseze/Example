package ru.macht.investmanager.data

import com.google.gson.annotations.SerializedName

// MOEX ISS Layout: { "marketdata": { "columns": ["SECID", "LAST"], "data": [ ["SBER", 250.5] ] } }

data class MoexResponse(
    @SerializedName("marketdata") val marketData: MoexBlock?,
    @SerializedName("securities") val securities: MoexBlock?
)

data class MoexBlock(
    @SerializedName("columns") val columns: List<String>,
    @SerializedName("data") val data: List<List<Any>>
)
