package com.example.hopfog

import android.content.Context
import android.widget.Toast
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MOCK (UI-only) NetworkManager
 * ----------------------------
 * This replaces all real backend calls with local, in-memory data so the app UI
 * still works even with no server/Wi‑Fi.
 *
 * You can later re-bind to a backend by restoring your previous Ktor-based
 * implementation or by introducing an interface + DI.
 */
object NetworkManager {

    // Toggle if you want to intentionally simulate failures.
    // (Kept here so you can demo error states without a server.)
    private const val SIMULATE_NETWORK_ERRORS = false

    private fun Context.toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // ---- Simple in-memory "backend" store ----
    private object MockStore {
        private val timeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        // Demo users (used by NewMessagePage)
        val users: MutableList<SelectableUser> = mutableListOf(
            SelectableUser(id = 1, username = "HopFog Admin"),
            SelectableUser(id = 2, username = "Barangay Desk"),
            SelectableUser(id = 3, username = "Resident A"),
            SelectableUser(id = 4, username = "Resident B")
        )

        // Conversations shown in ChatsListPage
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

        // Messages per conversation
        private val messagesByConversation: MutableMap<Int, MutableList<Message>> = mutableMapOf(
            101 to mutableListOf(
                Message(
                    messageId = 1,
                    messageText = "Welcome to HopFog! (UI-only demo)",
                    sentAt = timeFmt.format(Date()),
                    senderId = 1,
                    isFromCurrentUser = false,
                    senderUsername = "HopFog Admin"
                ),
                Message(
                    messageId = 2,
                    messageText = "You can navigate the UI without any backend.",
                    sentAt = timeFmt.format(Date()),
                    senderId = 1,
                    isFromCurrentUser = false,
                    senderUsername = "HopFog Admin"
                )
            ),
            102 to mutableListOf(
                Message(
                    messageId = 1,
                    messageText = "Hello! This is a mock thread.",
                    sentAt = timeFmt.format(Date()),
                    senderId = 2,
                    isFromCurrentUser = false,
                    senderUsername = "Barangay Desk"
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
            currentUserId: Int,
            currentUsername: String
        ): SendMessageResponse {
            val list = messagesByConversation.getOrPut(conversationId) { mutableListOf() }
            val nextId = (list.maxOfOrNull { it.messageId } ?: 0) + 1
            val msg = Message(
                messageId = nextId,
                messageText = messageText,
                sentAt = timeFmt.format(Date()),
                senderId = currentUserId,
                isFromCurrentUser = true,
                senderUsername = currentUsername
            )
            list.add(msg)

            // Update conversation preview
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

            // If a conversation already exists with that name, reuse it.
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
                    sentAt = timeFmt.format(Date()),
                    senderId = otherUserId,
                    isFromCurrentUser = false,
                    senderUsername = name
                )
            )
            return SosChatResponse(conversationId = newId, contactName = name)
        }
    }

    // ---- Public API used by your UI ----
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

        // Minimal structure expected by UserViewModel.onLoginSuccess()
        val userId = 999
        val username = if (usernameOrEmail.isBlank()) "Demo User" else usernameOrEmail
        val email = if (usernameOrEmail.contains("@")) usernameOrEmail else "demo@hopfog.local"

        // Persist session like the old backend did.
        SessionManager.saveSession(
            context = context,
            userId = userId,
            username = username,
            hasAgreedSos = SessionManager.hasAgreedToSos(context) // keep current
        )

        return JSONObject().apply {
            put("success", true)
            put("user", JSONObject().apply {
                put("user_id", userId)
                put("username", username)
                put("email", email)
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

        val currentUserId = SessionManager.getUserId(context).takeIf { it != -1 } ?: 999
        val currentUsername = SessionManager.getUsername(context).ifBlank { "Demo User" }
        return MockStore.addMessage(context, conversationId, messageText, currentUserId, currentUsername)
    }

    suspend fun findOrCreateSosChat(context: Context): SosChatResponse? {
        if (SIMULATE_NETWORK_ERRORS) {
            context.toast("Mock: failed to start SOS chat.")
            return null
        }
        // Reserve a consistent SOS conversation ID.
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
        // No-op for UI-only mode.
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
        // No push/polling in UI-only mode; keep service quiet.
        return emptyList()
    }

    suspend fun getAllUsers(context: Context): List<SelectableUser> {
        // In a real backend this would exclude the current user.
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
