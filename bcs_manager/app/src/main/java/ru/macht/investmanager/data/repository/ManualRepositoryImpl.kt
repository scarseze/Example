package ru.macht.investmanager.data.repository

import ru.macht.investmanager.api.MoexApiService
import ru.macht.investmanager.domain.model.BrokerType
import ru.macht.investmanager.domain.model.PortfolioAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.macht.investmanager.domain.repository.ManualRepository
import javax.inject.Inject

class ManualRepositoryImpl @Inject constructor(
    private val moexApi: MoexApiService,
    private val dao: ru.macht.investmanager.data.db.ManualPositionDao
) : ManualRepository {
    
    val brokerName: String = "Manual (MOEX)"
    
    override suspend fun getPortfolio(): List<PortfolioAsset> {
        return withContext(Dispatchers.IO) {
            val dbPositions = dao.getAll()
            if (dbPositions.isEmpty()) return@withContext emptyList()

            val tickers = dbPositions.joinToString(",") { it.ticker }
            
            // Fetch Prices for ALL tickers in one go
            val apiData: Pair<Map<String, Double>, Map<String, String>> = try {
                val response = moexApi.getSharesMarketData(tickers)
                val marketData = response.marketData
                val securities = response.securities
                    
                val priceMap = mutableMapOf<String, Double>()
                val nameMap = mutableMapOf<String, String>()
                    
                // 1. Extract Prices from MarketData
                if (marketData != null) {
                    val secIdIndex = marketData.columns.indexOf("SECID")
                    val lastIndex = marketData.columns.indexOf("LAST")
                    val lastToIndex = marketData.columns.indexOf("LCURRENTPRICE")
                        
                    marketData.data.forEach { row ->
                        val ticker = row.getOrNull(secIdIndex) as? String
                        val price = row.getOrNull(lastIndex)?.toString()?.toDoubleOrNull() 
                                    ?: row.getOrNull(lastToIndex)?.toString()?.toDoubleOrNull()
                            
                        if (ticker != null && price != null) {
                            priceMap[ticker] = price
                        }
                    }
                }

                // 2. Extract Names from Securities
                if (securities != null) {
                    val secIdIndex = securities.columns.indexOf("SECID")
                    val shortNameIndex = securities.columns.indexOf("SHORTNAME")
                        
                    securities.data.forEach { row ->
                        val ticker = row.getOrNull(secIdIndex) as? String
                        val name = row.getOrNull(shortNameIndex) as? String
                            
                        if (ticker != null && name != null) {
                            nameMap[ticker] = name
                        }
                    }
                }

                Pair(priceMap as Map<String, Double>, nameMap as Map<String, String>)
            } catch (e: Exception) {
                e.printStackTrace()
                Pair(emptyMap<String, Double>(), emptyMap<String, String>())
            }
            
            val (currentPrices, currentNames) = apiData

            dbPositions.map { pos ->
                PortfolioAsset(
                    ticker = pos.ticker,
                    name = currentNames[pos.ticker] ?: pos.name, // Use API name or fallback to DB
                    quantity = pos.quantity,
                    averagePrice = pos.averagePrice,
                    currentPrice = currentPrices[pos.ticker] ?: pos.averagePrice, // Fallback to avg
                    broker = BrokerType.ALFA, // Reusing Enum for now
                    type = "Ручные"
                )
            }
        }
    }

    override suspend fun addPosition(ticker: String, quantity: Double, price: Double) {
        withContext(Dispatchers.IO) {
            dao.insert(
                ru.macht.investmanager.data.db.ManualPositionEntity(
                    ticker = ticker, 
                    name = ticker, // ToDo: Fetch name from API
                    quantity = quantity, 
                    averagePrice = price
                )
            )
        }
    }
    
    override suspend fun deletePosition(ticker: String) {
        withContext(Dispatchers.IO) {
            dao.deleteByTicker(ticker)
        }
    }
}