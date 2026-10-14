package com.alain.visuales.core.common

/**
 * File extension → media type classification.
 *
 * Important: the Nginxy theme on the server only supports ~48 extensions and
 * classifies anything else as `error.png`. The `.mkv` extension is NOT in their
 * list, so we MUST classify by extension from the filename, not by the icon
 * that the HTML uses. This is the source of truth for type detection.
 */
enum class EntryType {
    DIRECTORY,
    VIDEO,
    SUBTITLE,
    IMAGE,
    ARCHIVE,
    AUDIO,
    DOCUMENT,
    OTHER;

    companion object {
        /** Map of lowercase extension (no dot) → EntryType. */
        private val EXT_MAP: Map<String, EntryType> = buildMap {
            // Video
            listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg", "m2ts", "ts", "vob", "3gp").forEach {
                put(it, VIDEO)
            }
            // Subtitles
            listOf("srt", "ass", "ssa", "vtt", "sub", "idx").forEach { put(it, SUBTITLE) }
            // Images
            listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "svg", "ico", "raw").forEach { put(it, IMAGE) }
            // Archives
            listOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso", "tgz").forEach { put(it, ARCHIVE) }
            // Audio
            listOf("mp3", "ogg", "wav", "wma", "flac", "aac", "m4a", "opus").forEach { put(it, AUDIO) }
            // Documents
            listOf("pdf", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "epub", "mobi", "djvu").forEach { put(it, DOCUMENT) }
        }

        /** Classify a filename by extension. */
        fun fromFilename(filename: String): EntryType {
            val ext = filename.substringAfterLast('.', "").lowercase()
            return EXT_MAP[ext] ?: OTHER
        }

        /** Returns true if the entry is downloadable as a video. */
        fun isVideo(filename: String): Boolean = fromFilename(filename) == VIDEO
    }
}

/**
 * Media types derived from path patterns + filename heuristics.
 */
enum class MediaType {
    MOVIE,
    SERIES,
    SERIES_EPISODE,
    DOCUMENTARY,
    COURSE,
    COURSE_LESSON,
    MUSIC_ALBUM,
    MUSIC_TRACK,
    BOOK,
    OTHER
}
