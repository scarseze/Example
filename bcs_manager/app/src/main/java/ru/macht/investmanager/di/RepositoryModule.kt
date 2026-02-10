package ru.macht.investmanager.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.macht.investmanager.data.repository.AnalyticsRepositoryImpl
import ru.macht.investmanager.data.repository.BcsRepositoryImpl
import ru.macht.investmanager.data.repository.ManualRepositoryImpl
import ru.macht.investmanager.data.repository.NewsRepositoryImpl
import ru.macht.investmanager.domain.repository.AnalyticsRepository
import ru.macht.investmanager.domain.repository.BcsRepository
import ru.macht.investmanager.domain.repository.ManualRepository
import ru.macht.investmanager.domain.repository.NewsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBcsRepository(
        bcsRepositoryImpl: BcsRepositoryImpl
    ): BcsRepository

    @Binds
    @Singleton
    abstract fun bindManualRepository(
        manualRepositoryImpl: ManualRepositoryImpl
    ): ManualRepository

    @Binds
    @Singleton
    abstract fun bindNewsRepository(
        newsRepositoryImpl: NewsRepositoryImpl
    ): NewsRepository

    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(
        analyticsRepositoryImpl: AnalyticsRepositoryImpl
    ): AnalyticsRepository
}
