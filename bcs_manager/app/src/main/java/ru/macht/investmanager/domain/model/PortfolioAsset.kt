package ru.macht.investmanager.domain.model

data class PortfolioAsset(
    val ticker: String,
    val name: String,
    val quantity: Double,
    val averagePrice: Double,
    val currentPrice: Double? = null,
    val broker: BrokerType,
    val type: String = "Акция" // По умолчанию
)
