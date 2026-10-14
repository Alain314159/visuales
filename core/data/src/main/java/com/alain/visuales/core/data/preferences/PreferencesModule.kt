package com.alain.visuales.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides the single application-wide Preferences DataStore.
 *
 * The backing file lives at `<filesDir>/datastore/visuales_prefs.preferences_pb`
 * via [Context.dataStoreFile], which is the canonical location recommended by
 * androidx.datastore (available since DataStore 1.0; current version: 1.1.1).
 */
@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.dataStoreFile("visuales_prefs.preferences_pb") }
        )
}
