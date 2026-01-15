package com.example.hopfog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val isFromCurrentUser: Boolean
)