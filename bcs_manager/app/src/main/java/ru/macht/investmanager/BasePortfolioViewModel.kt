package ru.macht.investmanager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.macht.investmanager.domain.model.PortfolioAsset
import ru.macht.investmanager.data.repository.BrokerRepository

sealed interface PortfolioUiState {
    data class Success(val positions: List<PortfolioAsset>, val total: Double) : PortfolioUiState
    data class Error(val message: String) : PortfolioUiState
    object Loading : PortfolioUiState
}

abstract class BasePortfolioViewModel(
    private val repository: BrokerRepository
) : ViewModel() {

    var uiState: PortfolioUiState by mutableStateOf(PortfolioUiState.Loading)
        private set

    init {
        // Option to trigger fetch on init or manually
        // fetchPortfolio() 
        // Better to trigger lazily or in UI onresume
    }

    fun fetchPortfolio() {
        viewModelScope.launch {
            uiState = PortfolioUiState.Loading
            try {
                val positions = repository.getPortfolio()
                val totalValue = positions.sumOf { it.quantity * (it.currentPrice ?: it.averagePrice) }
                uiState = PortfolioUiState.Success(positions, totalValue)
            } catch (e: Exception) {
                uiState = PortfolioUiState.Error("Ошибка: ${e.message}")
            }
        }
    }

    open fun addPosition(ticker: String, quantity: Double, price: Double) {
        // No-op by default
    }

    open fun deletePosition(ticker: String) {
        // No-op by default
    }
}
