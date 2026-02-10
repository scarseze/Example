package ru.macht.investmanager.domain.model

data class PortfolioNewsResult(
    val news: List<NewsArticle>,
    val errors: List<String>
)