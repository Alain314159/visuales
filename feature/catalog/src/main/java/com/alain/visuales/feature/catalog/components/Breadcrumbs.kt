package com.alain.visuales.feature.catalog.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** A single segment of the breadcrumb trail. */
data class Crumb(val label: String, val path: String)

/**
 * Horizontal breadcrumb bar used at the top of BrowseScreen.
 *
 * Each segment is a tappable TextButton; segments are separated by a
 * chevron-right icon. The last segment is highlighted as the current folder
 * and disabled (you're already there) when there are multiple crumbs.
 */
@Composable
fun Breadcrumbs(
    crumbs: List<Crumb>,
    onCrumbClick: (Crumb) -> Unit,
    modifier: Modifier = Modifier
) {
    if (crumbs.isEmpty()) return
    LazyRow(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(crumbs) { index, c ->
            val isLast = index == crumbs.lastIndex
            TextButton(
                onClick = { onCrumbClick(c) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                enabled = !(isLast && crumbs.size > 1)
            ) {
                Text(
                    text = c.label,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isLast && crumbs.size > 1)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.primary
                )
            }
            if (!isLast) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(horizontal = 0.dp)
                        .size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
