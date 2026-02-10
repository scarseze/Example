package ru.macht.investmanager.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.macht.investmanager.data.db.AppDatabase
import ru.macht.investmanager.data.db.ManualPositionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "invest_manager.db"
        ).build()
    }

    @Provides
    fun provideManualPositionDao(db: AppDatabase): ManualPositionDao {
        return db.manualPositionDao()
    }
}
