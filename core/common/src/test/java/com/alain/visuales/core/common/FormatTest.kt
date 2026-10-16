package com.alain.visuales.core.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FormatTest {

    // ---- formatBytes ----

    @Test
    fun `formatBytes returns dash for zero or negative`() {
        assertThat(formatBytes(0)).isEqualTo("—")
        assertThat(formatBytes(-1)).isEqualTo("—")
        assertThat(formatBytes(-1000)).isEqualTo("—")
    }

    @Test
    fun `formatBytes returns bytes for values less than 1KB`() {
        assertThat(formatBytes(1)).isEqualTo("1 B")
        assertThat(formatBytes(512)).isEqualTo("512 B")
        assertThat(formatBytes(1023)).isEqualTo("1023 B")
    }

    @Test
    fun `formatBytes returns KB for values between 1KB and 1MB`() {
        assertThat(formatBytes(1024L)).isEqualTo("1 KB")
        assertThat(formatBytes(1536L)).isEqualTo("1.5 KB")
        assertThat(formatBytes(10240L)).isEqualTo("10 KB")
    }

    @Test
    fun `formatBytes returns MB for values between 1MB and 1GB`() {
        assertThat(formatBytes(1024 * 1024L)).isEqualTo("1 MB")
        assertThat(formatBytes((1024 * 1024 * 1.5).toLong())).isEqualTo("1.5 MB")
        assertThat(formatBytes(10 * 1024 * 1024L)).isEqualTo("10 MB")
    }

    @Test
    fun `formatBytes returns GB for values between 1GB and 1TB`() {
        assertThat(formatBytes(1024 * 1024 * 1024L)).isEqualTo("1 GB")
        assertThat(formatBytes(1024L * 1024 * 1024 * 10)).isEqualTo("10 GB")
    }

    @Test
    fun `formatBytes returns TB for very large values`() {
        val tb = 1024L * 1024 * 1024 * 1024
        assertThat(formatBytes(tb)).isEqualTo("1 TB")
        assertThat(formatBytes(tb * 10)).isEqualTo("10 TB")
    }

    @Test
    fun `formatBytes handles typical movie sizes`() {
        assertThat(formatBytes(700 * 1024 * 1024L)).isEqualTo("700 MB")
        assertThat(formatBytes(1400 * 1024 * 1024L)).isEqualTo("1.4 GB")
        assertThat(formatBytes(4 * 1024L * 1024 * 1024)).isEqualTo("4 GB")
    }

    // ---- formatSpeed ----

    @Test
    fun `formatSpeed returns dash for zero or negative`() {
        assertThat(formatSpeed(0)).isEqualTo("—")
        assertThat(formatSpeed(-1)).isEqualTo("—")
    }

    @Test
    fun `formatSpeed appends per second suffix`() {
        assertThat(formatSpeed(1024)).isEqualTo("1 KB/s")
        assertThat(formatSpeed(1024 * 1024L)).isEqualTo("1 MB/s")
        assertThat(formatSpeed(1024L * 1024 * 1024)).isEqualTo("1 GB/s")
    }

    // ---- formatEta ----

    @Test
    fun `formatEta returns dash for zero or negative`() {
        assertThat(formatEta(0)).isEqualTo("—")
        assertThat(formatEta(-1)).isEqualTo("—")
    }

    @Test
    fun `formatEta returns seconds only for less than a minute`() {
        assertThat(formatEta(30_000)).isEqualTo("30s")
        assertThat(formatEta(59_000)).isEqualTo("59s")
    }

    @Test
    fun `formatEta returns minutes and seconds`() {
        assertThat(formatEta(65_000)).isEqualTo("1m 5s")
        assertThat(formatEta(125_000)).isEqualTo("2m 5s")
    }

    @Test
    fun `formatEta returns hours and minutes for long durations`() {
        assertThat(formatEta(3_605_000)).isEqualTo("1h 0m")
        assertThat(formatEta(7_325_000)).isEqualTo("2h 2m")
    }
}
