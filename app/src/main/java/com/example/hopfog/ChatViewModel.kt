package com.example.hopfog

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Holds the list of all chat conversations
    private val _conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    val conversations = _conversations.asStateFlow()

    // Holds the messages for the currently selected chat
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()

    // Holds the name of the person in the active chat
    private val _contactName = MutableStateFlow("")
    val contactName = _contactName.asStateFlow()

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
            // Clear old messages before loading new ones
            _messages.value = emptyList()
            _messages.value = NetworkManager.getMessages(context, conversationId)
        }
    }

    fun sendMessage(context: Context, conversationId: Int, messageText: String) {
        // User can't send empty msg
        if (messageText.isBlank()) return

        viewModelScope.launch {
            val success = NetworkManager.sendMessage(context, conversationId, messageText)
            if (success) {
                // If the message was sent successfully, refresh the message list
                _messages.value = NetworkManager.getMessages(context, conversationId)
            }
        }
    }
}