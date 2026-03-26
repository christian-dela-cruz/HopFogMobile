package com.example.hopfog

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hopfog.data.AppDatabase
import com.example.hopfog.data.ChatRepository
import com.example.hopfog.data.MessageEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Helper class to represent the cooldown state
sealed class CooldownState {
    object Ready : CooldownState()
    data class CoolingDown(val secondsRemaining: Int) : CooldownState()
}

class ChatViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    val conversations = _conversations.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _contactName = MutableStateFlow("")
    val contactName = _contactName.asStateFlow()

    // --- NEW STATE FOR COOLDOWN ---
    private val _cooldownState = MutableStateFlow<CooldownState>(CooldownState.Ready)
    val cooldownState = _cooldownState.asStateFlow()

    private var cooldownJob: Job? = null
    private var pollingJob: Job? = null
    private var roomCollectorJob: Job? = null

    // Tracks the other participant's user ID for Room-backed conversations.
    // @Volatile ensures visibility across coroutines on different threads.
    @Volatile
    private var currentOtherUserId: Int = 0

    // Lazily initialised repository — avoids needing Context at construction time.
    private var repository: ChatRepository? = null

    private fun getRepository(context: Context): ChatRepository {
        return repository ?: ChatRepository(
            AppDatabase.getDatabase(context).messageDao()
        ).also { repository = it }
    }

    /**
     * Converts a Room [MessageEntity] to the UI-facing [Message] model.
     * [currentUserId] is used to derive [Message.isFromCurrentUser].
     * [contactName] is used as the display name for the other participant.
     */
    private fun MessageEntity.toMessage(currentUserId: Int, contactName: String): Message {
        val isOwn = senderId == currentUserId
        return Message(
            messageId = id,
            messageText = content,
            sentAt = sentAt.toString(),
            senderId = senderId,
            isFromCurrentUser = isOwn,
            senderUsername = if (isOwn) "" else contactName
        )
    }

    /**
     * Converts a network [Message] to a [MessageEntity] suitable for Room storage.
     * [userId] is the current user's ID; [otherUserId] is the other participant's ID.
     * Both are needed to derive the receiver when [Message] doesn't carry that field.
     */
    private fun Message.toEntity(userId: Int, otherUserId: Int): MessageEntity {
        val receiverId = if (isFromCurrentUser) otherUserId else userId
        return MessageEntity(
            id = messageId,
            senderId = senderId,
            receiverId = receiverId,
            content = messageText,
            sentAt = parseTimestampToMillis(sentAt) / 1000L
        )
    }

    // --- END OF NEW STATE ---

    fun loadConversations(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _conversations.value = NetworkManager.getConversations(context)
            _isLoading.value = false
        }
    }

    /**
     * Loads messages for a conversation.
     *
     * When [otherUserId] is provided (non-zero) the call uses an offline-first strategy:
     *  1. Immediately collect cached messages from the local Room DB.
     *  2. Background-sync the full conversation from the server and store results in Room.
     *
     * When [otherUserId] is 0 (unknown), falls back to a simple network fetch.
     */
    fun loadMessages(context: Context, conversationId: Int, name: String, otherUserId: Int = 0) {
        _contactName.value = name
        currentOtherUserId = otherUserId
        val userId = SessionManager.getUserId(context)

        if (otherUserId != 0 && userId != -1) {
            _isLoading.value = true

            // Collect Room DB changes and push them to the UI state
            roomCollectorJob?.cancel()
            roomCollectorJob = viewModelScope.launch {
                getRepository(context).getConversation(userId, otherUserId)
                    .collect { entities ->
                        _messages.value = entities.map { it.toMessage(userId, name) }
                        if (entities.isNotEmpty()) {
                            _isLoading.value = false
                        }
                    }
            }

            // Sync full conversation history from server in the background
            viewModelScope.launch {
                getRepository(context).syncConversation(context, userId, otherUserId)
                // If Room was empty the collector won't have cleared the loading flag
                _isLoading.value = false
            }
        } else {
            // Fallback: no Room support for this conversation
            viewModelScope.launch {
                _isLoading.value = true
                _messages.value = NetworkManager.getMessages(context, conversationId)
                _isLoading.value = false
            }
        }
    }

    /**
     * Sends a message and, on success:
     *  - Starts the send cooldown.
     *  - Re-fetches the conversation from the network.
     *  - Persists the refreshed messages into Room (when [currentOtherUserId] is known).
     *    In this case Room's Flow drives the UI update, so `_messages` is not set directly.
     *  - When [currentOtherUserId] is unknown, updates `_messages` directly.
     */
    fun sendMessage(context: Context, conversationId: Int, messageText: String, kind: String = "message") {
        // 1. Immediately block if already in cooldown
        if (_cooldownState.value is CooldownState.CoolingDown) return
        if (messageText.isBlank()) return

        viewModelScope.launch {
            val response = NetworkManager.sendMessage(context, conversationId, messageText, kind)

            if (response != null) {
                if (response.success) {
                    startCooldown(10)
                    val updatedMessages = NetworkManager.getMessages(context, conversationId)

                    val otherUserId = currentOtherUserId
                    val userId = SessionManager.getUserId(context)
                    if (otherUserId != 0 && userId != -1) {
                        // Room-backed path: persist into Room; collector updates the UI
                        getRepository(context).insertAll(
                            updatedMessages.map { it.toEntity(userId, otherUserId) }
                        )
                    } else {
                        // Non-Room path: update UI directly
                        _messages.value = updatedMessages
                    }
                } else {
                    if (response.secondsRemaining > 0) {
                        startCooldown(response.secondsRemaining)
                    }
                }
            }
        }
    }

    // --- NEW HELPER FUNCTIONS FOR COOLDOWN ---
    private fun startCooldown(durationSeconds: Int) {
        cooldownJob?.cancel() // Cancel any previous timer
        cooldownJob = viewModelScope.launch {
            for (i in durationSeconds downTo 1) {
                _cooldownState.value = CooldownState.CoolingDown(i)
                delay(1000)
            }
            _cooldownState.value = CooldownState.Ready
        }
    }

    // Call this to reset the timer when leaving a chat screen
    fun cancelCooldown() {
        cooldownJob?.cancel()
        _cooldownState.value = CooldownState.Ready
    }

    /**
     * Polls for new messages every 3 seconds.
     *
     * When [currentOtherUserId] is known, uses the incremental `/new-messages` endpoint and
     * inserts results into Room so the offline cache stays up to date.
     * Otherwise falls back to a full `/messages` refresh.
     */
    fun startPolling(context: Context, conversationId: Int) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(3000)
                try {
                    val otherUserId = currentOtherUserId
                    val userId = SessionManager.getUserId(context)
                    if (otherUserId != 0 && userId != -1) {
                        // Incremental update via /new-messages
                        val lastId = _messages.value.maxOfOrNull { it.messageId } ?: 0
                        val newMessages = NetworkManager.getNewMessages(context, lastId)
                        if (newMessages.isNotEmpty()) {
                            // Inserting into Room triggers the collector to update _messages
                            getRepository(context).insertAll(
                                newMessages.map { it.toEntity(userId, otherUserId) }
                            )
                        }
                    } else {
                        // Fallback: full refresh when otherUserId is unknown
                        _messages.value = NetworkManager.getMessages(context, conversationId)
                    }
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Polling error: ${e.message}")
                }
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        roomCollectorJob?.cancel()
        roomCollectorJob = null
    }
}