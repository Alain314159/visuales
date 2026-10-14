package com.alain.visuales.core.network

import com.alain.visuales.core.network.api.VisualesApi
import com.alain.visuales.core.network.parser.ListadoHtmlParser
import com.alain.visuales.core.network.scraper.NginxyListingScraper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Hilt module providing the parsers and scrapers.
 */
@Module
@InstallIn(SingletonComponent::class)
object ParserModule {

    @Provides
    @Singleton
    fun provideListadoHtmlParser(): ListadoHtmlParser = ListadoHtmlParser()

    @Provides
    @Singleton
    fun provideNginxyListingScraper(): NginxyListingScraper = NginxyListingScraper()

    @Provides
    @Singleton
    fun provideVisualesApi(retrofit: Retrofit): VisualesApi =
        retrofit.create(VisualesApi::class.java)
}
