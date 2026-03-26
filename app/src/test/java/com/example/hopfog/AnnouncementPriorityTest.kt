package com.example.hopfog

import org.junit.Test
import org.junit.Assert.*

class AnnouncementPriorityTest {

    @Test
    fun priorityRank_sosTitle_returnsZero() {
        val announcement = Announcement(1, "SOS: Flood Warning", "msg")
        assertEquals(0, announcementPriorityRank(announcement))
    }

    @Test
    fun priorityRank_sosLowercase_returnsZero() {
        val announcement = Announcement(1, "sos emergency", "msg")
        assertEquals(0, announcementPriorityRank(announcement))
    }

    @Test
    fun priorityRank_alertTitle_returnsOne() {
        val announcement = Announcement(1, "Weather Alert", "msg")
        assertEquals(1, announcementPriorityRank(announcement))
    }

    @Test
    fun priorityRank_alertLowercase_returnsOne() {
        val announcement = Announcement(1, "alert: power outage", "msg")
        assertEquals(1, announcementPriorityRank(announcement))
    }

    @Test
    fun priorityRank_regularAnnouncement_returnsTwo() {
        val announcement = Announcement(1, "Community Meeting", "msg")
        assertEquals(2, announcementPriorityRank(announcement))
    }

    @Test
    fun priorityRank_noFalsePositive_sosSubstring() {
        // "Prisoner Transport" should NOT match as SOS
        val announcement = Announcement(1, "Prisoner Transport", "msg")
        assertEquals(2, announcementPriorityRank(announcement))
    }

    @Test
    fun priorityRank_noFalsePositive_alertSubstring() {
        // "Ballert" should NOT match as Alert
        val announcement = Announcement(1, "Ballert Street Closure", "msg")
        assertEquals(2, announcementPriorityRank(announcement))
    }

    @Test
    fun prioritySorting_correctOrder() {
        val announcements = listOf(
            Announcement(1, "Community Meeting", "msg", "1708992000"),
            Announcement(2, "SOS: Flood", "msg", "1708905600"),
            Announcement(3, "Weather Alert", "msg", "1708948800"),
            Announcement(4, "General Info", "msg", "1708990000")
        )

        val sorted = announcements.sortedWith(
            compareBy<Announcement> { announcementPriorityRank(it) }
                .thenByDescending { parseTimestampToMillis(it.createdAt) }
        )

        // SOS first
        assertEquals(2, sorted[0].id)
        // Alert second
        assertEquals(3, sorted[1].id)
        // Regular announcements by newest first
        assertEquals(1, sorted[2].id)
        assertEquals(4, sorted[3].id)
    }

    @Test
    fun prioritySorting_samePriority_sortedByNewest() {
        val announcements = listOf(
            Announcement(1, "SOS: Old", "msg", "1708905600"),
            Announcement(2, "SOS: New", "msg", "1708992000")
        )

        val sorted = announcements.sortedWith(
            compareBy<Announcement> { announcementPriorityRank(it) }
                .thenByDescending { parseTimestampToMillis(it.createdAt) }
        )

        assertEquals(2, sorted[0].id) // Newer SOS first
        assertEquals(1, sorted[1].id) // Older SOS second
    }
}
