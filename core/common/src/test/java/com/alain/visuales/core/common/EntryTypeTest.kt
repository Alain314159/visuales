package com.alain.visuales.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EntryTypeTest {

    @Test
    fun `fromFilename returns VIDEO for mp4`() {
        assertThat(EntryType.fromFilename("movie.mp4")).isEqualTo(EntryType.VIDEO)
    }

    @Test
    fun `fromFilename returns VIDEO for mkv`() {
        assertThat(EntryType.fromFilename("movie.mkv")).isEqualTo(EntryType.VIDEO)
    }

    @Test
    fun `fromFilename returns VIDEO for avi mov wmv flv webm`() {
        assertThat(EntryType.fromFilename("a.avi")).isEqualTo(EntryType.VIDEO)
        assertThat(EntryType.fromFilename("a.mov")).isEqualTo(EntryType.VIDEO)
        assertThat(EntryType.fromFilename("a.wmv")).isEqualTo(EntryType.VIDEO)
        assertThat(EntryType.fromFilename("a.flv")).isEqualTo(EntryType.VIDEO)
        assertThat(EntryType.fromFilename("a.webm")).isEqualTo(EntryType.VIDEO)
    }

    @Test
    fun `fromFilename returns SUBTITLE for srt ass vtt`() {
        assertThat(EntryType.fromFilename("movie.srt")).isEqualTo(EntryType.SUBTITLE)
        assertThat(EntryType.fromFilename("movie.ass")).isEqualTo(EntryType.SUBTITLE)
        assertThat(EntryType.fromFilename("movie.vtt")).isEqualTo(EntryType.SUBTITLE)
    }

    @Test
    fun `fromFilename returns IMAGE for jpg png gif webp`() {
        assertThat(EntryType.fromFilename("poster.jpg")).isEqualTo(EntryType.IMAGE)
        assertThat(EntryType.fromFilename("poster.png")).isEqualTo(EntryType.IMAGE)
        assertThat(EntryType.fromFilename("poster.gif")).isEqualTo(EntryType.IMAGE)
        assertThat(EntryType.fromFilename("poster.webp")).isEqualTo(EntryType.IMAGE)
    }

    @Test
    fun `fromFilename returns ARCHIVE for zip rar 7z iso`() {
        assertThat(EntryType.fromFilename("data.zip")).isEqualTo(EntryType.ARCHIVE)
        assertThat(EntryType.fromFilename("data.rar")).isEqualTo(EntryType.ARCHIVE)
        assertThat(EntryType.fromFilename("data.7z")).isEqualTo(EntryType.ARCHIVE)
        assertThat(EntryType.fromFilename("data.iso")).isEqualTo(EntryType.ARCHIVE)
    }

    @Test
    fun `fromFilename returns AUDIO for mp3 ogg flac`() {
        assertThat(EntryType.fromFilename("song.mp3")).isEqualTo(EntryType.AUDIO)
        assertThat(EntryType.fromFilename("song.ogg")).isEqualTo(EntryType.AUDIO)
        assertThat(EntryType.fromFilename("song.flac")).isEqualTo(EntryType.AUDIO)
    }

    @Test
    fun `fromFilename returns DOCUMENT for pdf epub mobi`() {
        assertThat(EntryType.fromFilename("book.pdf")).isEqualTo(EntryType.DOCUMENT)
        assertThat(EntryType.fromFilename("book.epub")).isEqualTo(EntryType.DOCUMENT)
        assertThat(EntryType.fromFilename("book.mobi")).isEqualTo(EntryType.DOCUMENT)
    }

    @Test
    fun `fromFilename is case-insensitive`() {
        assertThat(EntryType.fromFilename("MOVIE.MP4")).isEqualTo(EntryType.VIDEO)
        assertThat(EntryType.fromFilename("Movie.MKV")).isEqualTo(EntryType.VIDEO)
        assertThat(EntryType.fromFilename("MOVIE.SRT")).isEqualTo(EntryType.SUBTITLE)
    }

    @Test
    fun `fromFilename returns OTHER for unknown extensions`() {
        assertThat(EntryType.fromFilename("file.xyz")).isEqualTo(EntryType.OTHER)
        assertThat(EntryType.fromFilename("file.unknownext")).isEqualTo(EntryType.OTHER)
    }

    @Test
    fun `fromFilename returns OTHER for files without extension`() {
        assertThat(EntryType.fromFilename("README")).isEqualTo(EntryType.OTHER)
        assertThat(EntryType.fromFilename("Makefile")).isEqualTo(EntryType.OTHER)
    }

    @Test
    fun `fromFilename returns OTHER for empty string`() {
        assertThat(EntryType.fromFilename("")).isEqualTo(EntryType.OTHER)
    }

    @Test
    fun `isVideo returns true for video extensions`() {
        assertThat(EntryType.isVideo("movie.mp4")).isTrue()
        assertThat(EntryType.isVideo("movie.mkv")).isTrue()
        assertThat(EntryType.isVideo("movie.avi")).isTrue()
    }

    @Test
    fun `isVideo returns false for non-video extensions`() {
        assertThat(EntryType.isVideo("poster.jpg")).isFalse()
        assertThat(EntryType.isVideo("song.mp3")).isFalse()
        assertThat(EntryType.isVideo("book.pdf")).isFalse()
        assertThat(EntryType.isVideo("subtitle.srt")).isFalse()
    }

    @Test
    fun `isVideo returns false for empty filename`() {
        assertThat(EntryType.isVideo("")).isFalse()
    }
}
