package ru.macht.investmanager.domain.usecase

import ru.macht.investmanager.domain.repository.ManualRepository
import javax.inject.Inject

class AddManualAssetUseCase @Inject constructor(
    private val repository: ManualRepository
) {
    suspend operator fun invoke(ticker: String, quantity: Double, price: Double) {
        repository.addPosition(ticker, quantity, price)
    }
}