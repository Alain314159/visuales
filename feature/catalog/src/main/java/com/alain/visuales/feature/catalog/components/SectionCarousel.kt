package com.alain.visuales.feature.catalog.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alain.visuales.core.data.db.entity.DirectoryEntity

/**
 * A horizontal carousel of [DirectoryCard]s preceded by a section title.
 * Used on the Home screen — one per [com.alain.visuales.core.common.CatalogSections.FEATURED] entry.
 *
 * Renders nothing if [items] is empty so callers can blindly map over the
 * featured sections list.
 */
@Composable
fun SectionCarousel(
    title: String,
    items: List<DirectoryEntity>,
    onItemClick: (DirectoryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { dir ->
                DirectoryCard(directory = dir, onClick = { onItemClick(dir) })
            }
        }
    }
}
