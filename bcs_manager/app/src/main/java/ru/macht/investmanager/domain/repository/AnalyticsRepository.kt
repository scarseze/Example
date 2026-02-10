package ru.macht.investmanager.domain.repository

import ru.macht.investmanager.domain.model.YieldCurve

interface AnalyticsRepository {
    suspend fun getYieldCurve(): YieldCurve
}
