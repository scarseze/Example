package ru.macht.investmanager.data

data class NewsItem(
    val title: String,
    val description: String,
    val link: String,
    val pubDate: String,
    val relatedTickers: List<String> = emptyList(),
    val aiAnalysis: AiAnalysis? = null
)