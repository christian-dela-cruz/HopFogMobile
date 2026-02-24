package com.example.hopfog

import org.junit.Test
import org.junit.Assert.*

class FormatTimestampTest {

    @Test
    fun formatTimestamp_nullInput_returnsEmpty() {
        assertEquals("", formatTimestamp(null))
    }

    @Test
    fun formatTimestamp_emptyInput_returnsEmpty() {
        assertEquals("", formatTimestamp(""))
    }

    @Test
    fun formatTimestamp_blankInput_returnsEmpty() {
        assertEquals("", formatTimestamp("   "))
    }

    @Test
    fun formatTimestamp_epochSeconds_returnsFormattedDate() {
        // 1708905600 = 2024-02-26 00:00:00 UTC
        val result = formatTimestamp("1708905600")
        // Should return a non-empty formatted string (exact value depends on timezone)
        assertTrue("Expected non-empty result for epoch timestamp", result.isNotBlank())
        // Should not return the raw epoch number
        assertFalse("Should not return raw epoch number", result == "1708905600")
    }

    @Test
    fun formatTimestamp_dateString_returnsTimePortion() {
        val result = formatTimestamp("2024-02-25 14:30:00")
        assertEquals("14:30", result)
    }

    @Test
    fun formatTimestamp_dateStringWithoutTime_returnsOriginal() {
        val result = formatTimestamp("2024-02-25")
        assertEquals("2024-02-25", result)
    }

    @Test
    fun formatTimestamp_invalidInput_returnsOriginal() {
        val result = formatTimestamp("not-a-timestamp")
        assertEquals("not-a-timestamp", result)
    }
}
