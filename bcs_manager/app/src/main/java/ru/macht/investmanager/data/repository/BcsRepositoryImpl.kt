package ru.macht.investmanager.data.repository

import ru.macht.investmanager.api.BcsApiService
import ru.macht.investmanager.api.MdsApiService
import ru.macht.investmanager.api.MoexApiService
import ru.macht.investmanager.domain.model.BrokerType
import ru.macht.investmanager.domain.model.PortfolioAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.macht.investmanager.domain.repository.BcsRepository
import javax.inject.Inject

class BcsRepositoryImpl @Inject constructor(
    private val api: BcsApiService,
    private val mdsApi: MdsApiService,
    private val moexApi: MoexApiService
) : BcsRepository { // Implements Domain Interface

    // Note: domain interface does NOT require brokerName, but we can have it locally if needed
    val brokerName: String = "BCS"

    override suspend fun getPortfolio(): List<PortfolioAsset> {
        return withContext(Dispatchers.IO) {
            val positions = api.getPortfolio()
            android.util.Log.d("BcsRepository", "API returned ${positions.size} positions: $positions")

            // Calc dates for candles
            val now = java.time.Instant.now()
            val oneDayAgo = now.minusSeconds(86400)
            val formatter = java.time.format.DateTimeFormatter.ISO_INSTANT
            val endDate = formatter.format(now)
            val startDate = formatter.format(oneDayAgo)

            val rawAssets = positions.mapNotNull { bcsPos ->
                // Если тикер null, пропускаем эту позицию (например, это просто кэш/валюта без sec_id)
                val secId = bcsPos.secId
                if (secId == null) {
                    android.util.Log.w("BcsRepository", "Skipping position (no sec_id): $bcsPos")
                    return@mapNotNull null
                }

                var currentPrice = bcsPos.currentPrice
                
                // Try to fetch better price if missing
                if ((currentPrice == null || currentPrice == 0.0) && bcsPos.classCode != null) {
                    try {
                        val candles = mdsApi.getCandles(
                            ticker = secId,
                            classCode = bcsPos.classCode,
                            startDate = startDate,
                            endDate = endDate
                        )
                        currentPrice = candles.bars.lastOrNull()?.close
                    } catch (e: Exception) {
                        // ignore MDS errors
                    }
                }

                // Определяем имя: либо из API, либо по тикеру
                val assetName = bcsPos.name ?: ru.macht.investmanager.data.TickerNames.getName(secId, bcsPos.instrumentType ?: "")

                PortfolioAsset(
                    ticker = secId,
                    name = assetName,
                    quantity = bcsPos.quantity,
                    averagePrice = bcsPos.balancePrice ?: 0.0,
                    currentPrice = currentPrice ?: bcsPos.balancePrice ?: 0.0, // Fallback
                    broker = BrokerType.BCS,
                    type = mapInstrumentType(bcsPos.instrumentType)
                )
            }

            // Агрегация дубликатов (схлопываем T0, T1, T2)
            val aggregatedAssets = rawAssets.groupBy { it.ticker }
                .map { (ticker, items) ->
                    // ИСПРАВЛЕНИЕ: Берем MAX, а не SUM.
                    // API возвращает полные дубликаты для разных режимов торгов.
                    val maxQty = items.maxOf { it.quantity }
                    if (maxQty == 0.0) return@map null

                    // Средневзвешенная цена покупки
                    // Берем цену из той позиции, где она есть (обычно она одинаковая)
                    val avgPrice = items.map { it.averagePrice }.firstOrNull { it > 0.0 } ?: 0.0

                    // Текущая цена (берем первую ненулевую или первую попавшуюся)
                    val curPrice = items.mapNotNull { it.currentPrice }.firstOrNull { it > 0 } ?: 0.0
                    val firstItem = items.first()

                    firstItem.copy(
                        quantity = maxQty,
                        averagePrice = avgPrice,
                        currentPrice = curPrice
                    )
                }.filterNotNull()

            // ФОЛЛБЭК ЦЕН ЧЕРЕЗ MOEX
            // Если цены от БКС пришли нулевые, пробуем загрузить их с Мосбиржи
            val assetsWithoutPrice = aggregatedAssets.filter { it.currentPrice == 0.0 }
            
            val finalAssets = if (assetsWithoutPrice.isNotEmpty()) {
                // 1. Акции + Фонды (Stock Market)
                val stocksAndFunds = assetsWithoutPrice
                    .filter { it.type == "Акции" || it.type == "Фонды" }
                    .map { it.ticker }
                
                // 2. Облигации (Bond Market)
                val bonds = assetsWithoutPrice
                    .filter { it.type == "Облигации" }
                    .map { it.ticker }
                
                // 3. Валюта и Металлы (Currency Market)
                val currencies = assetsWithoutPrice
                    .filter { it.type == "Валюта" }
                    .map { it.ticker }
                
                val moexPrices = mutableMapOf<String, Double>()
                
                // --- Fetch Stocks & ETFs ---
                if (stocksAndFunds.isNotEmpty()) {
                    moexPrices.putAll(fetchMoexPrices(stocksAndFunds, MarketType.SHARES))
                }
                
                // --- Fetch Bonds ---
                if (bonds.isNotEmpty()) {
                    android.util.Log.d("BcsRepository", "Fetching MOEX prices for bonds: $bonds")
                    moexPrices.putAll(fetchMoexPrices(bonds, MarketType.BONDS))
                }
                
                // --- Fetch Currencies ---
                if (currencies.isNotEmpty()) {
                    val mappedCurrencies = currencies.map { mapCurrencyTicker(it) }
                    android.util.Log.d("BcsRepository", "Fetching MOEX prices for currencies: $mappedCurrencies")
                    // Map back: MOEX Ticker -> BCS Ticker
                    val currencyPrices = fetchMoexPrices(mappedCurrencies, MarketType.CURRENCY)
                    
                    // Remap back to original tickers for the result map
                    currencies.forEach { originalTicker ->
                        val moexTicker = mapCurrencyTicker(originalTicker)
                        val price = currencyPrices[moexTicker]
                        if (price != null) {
                            moexPrices[originalTicker] = price
                        }
                    }
                }
                
                aggregatedAssets.map { asset ->
                    if (asset.currentPrice == 0.0) {
                        val newPrice = moexPrices[asset.ticker] ?: 0.0
                        asset.copy(currentPrice = newPrice)
                    } else asset
                }
            } else {
                aggregatedAssets
            }

            android.util.Log.d("BcsRepository", "Mapped & Aggregated ${finalAssets.size} assets")
            finalAssets
        }
    }

    private enum class MarketType { SHARES, BONDS, CURRENCY }

    private suspend fun fetchMoexPrices(tickers: List<String>, type: MarketType): Map<String, Double> {
        return try {
            val tickerString = tickers.joinToString(",")
            val response = when (type) {
                MarketType.BONDS -> moexApi.getBondsMarketData(tickerString)
                MarketType.SHARES -> moexApi.getSharesMarketData(tickerString)
                MarketType.CURRENCY -> moexApi.getCurrencyMarketData(tickerString)
            }

            val marketData = response.marketData ?: run {
                android.util.Log.e("BcsRepository", "MarketData is null in response for $type")
                return emptyMap()
            }
            
            // android.util.Log.d("BcsRepository", "MOEX Columns ($type): ${marketData.columns}")

            val priceMap = mutableMapOf<String, Double>()
            // Case-insensitive column lookup
            val secIdIndex = marketData.columns.indexOfFirst { it.equals("SECID", ignoreCase = true) }
            val lastIndex = marketData.columns.indexOfFirst { it.equals("LAST", ignoreCase = true) }
            val lastToIndex = marketData.columns.indexOfFirst { it.equals("LCURRENTPRICE", ignoreCase = true) } // Bond specific

            if (secIdIndex == -1) {
                return emptyMap()
            }

            marketData.data.forEach { row ->
                val ticker = row.getOrNull(secIdIndex) as? String
                // Try LAST, then LCURRENTPRICE
                var priceStr = if (lastIndex != -1) row.getOrNull(lastIndex)?.toString() else null
                if (priceStr == null && lastToIndex != -1) {
                    priceStr = row.getOrNull(lastToIndex)?.toString()
                }

                val price = priceStr?.toDoubleOrNull()
                
                if (ticker != null && price != null) {
                    priceMap[ticker] = price
                }
            }
            android.util.Log.d("BcsRepository", "Parsed ${priceMap.size} prices for $type")
            priceMap
        } catch (e: Exception) {
            android.util.Log.e("BcsRepository", "Failed to fetch MOEX prices", e)
            emptyMap()
        }
    }

    private fun mapCurrencyTicker(bcsTicker: String): String {
        return when (bcsTicker.uppercase()) {
            "GLD" -> "GLDRUB_TOM"
            "SLV" -> "SLVRUB_TOM"
            "USD" -> "USDRUB_TOM"
            "EUR" -> "EURRUB_TOM"
            "CNY" -> "CNYRUB_TOM"
            else -> bcsTicker // Надеемся, что совпадает
        }
    }

    private fun mapInstrumentType(type: String?): String {
        return when (type?.uppercase()) {
            "STOCK", "SHARE", "EQ" -> "Акции"
            "BOND", "BONDS", "OBLIGATION" -> "Облигации"
            "FUT", "FUTURE" -> "Фьючерсы"
            "CURR", "CURRENCY", "FX" -> "Валюта"
            "ETF", "MUTUAL_FUNDS", "MUTUAL_FUND" -> "Фонды"
            else -> "Прочее"
        }
    }
}