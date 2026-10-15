package com.alain.visuales.core.network

import com.alain.visuales.core.common.AppConstants
import com.alain.visuales.core.network.api.EmbyApi
import com.alain.visuales.core.network.download.MultiThreadDownloader
import com.alain.visuales.core.network.subtitles.SubDbApi
import com.alain.visuales.core.network.subtitles.SubdivxScraper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
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

    /**
     * Lenient Json instance for Emby. The Emby 4.9.3.0 server returns a huge
     * object per item with many fields we don't care about; `ignoreUnknownKeys`
     * makes deserialization resilient to additions.
     */
    @Provides
    @Singleton
    fun provideEmbyJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    /**
     * A separate Retrofit instance for the Emby JSON API. Unlike the main
     * retrofit (which only deals with raw HTML strings) this one wires the
     * kotlinx-serialization converter so `@Body` / return-type DTOs are
     * (de)serialized automatically.
     */
    @Provides
    @Singleton
    fun provideEmbyApi(
        client: OkHttpClient,
        json: Json
    ): EmbyApi {
        val contentType = "application/json".toMediaType()
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("https://emby.uclv.cu/emby/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
        return retrofit.create(EmbyApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMultiThreadDownloader(client: OkHttpClient): MultiThreadDownloader =
        MultiThreadDownloader(client)

    @Provides
    @Singleton
    fun provideSubDbApi(client: OkHttpClient): SubDbApi = SubDbApi(client)

    @Provides
    @Singleton
    fun provideSubdivxScraper(client: OkHttpClient): SubdivxScraper = SubdivxScraper(client)
}
