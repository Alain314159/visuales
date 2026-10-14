package com.alain.visuales.core.network

import com.alain.visuales.core.common.AppConstants
import com.alain.visuales.core.network.download.MultiThreadDownloader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module providing HTTP-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC  // never BODY in production (huge logs)
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", AppConstants.USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Encoding", "gzip")
                .header("Accept-Language", "es-CU,es;q=0.9,en;q=0.8")
                .build()
            chain.proceed(req)
        }
        .addInterceptor(loggingInterceptor)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): retrofit2.Retrofit =
        retrofit2.Retrofit.Builder()
            .baseUrl(AppConstants.DEFAULT_BASE_URL + "/")
            .client(client)
            .build()

    @Provides
    @Singleton
    fun provideMultiThreadDownloader(client: OkHttpClient): MultiThreadDownloader =
        MultiThreadDownloader(client)
}
