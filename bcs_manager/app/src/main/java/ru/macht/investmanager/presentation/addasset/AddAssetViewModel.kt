package ru.macht.investmanager.presentation.addasset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.macht.investmanager.domain.usecase.AddManualAssetUseCase
import javax.inject.Inject

@HiltViewModel
class AddAssetViewModel @Inject constructor(
    private val addManualAssetUseCase: AddManualAssetUseCase
) : ViewModel() {

    fun saveAsset(ticker: String, quantityStr: String, priceStr: String, onSaved: () -> Unit) {
        val quantity = quantityStr.toDoubleOrNull()
        val price = priceStr.toDoubleOrNull()

        if (ticker.isNotBlank() && quantity != null && price != null) {
            viewModelScope.launch {
                addManualAssetUseCase(ticker.uppercase(), quantity, price)
                onSaved()
            }
        }
    }
}
