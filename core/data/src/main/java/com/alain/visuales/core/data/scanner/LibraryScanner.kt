package com.alain.visuales.core.data.scanner

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.alain.visuales.core.common.AppConstants
import com.alain.visuales.core.data.db.entity.LibraryItemEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scanner for local library files.
 *
 * Reads from:
 *  - MediaStore.Video (the system's index of videos the user can see)
 *  - The configured Visuales download folder (for freshly downloaded files not yet indexed)
 *
 * Files are filtered by:
 *  - Extension (.mp4, .mkv, .avi, .mov, etc. — see EntryType.isVideo)
 *  - Path: must be inside the user's Downloads/Visuales or a SAF folder
 */
@Singleton
class LibraryScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Scan the local filesystem for videos belonging to the Visuales library.
     * Returns a list of LibraryItemEntity ready to be upserted into Room.
     */
    suspend fun scan(): List<LibraryItemEntity> {
        val items = mutableListOf<LibraryItemEntity>()

        // 1. Scan the Visuales Downloads folder.
        items.addAll(scanDownloadFolder())

        // 2. Scan MediaStore (catches files in other places).
        items.addAll(scanMediaStore())

        // De-duplicate by path.
        return items.distinctBy { it.path }
    }

    private fun scanDownloadFolder(): List<LibraryItemEntity> {
        val folder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            AppConstants.DOWNLOAD_FOLDER_NAME
        )
        if (!folder.exists() || !folder.isDirectory) return emptyList()

        return folder.listFiles()?.filter { it.isFile && isVideo(it.name) }?.map { file ->
            val duration = getVideoDuration(file.absolutePath)
            LibraryItemEntity(
                path = file.absolutePath,
                name = file.name,
                size = file.length(),
                durationMs = duration,
                addedAt = file.lastModified()
            )
        } ?: emptyList()
    }

    private fun scanMediaStore(): List<LibraryItemEntity> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATA,  // deprecated in API 29+ but still useful
            MediaStore.Video.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Video.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("%${AppConstants.DOWNLOAD_FOLDER_NAME}%")
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        val items = mutableListOf<LibraryItemEntity>()
        context.contentResolver.query(
            collection, projection, selection, selectionArgs, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
            val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val path = if (dataCol >= 0) cursor.getString(dataCol) else continue
                val name = cursor.getString(nameCol)
                if (path.isNullOrBlank() || name.isNullOrBlank()) continue
                items.add(
                    LibraryItemEntity(
                        path = path,
                        name = name,
                        size = cursor.getLong(sizeCol),
                        durationMs = durationCol.takeIf { it >= 0 }?.let { cursor.getLong(it) },
                        addedAt = cursor.getLong(dateCol) * 1000
                    )
                )
            }
        }
        return items
    }

    private fun isVideo(filename: String): Boolean {
        val ext = filename.substringAfterLast('.', "").lowercase()
        return ext in VIDEO_EXTENSIONS
    }

    private fun getVideoDuration(path: String): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {}
        }
    }

    companion object {
        private val VIDEO_EXTENSIONS = setOf(
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg", "m2ts", "ts", "vob", "3gp"
        )
    }
}
