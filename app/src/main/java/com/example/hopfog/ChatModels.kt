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

//@Serializable
//data class Message(
//    @SerialName("message_id")
//    val messageId: Int,
//    @SerialName("message_text")
//    val messageText: String,
//    @SerialName("sent_at")
//    val sentAt: String,
//    @SerialName("sender_id")
//    val senderId: Int,
//    @SerialName("is_from_current_user")
//    val isFromCurrentUser: Boolean,
//    @SerialName("sender_username")
//    val senderUsername: String
//)

data class Message(
    val messageId: Int,
    val senderUsername: String,
    val messageText: String,
    val timestamp: String,
    val isFromCurrentUser: Boolean,
    var status: String = "delivered" // Add this field. "delivered", "sending", "failed"
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

