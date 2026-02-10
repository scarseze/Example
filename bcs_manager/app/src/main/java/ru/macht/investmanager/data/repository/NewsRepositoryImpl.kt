package ru.macht.investmanager.data.repository

import ru.macht.investmanager.api.DeepSeekApiService
import ru.macht.investmanager.api.DeepSeekRequest
import ru.macht.investmanager.api.Message
import ru.macht.investmanager.api.RssNewsService
import ru.macht.investmanager.data.AiAnalysis
import ru.macht.investmanager.data.NewsItem
import ru.macht.investmanager.data.Sentiment
import ru.macht.investmanager.data.SettingsManager
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import ru.macht.investmanager.domain.model.NewsArticle
import ru.macht.investmanager.domain.repository.NewsRepository
import javax.inject.Inject

class NewsRepositoryImpl @Inject constructor(
    private val rssService: RssNewsService,
    private val deepSeekService: DeepSeekApiService,
    private val settingsManager: SettingsManager
) : NewsRepository {

    private val rssFeeds = listOf(
        "https://rssexport.rbc.ru/rbcnews/news/20/full.rss",    // РБК (Главное)
        "https://bcs-express.ru/feed",                          // Актуальная лента BCS
        "https://www.vedomosti.ru/rss/rubric/finance",          // Ведомости (Финансы)
        "https://www.vedomosti.ru/rss/rubric/economics",        // Ведомости (Экономика)
        "https://www.kommersant.ru/RSS/section-finance.xml",    // Коммерсант (Финансы)
        "https://www.finam.ru/analysis/conews/rsspoint/"        // Лента Finam (может быть нестабильна)
    )

    private val newsCache = mutableMapOf<String, NewsItem>()
    
    override suspend fun getNews(tickers: List<String>): List<NewsArticle> {
        val portfolioTickers = tickers
        
        val allNews = mutableListOf<NewsItem>()
        
        // Получаем новости из всех RSS источников
        for (feedUrl in rssFeeds) {
            try {
                val feed = rssService.getRssFeed(feedUrl)
                val items = feed.channel?.items ?: emptyList()
                
                items.forEach { rssItem ->
                    val newsItem = NewsItem(
                        title = rssItem.title ?: "",
                        description = rssItem.description ?: "",
                        link = rssItem.link ?: "",
                        pubDate = rssItem.pubDate ?: "",
                        relatedTickers = extractTickers(rssItem.title + " " + rssItem.description, portfolioTickers)
                    )
                    allNews.add(newsItem)
                }
            } catch (e: Exception) {
                // Логируем ошибку, но продолжаем с другими источниками
                android.util.Log.e("NewsRepository", "Error fetching RSS from $feedUrl", e)
            }
        }
        
        // Фильтруем: исключаем криптовалюты
        val noCryptoNews = allNews.filter { news ->
            val text = (news.title + " " + news.description).lowercase()
            val excludeKeywords = listOf(
                "bitcoin", "биткоин", "ethereum", "эфир", "crypto", "крипто", 
                "metamask", "nft", "блокчейн", "криптовалют"
            )
            !excludeKeywords.any { text.contains(it) }
        }
        
        // Фильтруем по тикерам из портфеля (если портфель не пустой)
        val filteredNews = if (portfolioTickers.isNotEmpty()) {
            android.util.Log.d("NewsRepository", "=== FILTERING BY TICKERS ===")
            android.util.Log.d("NewsRepository", "Looking for tickers: $portfolioTickers")
            
            // Показываем примеры заголовков для отладки
            noCryptoNews.take(5).forEach { news ->
                android.util.Log.d("NewsRepository", "Sample title: ${news.title}")
            }
            
            // Маппинг тикеров на названия компаний для лучшего поиска
            // Расширенный список синонимов
            val tickerToNames = mapOf(
                "SBER" to listOf("сбер", "сбербанк"),
                "GAZP" to listOf("газпром"),
                "LKOH" to listOf("лукойл"),
                "YNDX" to listOf("яндекс", "yndx"),
                "ROSN" to listOf("роснефт"),
                "GMKN" to listOf("норникель", "норильский никель"),
                "VTBR" to listOf("втб"),
                "MGNT" to listOf("магнит"),
                "FIVE" to listOf("x5", "пятёрочка"),
                "TCSG" to listOf("tcsg", "tcs"), // Старый тикер
                "T" to listOf("т-банк", "тинькофф", "т-технологии", "t-bank"), // Новый тикер
                "TRNFP" to listOf("транснефт"),
                "ALRS" to listOf("алроса"),
                "NLMK" to listOf("нлмк"),
                "CHMF" to listOf("северсталь"),
                "NVTK" to listOf("новатэк"),
                "TATN" to listOf("татнефт"),
                "SNGS" to listOf("сургутнефтегаз"),
                "MTSS" to listOf("мтс"),
                "MOEX" to listOf("мосбирж"),
                "PLZL" to listOf("полюс"),
                "SPBE" to listOf("спб бирж", "spbe"),
                "GLD" to listOf("золото", "gold"),
                "SLV" to listOf("серебро", "silver"),
                
                // Добавлено по портфелю пользователя
                "IRAO" to listOf("интер рао", "inter rao"),
                "RAGR" to listOf("русагро", "rusagro"),
                "SELG" to listOf("селигдар", "seligdar"),
                "SVCB" to listOf("совкомбанк", "sovcombank"),
                "X5" to listOf("x5", "five", "пятерочка", "перекресток"),
                "AFKS" to listOf("система", "афк"),
                "MAGN" to listOf("ммк", "магнитогорск")
            )
            
            noCryptoNews.filter { news ->
                val text = (news.title + " " + news.description)
                
                // Ищем по тикерам И по названиям компаний
                val found = portfolioTickers.any { ticker ->
                    val tickerUpper = ticker.uppercase()
                    
                    // --- BASIC TICKER MATCHING ---
                    // 1. Поиск по полному тикеру
                    val isShort = tickerUpper.length < 3
                    val tickerRegex = if (isShort) {
                         Regex("(?<=[\\s(]|^)${Regex.escape(tickerUpper)}(?=[\\s.,;)]|\$)")
                    } else {
                        Regex("\\b${Regex.escape(tickerUpper)}\\b", RegexOption.IGNORE_CASE)
                    }
                    var matchFound = tickerRegex.containsMatchIn(text)

                    // --- SMART BOND MATCHING (OFZ) ---
                    // Example: SU26243RMFS4 -> Search for "26243"
                    if (!matchFound && tickerUpper.startsWith("SU") && tickerUpper.contains("RMFS")) {
                        val bondNumber = tickerUpper.removePrefix("SU").substringBefore("RMFS")
                        if (bondNumber.isNotEmpty()) {
                            matchFound = text.contains(bondNumber) || text.contains("ОФЗ $bondNumber") || text.contains("ОФЗ-$bondNumber")
                        }
                    }

                    // --- SMART CURRENCY/COMMODITY MATCHING ---
                    // Example: GLDRUB_TOM -> Search for "GLD", "Gold", "Золото"
                    if (!matchFound && tickerUpper.contains("RUB")) {
                        val shortTicker = tickerUpper.substringBefore("RUB")
                        if (shortTicker.isNotEmpty()) {
                             val shortRegex = Regex("\\b${Regex.escape(shortTicker)}\\b", RegexOption.IGNORE_CASE)
                             matchFound = shortRegex.containsMatchIn(text)
                        }
                    }

                    // --- NAME MATCHING ---
                    if (!matchFound) {
                        // Check explicit names map
                        val names = tickerToNames[tickerUpper] ?: emptyList()
                        // Also check short ticker in map (e.g. GLD from GLDRUB_TOM)
                        val shortTicker = tickerUpper.substringBefore("RUB").substringBefore("_")
                        val shortNames = tickerToNames[shortTicker] ?: emptyList()
                        
                        val allNames = names + shortNames
                        
                        matchFound = allNames.any { name -> 
                            text.lowercase().contains(name.lowercase())
                        }
                    }
                    
                    matchFound
                }
                
                if (found) {
                    android.util.Log.d("NewsRepository", "✓ FOUND: ${news.title} (Match for portfolio)")
                }
                
                found
            }
        } else {
            android.util.Log.d("NewsRepository", "Portfolio is EMPTY - showing first 10 news")
            // Если портфель пустой - показываем первые 10 новостей
            noCryptoNews.take(10)
        }
        
        android.util.Log.d("NewsRepository", "Total: ${allNews.size}, No crypto: ${noCryptoNews.size}, Filtered by tickers: ${filteredNews.size}")
        android.util.Log.d("NewsRepository", "Portfolio tickers: $portfolioTickers")
        
        // Берем первые 10 новостей для AI-анализа (ПАРАЛЛЕЛЬНО)
        val analyzedNews = coroutineScope {
            filteredNews.take(10).map { news ->
                async {
                    if (newsCache.containsKey(news.link)) {
                        newsCache[news.link]!!
                    } else {
                        // Pass tickers to analysis if needed, but currently not used in prompt strictly differently
                        val analyzed = analyzeWithAI(news, portfolioTickers) 
                        newsCache[news.link] = analyzed
                        analyzed
                    }
                }
            }.awaitAll()
        }

        // Map to Domain Model
        return analyzedNews.map { item ->
            NewsArticle(
                title = item.title,
                description = item.description,
                link = item.link,
                pubDate = item.pubDate,
                sentiment = mapSentiment(item.aiAnalysis?.sentiment),
                summary = item.aiAnalysis?.summary ?: "",
                impact = item.aiAnalysis?.impact ?: ""
            )
        }
    }
    
    private fun extractTickers(text: String, tickers: List<String>): List<String> {
        // Простой regex для поиска тикеров (4 заглавные буквы)
        val tickerRegex = Regex("\\b[A-ZА-Я]{4}\\b")
        return tickerRegex.findAll(text)
            .map { it.value }
            .filter { tickers.contains(it) }
            .distinct()
            .toList()
    }
    
    private suspend fun analyzeWithAI(news: NewsItem, tickers: List<String>): NewsItem {
        android.util.Log.d("NewsRepository", "=== Starting AI analysis ===")
        android.util.Log.d("NewsRepository", "News title: ${news.title}")
        try {
            val prompt = """
                Ты - аналитик российского фондового рынка. Проанализируй новость и определи:
                1. Относится ли она к акциям/облигациям из портфеля: ${tickers.joinToString(", ")}
                2. Тональность (позитивная/негативная/нейтральная)
                3. Краткое резюме и влияние на активы
                
                Новость:
                Заголовок: ${news.title}
                Описание: ${news.description}
                
                Ответь СТРОГО в формате JSON:
                {
                  "sentiment": "POSITIVE",
                  "summary": "краткое резюме на русском (1-2 предложения)",
                  "impact": "влияние на активы из портфеля или 'Не относится к портфелю'"
                }
                
                Значение sentiment: POSITIVE, NEGATIVE или NEUTRAL
            """.trimIndent()
            
            val request = DeepSeekRequest(
                messages = listOf(
                    Message(role = "user", content = prompt)
                )
            )
            
            android.util.Log.d("NewsRepository", "Sending request to DeepSeek API...")
            val apiKey = settingsManager.deepSeekKeyFlow.first() ?: ""
            val keyLength = apiKey.length
            val maskedKey = if (keyLength > 8) {
                "${apiKey.take(4)}...${apiKey.takeLast(4)}"
            } else "SHORT_KEY"
            android.util.Log.d("NewsRepository", "API Key (masked): $maskedKey")
            
            // Проверяем, не добавлен ли уже префикс Bearer в конфиг
            val authHeader = if (apiKey.startsWith("Bearer ")) {
                apiKey
            } else {
                "Bearer $apiKey"
            }

            val response = deepSeekService.analyzeNews(
                authorization = authHeader,
                request = request
            )
            
            val aiResponse = response.choices.firstOrNull()?.message?.content ?: ""
            android.util.Log.d("NewsRepository", "AI Response received: $aiResponse")
            val analysis = parseAiResponse(aiResponse)
            android.util.Log.d("NewsRepository", "Analysis parsed successfully: ${analysis.sentiment}")
            
            return news.copy(aiAnalysis = analysis)
        } catch (e: Exception) {
            android.util.Log.e("NewsRepository", "!!! ERROR in AI analysis !!!")
            android.util.Log.e("NewsRepository", "Error message: ${e.message}")
            android.util.Log.e("NewsRepository", "Error type: ${e.javaClass.simpleName}")
            
            val keyLength = (settingsManager.deepSeekKeyFlow.first() ?: "").length
            val maskedKey = if (keyLength > 8) {
                "HIDDEN"
            } else "SHORT_KEY"
            android.util.Log.e("NewsRepository", "API Key used (masked): $maskedKey")
            android.util.Log.e("NewsRepository", "Stack trace:", e)
            return news.copy(
                aiAnalysis = AiAnalysis(
                    sentiment = Sentiment.NEUTRAL,
                    summary = "AI-анализ временно недоступен: ${e.message}",
                    impact = "Не определено"
                )
            )
        }
    }
    
    private fun parseAiResponse(response: String): AiAnalysis {
        // Парсим JSON ответ от AI
        return try {
            // Более надежный поиск JSON объекта: от первой { до последней }
            val startIndex = response.indexOf('{')
            val endIndex = response.lastIndexOf('}')
            
            val jsonString = if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                response.substring(startIndex, endIndex + 1)
            } else {
                // Fallback: просто чистим markdown, если скобки не найдены явно
                response.replace("```json", "").replace("```", "").trim()
            }
            
            Gson().fromJson(jsonString, AiAnalysis::class.java)
        } catch (e: JsonSyntaxException) {
            android.util.Log.e("NewsRepository", "Error parsing AI response: $response", e)
            AiAnalysis(
                sentiment = Sentiment.NEUTRAL,
                summary = "Ошибка парсинга ответа AI",
                impact = "Не определено"
            )
        }
    }

    private fun mapSentiment(sentiment: Sentiment?): ru.macht.investmanager.domain.model.Sentiment {
        return when (sentiment) {
            Sentiment.POSITIVE -> ru.macht.investmanager.domain.model.Sentiment.POSITIVE
            Sentiment.NEGATIVE -> ru.macht.investmanager.domain.model.Sentiment.NEGATIVE
            else -> ru.macht.investmanager.domain.model.Sentiment.NEUTRAL
        }
    }
}
