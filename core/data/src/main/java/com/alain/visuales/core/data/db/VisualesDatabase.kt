package com.alain.visuales.core.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import android.content.Context
import com.alain.visuales.core.data.db.dao.DirectoryDao
import com.alain.visuales.core.data.db.dao.DirectoryEntryDao
import com.alain.visuales.core.data.db.dao.DownloadDao
import com.alain.visuales.core.data.db.dao.LibraryDao
import com.alain.visuales.core.data.db.dao.SyncStateDao
import com.alain.visuales.core.data.db.entity.ChunkEntity
import com.alain.visuales.core.data.db.entity.DirectoryEntity
import com.alain.visuales.core.data.db.entity.DirectoryEntryEntity
import com.alain.visuales.core.data.db.entity.DownloadEntity
import com.alain.visuales.core.data.db.entity.DownloadPriority
import com.alain.visuales.core.data.db.entity.DownloadStatus
import com.alain.visuales.core.data.db.entity.LibraryItemEntity
import com.alain.visuales.core.data.db.entity.SyncStateEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    version = 1,
    exportSchema = true,
    entities = [
        DirectoryEntity::class,
        DirectoryEntryEntity::class,
        DownloadEntity::class,
        ChunkEntity::class,
        LibraryItemEntity::class,
        SyncStateEntity::class
    ]
)
@TypeConverters(VisualesConverters::class)
abstract class VisualesDatabase : RoomDatabase() {
    abstract fun directoryDao(): DirectoryDao
    abstract fun directoryEntryDao(): DirectoryEntryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun libraryDao(): LibraryDao
    abstract fun syncStateDao(): SyncStateDao

    companion object {
        const val NAME = "visuales.db"
    }
}

class VisualesConverters {
    @TypeConverter
    fun fromDownloadStatus(s: DownloadStatus): String = s.name

    @TypeConverter
    fun toDownloadStatus(s: String): DownloadStatus = DownloadStatus.valueOf(s)

    @TypeConverter
    fun fromDownloadPriority(p: DownloadPriority): String = p.name

    @TypeConverter
    fun toDownloadPriority(s: String): DownloadPriority = DownloadPriority.valueOf(s)
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VisualesDatabase =
        Room.databaseBuilder(context, VisualesDatabase::class.java, VisualesDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDirectoryDao(db: VisualesDatabase): DirectoryDao = db.directoryDao()

    @Provides
    fun provideDirectoryEntryDao(db: VisualesDatabase): DirectoryEntryDao = db.directoryEntryDao()

    @Provides
    fun provideDownloadDao(db: VisualesDatabase): DownloadDao = db.downloadDao()

    @Provides
    fun provideLibraryDao(db: VisualesDatabase): LibraryDao = db.libraryDao()

    @Provides
    fun provideSyncStateDao(db: VisualesDatabase): SyncStateDao = db.syncStateDao()
}
