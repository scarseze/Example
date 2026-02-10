package ru.macht.investmanager.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.macht.investmanager.api.BcsApiService
import ru.macht.investmanager.api.MdsApiService
import ru.macht.investmanager.api.MoexApiService
import ru.macht.investmanager.api.RssNewsService
import ru.macht.investmanager.api.DeepSeekApiService
import ru.macht.investmanager.api.AuthInterceptor
import ru.macht.investmanager.data.SettingsManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        // Базовый URL для BCS (заглушка, замените на реальный при наличии)
        return Retrofit.Builder()
            .baseUrl("https://api.bcs.ru/") 
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideBcsApi(authInterceptor: AuthInterceptor): BcsApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging) // Логируем запросы к BCS
            .addInterceptor(authInterceptor) // Используем наш умный интерцептор
            .build()

        return Retrofit.Builder()
            .baseUrl("https://be.broker.ru/trade-api-bff-portfolio/api/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BcsApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMdsApi(retrofit: Retrofit): MdsApiService {
        // Если MDS на другом хосте, создайте отдельный Retrofit instance
        return retrofit.create(MdsApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMoexApi(): MoexApiService {
        return Retrofit.Builder()
            .baseUrl("https://iss.moex.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MoexApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRssNewsService(authInterceptor: AuthInterceptor): RssNewsService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC // Для RSS достаточно BASIC
        }
        // RSS часто блокируют ботов, добавляем User-Agent
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(authInterceptor) // Используем AuthInterceptor для добавления User-Agent
            .build()

        return Retrofit.Builder()
            .baseUrl("https://dummy.url/") // Base URL ignored for @Url
            .client(client)
            .addConverterFactory(retrofit2.converter.simplexml.SimpleXmlConverterFactory.create())
            .build()
            .create(RssNewsService::class.java)
    }

    @Provides
    @Singleton
    fun provideDeepSeekApi(): DeepSeekApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.deepseek.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeepSeekApiService::class.java)
    }
}
