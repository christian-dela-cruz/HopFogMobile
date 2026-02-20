package com.example.hopfog

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    // --- END OF NEW STATE ---

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

    // --- UPDATED sendMessage FUNCTION ---
    fun sendMessage(context: Context, conversationId: Int, messageText: String) {
        // 1. Immediately block if already in cooldown
        if (_cooldownState.value is CooldownState.CoolingDown) return
        if (messageText.isBlank()) return

        viewModelScope.launch {
            // This is the network call
            val response = NetworkManager.sendMessage(context, conversationId, messageText)

            if (response != null) {
                // The server gave a response
                if (response.success) {
                    // 2. If the server says success, start the client-side cooldown
                    startCooldown(10)
                    // And refresh the message list immediately
                    _messages.value = NetworkManager.getMessages(context, conversationId)
                } else {
                    // Server returned an error (like "please wait X seconds")
                    // We can optionally use response.secondsRemaining to sync our timer
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

    fun startPolling(context: Context, conversationId: Int) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(3000) // Poll every 3 seconds
                try {
                    _messages.value = NetworkManager.getMessages(context, conversationId)
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Polling error: ${e.message}")
                }
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
}