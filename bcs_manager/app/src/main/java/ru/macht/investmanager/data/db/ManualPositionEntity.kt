package ru.macht.investmanager.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "manual_positions")
data class ManualPositionEntity(
    @PrimaryKey val ticker: String,
    val name: String,
    val quantity: Double,
    val averagePrice: Double
)
