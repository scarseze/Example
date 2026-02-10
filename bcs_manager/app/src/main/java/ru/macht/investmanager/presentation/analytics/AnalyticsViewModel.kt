package ru.macht.investmanager.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.macht.investmanager.domain.model.BondAnalysis
import ru.macht.investmanager.domain.model.YieldCurve
import ru.macht.investmanager.domain.usecase.GetYieldCurveAnalysisUseCase
import javax.inject.Inject

sealed interface AnalyticsUiState {
    data object Loading : AnalyticsUiState
    data class Success(
        val yieldCurve: YieldCurve,
        val analyzedBonds: List<BondAnalysis>
    ) : AnalyticsUiState
    data class Error(val message: String) : AnalyticsUiState
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getYieldCurveAnalysisUseCase: GetYieldCurveAnalysisUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = AnalyticsUiState.Loading
            try {
                val (curve, analysis) = getYieldCurveAnalysisUseCase()
                _uiState.value = AnalyticsUiState.Success(curve, analysis)
            } catch (e: Exception) {
                _uiState.value = AnalyticsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
