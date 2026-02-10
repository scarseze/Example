package ru.macht.investmanager.data.repository

import ru.macht.investmanager.domain.model.PortfolioAsset

interface BrokerRepository {
    val brokerName: String
    suspend fun getPortfolio(): List<PortfolioAsset>
}
