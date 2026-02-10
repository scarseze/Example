package ru.macht.investmanager.domain.model

data class PortfolioResult(
    val assets: List<PortfolioAsset>,
    val totalValue: Double,
    val totalProfit: Double,
    val hasBcsError: Boolean,
    val bcsErrorMessage: String? = null
)