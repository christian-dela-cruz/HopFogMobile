package com.example.hopfog

import android.content.Context
import android.widget.Toast
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MOCK (UI-only) NetworkManager
 * This replaces all real backend calls with local, in-memory data.
 */
object NetworkManager {

    private const val SIMULATE_NETWORK_ERRORS = false

    private fun Context.toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private object MockStore {
        private val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        val users: MutableList<SelectableUser> = mutableListOf(
            SelectableUser(id = 1, username = "HopFog Admin"),
            SelectableUser(id = 2, username = "Barangay Desk"),
            SelectableUser(id = 3, username = "Resident A"),
            SelectableUser(id = 4, username = "Resident B")
        )

        val conversations: MutableList<ChatConversation> = mutableListOf(
            ChatConversation(
                conversationId = 101,
                contactName = "HopFog Admin",
                lastMessage = "Welcome to HopFog (UI-only demo).",
                timestamp = timeFmt.format(Date())
            ),
            ChatConversation(
                conversationId = 102,
                contactName = "Barangay Desk",
                lastMessage = "This is a sample conversation.",
                timestamp = timeFmt.format(Date())
            )
        )

        private val messagesByConversation: MutableMap<Int, MutableList<Message>> = mutableMapOf(
            101 to mutableListOf(
                Message(
                    messageId = 1,
                    messageText = "Welcome to HopFog! (UI-only demo)",
                    timestamp = timeFmt.format(Date()), // Use timestamp instead of sentAt
                    isFromCurrentUser = false,
                    senderUsername = "HopFog Admin",
                    status = "delivered" // FIXED: Add status field
                ),
                Message(
                    messageId = 2,
                    messageText = "You can navigate the UI without any backend.",
                    timestamp = timeFmt.format(Date()), // Use timestamp instead of sentAt
                    isFromCurrentUser = false,
                    senderUsername = "HopFog Admin",
                    status = "delivered" // FIXED: Add status field
                )
            ),
            102 to mutableListOf(
                Message(
                    messageId = 1,
                    messageText = "Hello! This is a mock thread.",
                    timestamp = timeFmt.format(Date()), // Use timestamp instead of sentAt
                    isFromCurrentUser = false,
                    senderUsername = "Barangay Desk",
                    status = "delivered" // FIXED: Add status field
                )
            )
        )

        private var nextConversationId = 200

        fun getMessages(conversationId: Int): List<Message> {
            return messagesByConversation[conversationId]?.toList() ?: emptyList()
        }

        fun addMessage(
            context: Context,
            conversationId: Int,
            messageText: String,
            currentUsername: String
        ): SendMessageResponse {
            val list = messagesByConversation.getOrPut(conversationId) { mutableListOf() }
            val nextId = (list.maxOfOrNull { it.messageId } ?: 0) + 1
            val msg = Message(
                messageId = nextId,
                messageText = messageText,
                timestamp = timeFmt.format(Date()), // Use timestamp instead of sentAt
                isFromCurrentUser = true,
                senderUsername = currentUsername,
                status = "delivered" // FIXED: Add status field
            )
            list.add(msg)

            conversations.indexOfFirst { it.conversationId == conversationId }
                .takeIf { it >= 0 }
                ?.let { idx ->
                    conversations[idx] = conversations[idx].copy(
                        lastMessage = messageText,
                        timestamp = timeFmt.format(Date())
                    )
                }

            context.toast("Message sent (mock).")
            return SendMessageResponse(success = true, message = "sent")
        }

        fun findOrCreateChatWithUser(otherUserId: Int): SosChatResponse {
            val user = users.firstOrNull { it.id == otherUserId }
            val name = user?.username ?: "User $otherUserId"

            val existing = conversations.firstOrNull { it.contactName == name }
            if (existing != null) {
                return SosChatResponse(conversationId = existing.conversationId, contactName = existing.contactName)
            }

            val newId = nextConversationId++
            val convo = ChatConversation(
                conversationId = newId,
                contactName = name,
                lastMessage = "Say hi! (mock chat)",
                timestamp = timeFmt.format(Date())
            )
            conversations.add(0, convo)
            messagesByConversation[newId] = mutableListOf(
                Message(
                    messageId = 1,
                    messageText = "This chat was created offline (mock).",
                    timestamp = timeFmt.format(Date()), // Use timestamp instead of sentAt
                    isFromCurrentUser = false,
                    senderUsername = name,
                    status = "delivered" // FIXED: Add status field
                )
            )
            return SosChatResponse(conversationId = newId, contactName = name)
        }
    }

    suspend fun registerUser(context: Context, username: String, email: String, password: String): Boolean {
        if (SIMULATE_NETWORK_ERRORS) {
            context.toast("Mock: registration failed.")
            return false
        }
        context.toast("Registered (mock). You can now sign in.")
        return true
    }

    suspend fun loginUser(context: Context, usernameOrEmail: String, password: String): JSONObject? {
        if (SIMULATE_NETWORK_ERRORS) {
            context.toast("Mock: login failed.")
            return null
        }
        val userId = 999
        val username = if (usernameOrEmail.isBlank()) "Demo User" else usernameOrEmail
        SessionManager.saveSession(
            context = context,
            userId = userId,
            username = username,
            hasAgreedSos = SessionManager.hasAgreedToSos(context)
        )
        return JSONObject().apply {
            put("success", true)
            put("user", JSONObject().apply {
                put("user_id", userId)
                put("username", username)
                put("has_agreed_sos", SessionManager.hasAgreedToSos(context))
            })
        }
    }

    suspend fun getConversations(context: Context): List<ChatConversation> {
        if (SIMULATE_NETWORK_ERRORS) {
            context.toast("Mock: failed to fetch conversations.")
            return emptyList()
        }
        return MockStore.conversations.toList()
    }

    suspend fun getMessages(context: Context, conversationId: Int): List<Message> {
        if (SIMULATE_NETWORK_ERRORS) {
            context.toast("Mock: failed to fetch messages.")
            return emptyList()
        }
        return MockStore.getMessages(conversationId)
    }

    suspend fun sendMessage(context: Context, conversationId: Int, messageText: String): SendMessageResponse? {
        if (SIMULATE_NETWORK_ERRORS) {
            context.toast("Mock: message not sent.")
            return null
        }
        val currentUsername = SessionManager.getUsername(context).ifBlank { "Demo User" }
        return MockStore.addMessage(context, conversationId, messageText, currentUsername)
    }

    suspend fun findOrCreateSosChat(context: Context): SosChatResponse? {
        if (SIMULATE_NETWORK_ERRORS) {
            context.toast("Mock: failed to start SOS chat.")
            return null
        }
        val existing = MockStore.conversations.firstOrNull { it.contactName == "SOS" }
        if (existing != null) {
            return SosChatResponse(existing.conversationId, existing.contactName)
        }
        val convo = ChatConversation(
            conversationId = 911,
            contactName = "SOS",
            lastMessage = "SOS channel (offline demo)",
            timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        )
        (MockStore.conversations as MutableList).add(0, convo)
        return SosChatResponse(conversationId = 911, contactName = "SOS")
    }

    suspend fun changePassword(context: Context, oldPass: String, newPass: String): Boolean {
        if (SIMULATE_NETWORK_ERRORS) {
            context.toast("Mock: change password failed.")
            return false
        }
        context.toast("Password updated (mock).")
        return true
    }

    suspend fun runMessageCleanup(context: Context): Boolean {
        return true
    }

    suspend fun agreeToSos(context: Context): Boolean {
        if (SIMULATE_NETWORK_ERRORS) {
            context.toast("Mock: failed to update SOS agreement.")
            return false
        }
        SessionManager.setHasAgreedToSos(context, true)
        context.toast("SOS agreement saved (mock).")
        return true
    }

    suspend fun getNewMessages(context: Context, lastMessageId: Int): List<Message> {
        return emptyList()
    }

    suspend fun getAllUsers(context: Context): List<SelectableUser> {
        return MockStore.users.toList()
    }

    suspend fun findOrCreateChatWithUser(context: Context, otherUserId: Int): SosChatResponse? {
        if (SIMULATE_NETWORK_ERRORS) {
            context.toast("Mock: failed to create chat.")
            return null
        }
        return MockStore.findOrCreateChatWithUser(otherUserId)
    }
}