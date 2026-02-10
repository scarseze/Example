package ru.macht.investmanager.domain.model

data class YieldPoint(
    val maturityYears: Double,
    val yieldPercent: Double
)

data class YieldCurve(
    val date: String,
    val points: List<YieldPoint>
)

data class BondAnalysis(
    val ticker: String,
    val marketPrice: Double?,
    val fairPrice: Double,
    val upsidePercent: Double,
    val maturityDate: String,
    val yearsToMaturity: Double,
    val interpolatedYield: Double
)
