package ru.macht.investmanager.presentation.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.macht.investmanager.domain.model.PortfolioResult
import ru.macht.investmanager.domain.usecase.DeleteManualAssetUseCase
import ru.macht.investmanager.domain.usecase.GetCombinedPortfolioUseCase
import javax.inject.Inject

sealed interface PortfolioUiState {
    data object Loading : PortfolioUiState
    data class Success(val result: PortfolioResult) : PortfolioUiState
    data class Error(val message: String) : PortfolioUiState
}

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val getCombinedPortfolioUseCase: GetCombinedPortfolioUseCase,
    private val deleteManualAssetUseCase: DeleteManualAssetUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<PortfolioUiState>(PortfolioUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadPortfolio()
    }

    fun loadPortfolio() {
        viewModelScope.launch {
            _uiState.value = PortfolioUiState.Loading
            try {
                val result = getCombinedPortfolioUseCase()
                _uiState.value = PortfolioUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = PortfolioUiState.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    fun deleteAsset(ticker: String) {
        viewModelScope.launch {
            deleteManualAssetUseCase(ticker)
            loadPortfolio() // Обновляем список после удаления
        }
    }
}
