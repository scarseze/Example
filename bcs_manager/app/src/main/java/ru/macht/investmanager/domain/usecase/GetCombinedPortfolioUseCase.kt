package ru.macht.investmanager.domain.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import ru.macht.investmanager.domain.model.PortfolioAsset
import ru.macht.investmanager.domain.model.PortfolioResult
import ru.macht.investmanager.domain.repository.BcsRepository
import ru.macht.investmanager.domain.repository.ManualRepository
import javax.inject.Inject

class GetCombinedPortfolioUseCase @Inject constructor(
    private val bcsRepository: BcsRepository,
    private val manualRepository: ManualRepository
) {
    /**
     * Возвращает объединенный список активов и общую сумму.
     * Обрабатывает ошибки (Partial Failure) внутри.
     */
    suspend operator fun invoke(): PortfolioResult = coroutineScope {
        // Параллельный запрос данных
        val bcsDeferred = async { 
            runCatching { bcsRepository.getPortfolio() }
        }
        val manualDeferred = async { 
            runCatching { manualRepository.getPortfolio() }
        }

        val bcsResult = bcsDeferred.await()
        val manualResult = manualDeferred.await()

        val bcsAssets = bcsResult.getOrDefault(emptyList())
        val manualAssets = manualResult.getOrDefault(emptyList())
        
        var errorMsg: String? = null
        // Логируем ошибку, если она есть
        bcsResult.onFailure { e -> 
            android.util.Log.e("PortfolioUseCase", "BCS Error: ${e.message}", e) 
            errorMsg = if (e is java.net.UnknownHostException) {
                "Ошибка сети: Не удалось найти сервер (проверьте интернет)"
            } else {
                e.message
            }
        }

        // Бизнес-логика: Объединение и расчеты
        val allAssets = bcsAssets + manualAssets
        val totalValue = allAssets.sumOf { it.quantity * (it.currentPrice ?: it.averagePrice) }
        val totalProfit = allAssets.sumOf { 
            val price = it.currentPrice ?: it.averagePrice
            (price - it.averagePrice) * it.quantity 
        }

        PortfolioResult(
            assets = allAssets,
            totalValue = totalValue,
            totalProfit = totalProfit,
            hasBcsError = bcsResult.isFailure,
            bcsErrorMessage = errorMsg
        )
    }
}
