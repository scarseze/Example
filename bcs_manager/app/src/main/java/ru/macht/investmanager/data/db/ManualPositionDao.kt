package ru.macht.investmanager.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ManualPositionDao {
    @Query("SELECT * FROM manual_positions")
    suspend fun getAll(): List<ManualPositionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(position: ManualPositionEntity)

    @Query("DELETE FROM manual_positions WHERE ticker = :ticker")
    suspend fun deleteByTicker(ticker: String)
}
