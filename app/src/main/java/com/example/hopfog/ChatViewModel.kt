package com.example.hopfog

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChatViewModel : ViewModel() {

    // --- Properties for the Conversation List Page ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    val conversations = _conversations.asStateFlow()

    // --- Properties for the Single Message Page ---
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _contactName = MutableStateFlow("")
    val contactName = _contactName.asStateFlow()

    val connectionStatus = BleManager.status

    // --- Functions for the Conversation List Page ---
    fun loadConversations(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            // This will get the conversations from the MockStore inside NetworkManager
            _conversations.value = NetworkManager.getConversations(context)
            _isLoading.value = false
        }
    }

    // --- Functions for the Single Message Page ---
    fun prepareChat(conversationId: Int, name: String) {
        _contactName.value = name
        // This will get messages for a specific chat from the MockStore
        _messages.value = NetworkManager.MockStore.getMessages(conversationId)
    }

    fun connectToHub() {
        // --- THIS IS THE FIX ---
        // Only try to connect if we are currently disconnected.
        // If a connection was already established by the LoginPage, this will do nothing,
        // which is exactly what we want.
        if (BleManager.status.value is ConnectionStatus.Disconnected) {
            Log.d("ChatViewModel", "Requesting new BLE connection...")
            BleManager.connect()
        } else {
            Log.d("ChatViewModel", "Using existing BLE connection established during login.")
        }
    }

    fun disconnectFromHub() {
        Log.d("ChatViewModel", "Requesting BLE disconnection...")
        BleManager.disconnect()
    }

    fun sendMessage(context: Context, messageText: String) {
        if (messageText.isBlank()) return
        if (connectionStatus.value != ConnectionStatus.Connected) {
            Log.w("ChatViewModel", "Cannot send message, not connected.")
            return
        }

        addProvisionalMessage(messageText)

        val currentUserId = SessionManager.getUserId(context)
        val currentUsername = SessionManager.getUsername(context)
        val messageData = mapOf(
            "action" to "sendMessage",
            "sender_id" to currentUserId,
            "sender_username" to currentUsername,
            "text" to messageText
        )
        val jsonString = Gson().toJson(messageData)

        BleManager.sendJson(jsonString)
        updateLastMessageStatus("delivered") // Assume success for now
    }

    // --- Helper functions for UI updates ---
    private fun addProvisionalMessage(messageText: String) {
        val provisionalMessage = Message(
            messageId = -1,
            senderUsername = "Me",
            messageText = messageText,
            timestamp = "Sending...",
            isFromCurrentUser = true,
            status = "sending"
        )
        _messages.value = _messages.value + provisionalMessage
    }

    private fun updateLastMessageStatus(newStatus: String) {
        val updatedList = _messages.value.toMutableList()
        val lastSendingIndex = updatedList.indexOfLast { it.isFromCurrentUser && it.status == "sending" }

        if (lastSendingIndex != -1) {
            val messageToUpdate = updatedList[lastSendingIndex]
            val updatedMessage = messageToUpdate.copy(
                status = newStatus,
                timestamp = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            )
            updatedList[lastSendingIndex] = updatedMessage
            _messages.value = updatedList
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnectFromHub()
    }
}