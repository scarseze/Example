package ru.macht.investmanager.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.macht.investmanager.api.MoexApiService
import ru.macht.investmanager.domain.model.YieldCurve
import ru.macht.investmanager.domain.model.YieldPoint
import ru.macht.investmanager.domain.repository.AnalyticsRepository
import javax.inject.Inject

class AnalyticsRepositoryImpl @Inject constructor(
    private val api: MoexApiService
) : AnalyticsRepository {

    override suspend fun getYieldCurve(): YieldCurve = withContext(Dispatchers.IO) {
        try {
            val response = api.getYieldCurve()
            val block = response.yearYields ?: throw Exception("No 'yearyields' block in MOEX response")
            
            // Map column names to indices
            val dateIdx = block.columns.indexOf("tradedate")
            val periodIdx = block.columns.indexOf("period")
            val valIdx = block.columns.indexOf("value")

            if (periodIdx == -1 || valIdx == -1) {
                throw Exception("Required columns not found in MOEX response")
            }

            val points = mutableListOf<YieldPoint>()
            var latestDate = ""

            if (block.data.isNotEmpty()) {
                val lastRow = block.data.last()
                if (dateIdx != -1) {
                    latestDate = lastRow[dateIdx].toString()
                }

                block.data.forEach { row ->
                    // Just take the last date available
                    val rowDate = if (dateIdx != -1) row[dateIdx].toString() else ""
                    if (rowDate == latestDate) {
                        try {
                            val period = (row[periodIdx] as? Number)?.toDouble() ?: 0.0
                            val value = (row[valIdx] as? Number)?.toDouble() ?: 0.0
                            points.add(YieldPoint(period, value))
                        } catch (e: Exception) {
                            // Skip bad rows
                        }
                    }
                }
            }

            YieldCurve(
                date = latestDate,
                points = points.sortedBy { it.maturityYears }
            )

        } catch (e: Exception) {
            android.util.Log.e("AnalyticsRepo", "Error parsing ZCYC", e)
            throw e
        }
    }
}
