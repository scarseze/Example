package ru.macht.investmanager.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ManualPositionEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun manualPositionDao(): ManualPositionDao
}
