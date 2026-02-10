package ru.macht.investmanager.data

import java.time.LocalDate

data class BondInfo(
    val ticker: String,
    val maturityDate: LocalDate,
    val nominal: Double = 1000.0,
    val couponRate: Double = 0.0 // Simplified: ignoring coupons for zero-coupon proxy
)

object BondRegistry {
    // Hardcoded registry of popular OFZ bonds
    private val registry = mapOf(
        // Short term
        "SU26234RMFS3" to LocalDate.of(2025, 7, 16),
        "SU26229RMFS3" to LocalDate.of(2025, 11, 12),
        "SU26226RMFS8" to LocalDate.of(2026, 10, 7),
        
        // Medium term
        "SU26207RMFS9" to LocalDate.of(2027, 2, 3),
        "SU26232RMFS7" to LocalDate.of(2027, 10, 6),
        "SU26212RMFS9" to LocalDate.of(2028, 1, 19),
        "SU26242RMFS6" to LocalDate.of(2029, 8, 29),
        
        // Long term
        "SU26230RMFS1" to LocalDate.of(2039, 3, 16),
        "SU26238RMFS4" to LocalDate.of(2041, 5, 15),

        // New additions (Spring 2026 update)
        "SU26243RMFS4" to LocalDate.of(2038, 5, 19),
        "SU26245RMFS9" to LocalDate.of(2035, 9, 26),
        "SU26246RMFS7" to LocalDate.of(2036, 3, 12),
        "SU26247RMFS5" to LocalDate.of(2039, 5, 10),
        "SU26248RMFS3" to LocalDate.of(2040, 5, 16)
    )

    fun getBondInfo(ticker: String): BondInfo? {
        val date = registry[ticker] ?: registry[ticker.uppercase()] 
        return if (date != null) {
            BondInfo(ticker, date)
        } else {
            null
        }
    }
}
