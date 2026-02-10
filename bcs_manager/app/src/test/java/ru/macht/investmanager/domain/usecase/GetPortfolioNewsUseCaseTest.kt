package ru.macht.investmanager.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.macht.investmanager.data.NewsItem
import ru.macht.investmanager.data.repository.BrokerRepository
import ru.macht.investmanager.data.repository.ManualRepository
import ru.macht.investmanager.data.repository.NewsRepository

class GetPortfolioNewsUseCaseTest {

    // 1. Mock dependencies
    private val bcsRepository = mockk<BrokerRepository>()
    private val manualRepository = mockk<ManualRepository>()
    private val newsRepository = mockk<NewsRepository>()

    // 2. Class under test
    private val useCase = GetPortfolioNewsUseCase(bcsRepository, manualRepository, newsRepository)

    @Test
    fun `invoke should merge tickers, remove duplicates and request news`() = runTest {
        // GIVEN
        val bcsTickers = listOf(
            ru.macht.investmanager.data.InvestmentAsset("AAPL", "Apple", 1.0, 100.0, 100.0, ru.macht.investmanager.data.BrokerType.BCS),
            ru.macht.investmanager.data.InvestmentAsset("TSLA", "Tesla", 1.0, 100.0, 100.0, ru.macht.investmanager.data.BrokerType.BCS)
        )
        val manualTickers = listOf(
             ru.macht.investmanager.data.InvestmentAsset("TSLA", "Tesla", 1.0, 100.0, 100.0, ru.macht.investmanager.data.BrokerType.ALFA),
             ru.macht.investmanager.data.InvestmentAsset("GOOGL", "Google", 1.0, 100.0, 100.0, ru.macht.investmanager.data.BrokerType.ALFA)
        )
        val expectedQuery = listOf("AAPL", "TSLA", "GOOGL")
        val expectedNews = listOf(mockk<NewsItem>())

        // Setup Mocks
        coEvery { bcsRepository.getPortfolio() } returns bcsTickers
        coEvery { manualRepository.getPortfolio() } returns manualTickers
        coEvery { newsRepository.getNews(expectedQuery) } returns expectedNews

        // WHEN
        val result = useCase()

        // THEN
        assertEquals(expectedNews, result.news)
        assertEquals(0, result.errors.size)

        // Verify correct call
        coVerify(exactly = 1) { newsRepository.getNews(match { it.containsAll(expectedQuery) && it.size == 3 }) }
    }
    
    @Test
    fun `invoke should handle empty tickers gracefully`() = runTest {
        // GIVEN
        coEvery { bcsRepository.getPortfolio() } returns emptyList()
        coEvery { manualRepository.getPortfolio() } returns emptyList()

        // WHEN
        val result = useCase()

        // THEN
        assertEquals(emptyList<NewsItem>(), result.news)
        
        // Verify news API is NOT called
        coVerify(exactly = 0) { newsRepository.getNews(any()) }
    }
    
    @Test
    fun `invoke should handle partial failure (BCS fails)`() = runTest {
        // GIVEN
        val manualTickers = listOf(
             ru.macht.investmanager.data.InvestmentAsset("GOOGL", "Google", 1.0, 100.0, 100.0, ru.macht.investmanager.data.BrokerType.ALFA)
        )
        val expectedQuery = listOf("GOOGL")
        val expectedNews = listOf(mockk<NewsItem>())

        // Setup Mocks
        coEvery { bcsRepository.getPortfolio() } throws RuntimeException("Network Error")
        coEvery { manualRepository.getPortfolio() } returns manualTickers
        coEvery { newsRepository.getNews(expectedQuery) } returns expectedNews

        // WHEN
        val result = useCase()

        // THEN - Should succeed with partial data
        assertEquals(expectedNews, result.news)
        assertEquals(1, result.errors.size)
        // Verify we captured the correct error
        assert(result.errors.first().contains("Network Error"))
        coVerify(exactly = 1) { newsRepository.getNews(expectedQuery) }
    }
}
