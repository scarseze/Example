package ru.macht.investmanager.domain.repository

import ru.macht.investmanager.domain.model.PortfolioAsset

interface BcsRepository {
    suspend fun getPortfolio(): List<PortfolioAsset>
}
