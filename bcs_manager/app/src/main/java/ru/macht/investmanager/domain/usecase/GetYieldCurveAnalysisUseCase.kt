package ru.macht.investmanager.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.macht.investmanager.data.BondRegistry
import ru.macht.investmanager.domain.model.BondAnalysis
import ru.macht.investmanager.domain.model.YieldCurve
import ru.macht.investmanager.domain.model.YieldPoint
import ru.macht.investmanager.domain.repository.AnalyticsRepository
import ru.macht.investmanager.domain.repository.BcsRepository
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.pow

class GetYieldCurveAnalysisUseCase @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val bcsRepository: BcsRepository
) {
    suspend operator fun invoke(): Pair<YieldCurve, List<BondAnalysis>> = withContext(Dispatchers.Default) {
        // 1. Fetch Curve (IO)
        val curve = analyticsRepository.getYieldCurve()
        
        // 2. Fetch User Portfolio (IO)
        // We only care about Bonds here
        val portfolio = try {
            bcsRepository.getPortfolio().filter { it.type == "Облигации" || it.type == "BOND" }
        } catch (e: Exception) {
            emptyList()
        }

        // 3. Analyze (CPU)
        val analysis = analyzePortfolio(portfolio, curve)
        
        Pair(curve, analysis)
    }

    private fun analyzePortfolio(
        assets: List<ru.macht.investmanager.domain.model.PortfolioAsset>, 
        curve: YieldCurve
    ): List<BondAnalysis> {
        val analysisList = mutableListOf<BondAnalysis>()
        val today = LocalDate.now()

        for (asset in assets) {
            val bondInfo = BondRegistry.getBondInfo(asset.ticker) ?: continue
            
            // 1. Calculate Years to Maturity
            val daysToMaturity = ChronoUnit.DAYS.between(today, bondInfo.maturityDate)
            if (daysToMaturity <= 0) continue
            val yearsToMaturity = daysToMaturity / 365.25

            // 2. Interpolate Yield
            val yieldPercent = interpolateYield(curve.points, yearsToMaturity)
            
            // 3. Fair Price (Zero-Coupon approx)
            val r = yieldPercent / 100.0
            val fairPrice = bondInfo.nominal / (1.0 + r).pow(yearsToMaturity)
            
            // 4. Upside
            // Assuming currentPrice is in Rubles (absolute)
            val marketPrice = asset.currentPrice
            val upside = if (marketPrice != null && marketPrice > 0) {
                ((fairPrice - marketPrice) / marketPrice) * 100.0
            } else 0.0

            analysisList.add(
                BondAnalysis(
                    ticker = asset.ticker,
                    marketPrice = marketPrice,
                    fairPrice = fairPrice,
                    upsidePercent = upside,
                    maturityDate = bondInfo.maturityDate.toString(),
                    yearsToMaturity = yearsToMaturity,
                    interpolatedYield = yieldPercent
                )
            )
        }
        return analysisList
    }

    private fun interpolateYield(points: List<YieldPoint>, targetYear: Double): Double {
        if (points.isEmpty()) return 0.0
        if (targetYear <= points.first().maturityYears) return points.first().yieldPercent
        if (targetYear >= points.last().maturityYears) return points.last().yieldPercent

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            if (targetYear >= p1.maturityYears && targetYear <= p2.maturityYears) {
                val fraction = (targetYear - p1.maturityYears) / (p2.maturityYears - p1.maturityYears)
                return p1.yieldPercent + fraction * (p2.yieldPercent - p1.yieldPercent)
            }
        }
        return points.last().yieldPercent
    }
}
