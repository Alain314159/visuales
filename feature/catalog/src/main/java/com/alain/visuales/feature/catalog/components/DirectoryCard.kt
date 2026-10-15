package com.alain.visuales.feature.catalog.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alain.visuales.core.data.db.entity.DirectoryEntity

/**
 * Card used in the Home carousels and Search grid.
 *
 * Renders a folder icon + the directory name. Wired to accept an optional
 * `posterUrl` (Coil) — listado.html doesn't include posters today, but the
 * signature is in place so we can plug a `MediaItemDetector` later.
 */
@Composable
fun DirectoryCard(
    directory: DirectoryEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    posterUrl: String? = null
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(140.dp)
            .height(176.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Coil placeholder for future poster support.
            // AsyncImage(model = posterUrl, ...) would go here when posters exist.
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = directory.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (directory.childCount > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${directory.childCount} elementos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
