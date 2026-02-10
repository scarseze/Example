package ru.macht.investmanager.domain.repository

import ru.macht.investmanager.domain.model.PortfolioAsset

interface ManualRepository {
    suspend fun getPortfolio(): List<PortfolioAsset>
    suspend fun addPosition(ticker: String, quantity: Double, price: Double)
    suspend fun deletePosition(ticker: String)
}
