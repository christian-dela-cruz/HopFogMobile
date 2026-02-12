package com.example.hopfog

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Helper class to represent the cooldown state
sealed class CooldownState {
    data object Ready : CooldownState()
    data class CoolingDown(val secondsRemaining: Int) : CooldownState()
}

class ChatViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    val conversations = _conversations.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _contactName = MutableStateFlow("")
    val contactName = _contactName.asStateFlow()

    // --- Cooldown State for Online Mode ---
    private val _cooldownState = MutableStateFlow<CooldownState>(CooldownState.Ready)
    val cooldownState = _cooldownState.asStateFlow()

    private var cooldownJob: Job? = null

    fun loadConversations(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _conversations.value = NetworkManager.getConversations(context)
            _isLoading.value = false
        }
    }

    fun loadMessages(context: Context, conversationId: Int, name: String) {
        _contactName.value = name
        viewModelScope.launch {
            _isLoading.value = true
            _messages.value = NetworkManager.getMessages(context, conversationId)
            _isLoading.value = false
        }
    }

    // --- sendMessage for ONLINE mode ---
    fun sendMessage(context: Context, conversationId: Int, messageText: String) {
        if (_cooldownState.value is CooldownState.CoolingDown) return
        if (messageText.isBlank()) return

        viewModelScope.launch {
            val response = NetworkManager.sendMessage(context, conversationId, messageText)
            if (response != null) {
                if (response.success) {
                    startCooldown(30)
                    _messages.value = NetworkManager.getMessages(context, conversationId)
                } else {
                    if (response.secondsRemaining > 0) {
                        startCooldown(response.secondsRemaining)
                    }
                }
            }
        }
    }

    private fun startCooldown(durationSeconds: Int) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (i in durationSeconds downTo 1) {
                _cooldownState.value = CooldownState.CoolingDown(i)
                delay(1000)
            }
            _cooldownState.value = CooldownState.Ready
        }
    }

    fun cancelCooldown() {
        cooldownJob?.cancel()
        _cooldownState.value = CooldownState.Ready
    }


    // ##################################################################
    // --- NEW FUNCTIONS FOR OFFLINE (BLE) MESSAGING ---
    // ##################################################################

    /**
     * Instantly adds a new message to the UI with a "sending" status.
     * This is used to provide immediate feedback to the user in BLE mode.
     */
    fun addProvisionalMessage(messageText: String) {
        val provisionalMessage = Message(
            messageId = -1, // Use a temporary ID
            senderUsername = "Me", // Or get the actual current user's name
            messageText = messageText,
            timestamp = "Sending...",
            isFromCurrentUser = true,
            status = "sending"
        )
        // Add this new message to the end of the current list
        _messages.value = _messages.value + provisionalMessage
    }

    /**
     * Finds the last message sent by the current user and updates its status.
     * This is called from the BLE transaction callbacks.
     */
    fun updateLastMessageStatus(newStatus: String) {
        // Create a mutable copy of the list
        val updatedList = _messages.value.toMutableList()
        var lastSendingIndex = -1

        // --- THIS IS THE FIX ---
        // Replace findLastIndex with a standard loop that iterates backwards.
        for (i in updatedList.indices.reversed()) {
            val message = updatedList[i]
            if (message.isFromCurrentUser && message.status == "sending") {
                lastSendingIndex = i
                break // We found the last one, so we can stop looking.
            }
        }
        // --- END OF FIX ---

        if (lastSendingIndex != -1) {
            // Get the message to be updated
            val messageToUpdate = updatedList[lastSendingIndex]
            // Create a new message object with the updated status
            val updatedMessage = messageToUpdate.copy(
                status = newStatus,
                // Optionally update the timestamp to the current time
                timestamp = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            )
            // Replace the old message with the updated one
            updatedList[lastSendingIndex] = updatedMessage
            // Update the state flow to refresh the UI
            _messages.value = updatedList
        }
    }
}