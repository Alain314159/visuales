package com.alain.visuales.core.common

import java.text.DecimalFormat

/**
 * Format a byte count into a human-readable string (e.g. "1.4 GB").
 * Returns "—" if the size is 0 or negative.
 */
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "—"
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    val tb = gb * 1024
    val df = DecimalFormat("0.##")
    return when {
        bytes >= tb -> df.format(bytes / tb) + " TB"
        bytes >= gb -> df.format(bytes / gb) + " GB"
        bytes >= mb -> df.format(bytes / mb) + " MB"
        bytes >= kb -> df.format(bytes / kb) + " KB"
        else -> "$bytes B"
    }
}

/**
 * Format a download speed in bytes per second.
 */
fun formatSpeed(bytesPerSecond: Long): String {
    if (bytesPerSecond <= 0) return "—"
    return formatBytes(bytesPerSecond) + "/s"
}

/**
 * Format an ETA in milliseconds to a "Xm Ys" string.
 */
fun formatEta(ms: Long): String {
    if (ms <= 0) return "—"
    val seconds = ms / 1000
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${s}s"
        else -> "${s}s"
    }
}
