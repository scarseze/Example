package ru.macht.investmanager.domain.repository

import ru.macht.investmanager.domain.model.NewsArticle

interface NewsRepository {
    suspend fun getNews(tickers: List<String>): List<NewsArticle>
}