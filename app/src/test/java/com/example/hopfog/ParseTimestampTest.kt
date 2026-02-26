package com.example.hopfog

import org.junit.Test
import org.junit.Assert.*

class ParseTimestampTest {

    @Test
    fun parseTimestampToMillis_nullInput_returnsZero() {
        assertEquals(0L, parseTimestampToMillis(null))
    }

    @Test
    fun parseTimestampToMillis_emptyInput_returnsZero() {
        assertEquals(0L, parseTimestampToMillis(""))
    }

    @Test
    fun parseTimestampToMillis_blankInput_returnsZero() {
        assertEquals(0L, parseTimestampToMillis("   "))
    }

    @Test
    fun parseTimestampToMillis_epochSeconds_returnsMillis() {
        // 1708905600 seconds = 1708905600000 millis
        assertEquals(1708905600000L, parseTimestampToMillis("1708905600"))
    }

    @Test
    fun parseTimestampToMillis_dateString_returnsNonZero() {
        val result = parseTimestampToMillis("2024-02-25 14:30:00")
        assertTrue("Expected non-zero result for valid date string", result > 0L)
    }

    @Test
    fun parseTimestampToMillis_invalidInput_returnsZero() {
        assertEquals(0L, parseTimestampToMillis("not-a-timestamp"))
    }

    @Test
    fun parseTimestampToMillis_newerTimestamp_isGreater() {
        val older = parseTimestampToMillis("1708905600") // 2024-02-26
        val newer = parseTimestampToMillis("1708992000") // 2024-02-27
        assertTrue("Newer timestamp should have greater millis value", newer > older)
    }

    @Test
    fun parseTimestampToMillis_sortingOrder() {
        val announcements = listOf(
            Announcement(1, "Old", "msg", "1708905600"),
            Announcement(2, "New", "msg", "1708992000"),
            Announcement(3, "Middle", "msg", "1708948800")
        )

        val newestFirst = announcements.sortedByDescending { parseTimestampToMillis(it.createdAt) }
        assertEquals(2, newestFirst[0].id) // Newest
        assertEquals(3, newestFirst[1].id) // Middle
        assertEquals(1, newestFirst[2].id) // Oldest

        val oldestFirst = announcements.sortedBy { parseTimestampToMillis(it.createdAt) }
        assertEquals(1, oldestFirst[0].id) // Oldest
        assertEquals(3, oldestFirst[1].id) // Middle
        assertEquals(2, oldestFirst[2].id) // Newest
    }
}
