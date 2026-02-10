package ru.macht.investmanager.data

data class AlfaPortfolioResponse(
    val positions: List<AlfaPosition>
)

data class AlfaPosition(
    val ticker: String,
    val name: String,
    val quantity: Double,
    val price: Double
)
