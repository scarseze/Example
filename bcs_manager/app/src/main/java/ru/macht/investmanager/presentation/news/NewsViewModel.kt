package ru.macht.investmanager.presentation.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.macht.investmanager.domain.model.NewsArticle
import ru.macht.investmanager.domain.usecase.GetPortfolioNewsUseCase
import javax.inject.Inject

sealed interface NewsUiState {
    data object Loading : NewsUiState
    data class Success(val news: List<NewsArticle>, val errors: List<String>) : NewsUiState
    data class Error(val message: String) : NewsUiState
}

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val getPortfolioNewsUseCase: GetPortfolioNewsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<NewsUiState>(NewsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadNews()
    }

    fun loadNews() {
        viewModelScope.launch {
            _uiState.value = NewsUiState.Loading
            try {
                val result = getPortfolioNewsUseCase()
                _uiState.value = NewsUiState.Success(result.news, result.errors)
            } catch (e: Exception) {
                _uiState.value = NewsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}