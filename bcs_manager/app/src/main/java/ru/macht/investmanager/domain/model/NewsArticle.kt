package ru.macht.investmanager.domain.model

data class NewsArticle(
    val title: String,
    val description: String,
    val link: String,
    val pubDate: String,
    val sentiment: Sentiment = Sentiment.NEUTRAL,
    val summary: String = "",
    val impact: String = ""
)

enum class Sentiment {
    POSITIVE, NEGATIVE, NEUTRAL
}