package com.alain.visuales.feature.catalog.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alain.visuales.core.common.EntryType
import com.alain.visuales.core.common.formatBytes
import com.alain.visuales.core.data.db.entity.DirectoryEntity
import com.alain.visuales.core.data.db.entity.DirectoryEntryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Row used in [com.alain.visuales.feature.catalog.screens.BrowseScreen].
 *
 * Two overloads: one for sub-directories (no download button, chevron right
 * trailing) and one for file entries (icon based on [EntryType], size + date
 * subtitle, optional download button).
 */
@Composable
fun EntryRow(
    directory: DirectoryEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = directory.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (directory.childCount > 0) {
                    Text(
                        text = "${directory.childCount} elementos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun EntryRow(
    entry: DirectoryEntryEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDownloadClick: (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = entryIcon(entry),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = buildString {
                    append(formatBytes(entry.size))
                    entry.modifiedAt?.let {
                        if (it > 0) append(" · ").append(dateFormatter.format(Date(it)))
                    }
                }
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!entry.isDirectory && onDownloadClick != null && isDownloadable(entry)) {
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onDownloadClick) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = "Descargar"
                    )
                }
            }
        }
    }
}

/** Resolve a Material icon by EntryType. */
private fun entryIcon(entry: DirectoryEntryEntity): ImageVector {
    if (entry.isDirectory) return Icons.Filled.Folder
    val type = runCatching { EntryType.valueOf(entry.type) }.getOrNull() ?: EntryType.OTHER
    return when (type) {
        EntryType.DIRECTORY -> Icons.Filled.Folder
        EntryType.VIDEO -> Icons.Filled.Movie
        EntryType.SUBTITLE -> Icons.Filled.Subtitles
        EntryType.IMAGE -> Icons.Filled.Image
        EntryType.ARCHIVE -> Icons.Filled.Archive
        EntryType.AUDIO -> Icons.Filled.MusicNote
        EntryType.DOCUMENT -> Icons.Filled.PictureAsPdf
        EntryType.OTHER -> Icons.Filled.InsertDriveFile
    }
}

/** Only offer a download button for entries that make sense to download. */
private fun isDownloadable(entry: DirectoryEntryEntity): Boolean {
    val type = runCatching { EntryType.valueOf(entry.type) }.getOrNull() ?: return false
    return type in downloadableTypes
}

private val downloadableTypes = setOf(
    EntryType.VIDEO,
    EntryType.AUDIO,
    EntryType.DOCUMENT,
    EntryType.ARCHIVE,
    EntryType.IMAGE,
    EntryType.SUBTITLE,
    EntryType.OTHER
)

private val dateFormatter: SimpleDateFormat by lazy {
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
}
