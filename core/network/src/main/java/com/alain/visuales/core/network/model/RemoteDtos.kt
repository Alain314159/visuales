package com.alain.visuales.core.network.model

/**
 * A directory entry parsed from the server's listing (either listado.html or
 * a per-folder Nginxy listing).
 *
 * On disk this maps to a Room entity, but the network layer speaks these
 * lightweight DTOs to keep a clean boundary.
 */
data class RemoteDirectory(
    /** Full URL on the server, e.g. "https://visuales.uclv.cu/Peliculas/Cubanas/1938_Romance%20del%20Palmar/". */
    val url: String,
    /** URL-decoded path relative to the server, e.g. "/Peliculas/Cubanas/1938_Romance del Palmar". */
    val path: String,
    /** URL-decoded display name, e.g. "1938_Romance del Palmar". */
    val name: String,
    /** URL-decoded parent path, e.g. "/Peliculas/Cubanas". Empty for the root. */
    val parentPath: String,
    /** Depth in the tree (0 = root). */
    val depth: Int,
    /** First path segment, e.g. "Peliculas". */
    val section: String,
    /** Second path segment if present, e.g. "Cubanas". */
    val subcategory: String?
)

/**
 * A file or directory entry parsed from a per-folder Nginxy listing.
 */
data class RemoteEntry(
    val name: String,
    val url: String,
    /** Path relative to the server, e.g. "/Peliculas/Cubanas/Viva Cuba (2005).mp4". */
    val path: String,
    val isDirectory: Boolean,
    /** Size in bytes, or 0 if unknown / a directory. */
    val size: Long,
    /** Last modified timestamp in epoch millis, or null if unknown. */
    val modifiedAt: Long?,
    /** Detected entry type based on extension (more reliable than the Nginxy icon). */
    val type: String  // EntryType name
)

/**
 * Result of a HEAD request used to check if a remote resource changed.
 */
data class ResourceMetadata(
    val statusCode: Int,
    val contentLength: Long,
    val lastModified: Long?,
    val etag: String?
)
