package ru.macht.investmanager.domain.usecase

import ru.macht.investmanager.domain.repository.ManualRepository
import javax.inject.Inject

class DeleteManualAssetUseCase @Inject constructor(
    private val repository: ManualRepository
) {
    suspend operator fun invoke(ticker: String) {
        repository.deletePosition(ticker)
    }
}