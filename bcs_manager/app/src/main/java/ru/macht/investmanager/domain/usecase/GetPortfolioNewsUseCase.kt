package ru.macht.investmanager.domain.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import ru.macht.investmanager.domain.repository.BcsRepository
import ru.macht.investmanager.domain.repository.ManualRepository
import ru.macht.investmanager.domain.repository.NewsRepository
import javax.inject.Inject

class GetPortfolioNewsUseCase @Inject constructor(
    private val bcsRepository: BcsRepository,
    private val manualRepository: ManualRepository,
    private val newsRepository: NewsRepository
) {
    suspend operator fun invoke(): ru.macht.investmanager.domain.model.PortfolioNewsResult = kotlinx.coroutines.coroutineScope {
        // Parallel execution
        val bcsDeferred = async { 
            kotlin.runCatching { bcsRepository.getPortfolio().map { it.ticker } } 
        }
        val manualDeferred = async { 
            kotlin.runCatching { manualRepository.getPortfolio().map { it.ticker } } 
        }

        val bcsResult = bcsDeferred.await()
        val manualResult = manualDeferred.await()

        val errors = mutableListOf<String>()
        val tickers = mutableListOf<String>()

        bcsResult.onSuccess { list -> tickers.addAll(list) }
            .onFailure { e -> errors.add("BCS: ${e.message}") }

        manualResult.onSuccess { list -> tickers.addAll(list) }
            .onFailure { e -> errors.add("Manual/MOEX: ${e.message}") }

        // 3. Aggregate and De-duplicate
        val uniqueTickers = tickers.distinct()

        // 4. Fetch News using only the list of tickers
        // Note: NewsRepository itself might fail partially, but we treat it as single unit for now
        // Ideally NewsRepository could also return Result, but let's stick to catching its full failure in ViewModel usually.
        // However, if we return empty news list because no tickers found due to errors, we should know.
        
        val news = if (uniqueTickers.isNotEmpty()) {
             newsRepository.getNews(uniqueTickers)
        } else {
             emptyList()
        }

        ru.macht.investmanager.domain.model.PortfolioNewsResult(news, errors)
    }
}
