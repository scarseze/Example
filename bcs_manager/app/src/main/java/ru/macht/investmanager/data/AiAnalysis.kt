package ru.macht.investmanager.data

data class AiAnalysis(
    val sentiment: Sentiment,
    val summary: String,
    val impact: String
)