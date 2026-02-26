package com.example.hopfog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable



@Serializable
data class SendMessageRequest(
    @SerialName("conversation_id")
    val conversationId: Int,
    @SerialName("message_text")
    val messageText: String
)

@Serializable
data class ChatConversation(
    @SerialName("conversation_id")
    val conversationId: Int,
    @SerialName("contact_name")
    val contactName: String,
    @SerialName("last_message")
    val lastMessage: String?,
    @SerialName("timestamp")
    val timestamp: String?
)

@Serializable
data class Message(
    @SerialName("message_id")
    val messageId: Int,
    @SerialName("message_text")
    val messageText: String,
    @SerialName("sent_at")
    val sentAt: String,
    @SerialName("sender_id")
    val senderId: Int,
    @SerialName("is_from_current_user")
    val isFromCurrentUser: Boolean,
    @SerialName("sender_username")
    val senderUsername: String
)

@Serializable
data class SosChatResponse(
    @SerialName("conversation_id")
    val conversationId: Int,
    @SerialName("contact_name")
    val contactName: String
)

@Serializable
data class ChangePasswordRequest(
    @SerialName("old_password")
    val oldPassword: String,
    @SerialName("new_password")
    val newPassword: String
)

@Serializable
data class GenericErrorResponse(
    val error: String? = null
)

@Serializable
data class SendMessageResponse(
    val success: Boolean,
    val message: String? = null,
    // This will capture the 'secondsRemaining' from the server error response
    @SerialName("secondsRemaining")
    val secondsRemaining: Int = 0
)

// This specifically captures the JSON format: {"error": "some message"}
@Serializable
data class ErrorResponse(
    val error: String
)

@Serializable
data class SelectableUser(
    @SerialName("id")
    val id: Int,
    @SerialName("username")
    val username: String
)

@Serializable
data class Announcement(
    @SerialName("id")
    val id: Int,
    @SerialName("title")
    val title: String,
    @SerialName("message")
    val message: String,
    @SerialName("created_at")
    val createdAt: String? = null
)

/**
 * Parses a timestamp string into epoch milliseconds for sorting.
 * Handles both Unix epoch seconds (numeric strings) and date strings (e.g., "2024-02-25 14:30:00").
 * Returns 0 if parsing fails.
 */
fun parseTimestampToMillis(value: String?): Long {
    if (value.isNullOrBlank()) return 0L
    return try {
        if (value.all { it.isDigit() }) {
            value.toLong() * 1000L
        } else {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .parse(value)?.time ?: 0L
        }
    } catch (e: Exception) {
        0L
    }
}

/**
 * Formats a timestamp string for display. Handles both Unix epoch seconds
 * (numeric strings from ESP32) and date strings (e.g., "2024-02-25 14:30:00").
 * Returns "HH:mm" for today's timestamps, "MMM dd, HH:mm" for older dates.
 */
fun formatTimestamp(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return try {
        if (value.all { it.isDigit() }) {
            val date = java.util.Date(value.toLong() * 1000L)
            val todayFmt = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
            val isToday = todayFmt.format(date) == todayFmt.format(java.util.Date())
            if (isToday) {
                java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(date)
            } else {
                java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(date)
            }
        } else {
            // Try to extract time portion from date string like "2024-02-25 14:30:00"
            value.split(" ").getOrNull(1)?.substringBeforeLast(":") ?: value
        }
    } catch (e: Exception) {
        value
    }
}