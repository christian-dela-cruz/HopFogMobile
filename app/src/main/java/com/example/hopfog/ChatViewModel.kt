package com.example.hopfog

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// The private RememberMeManager object has been completely removed.

class ChatViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    private val _conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    val conversations = _conversations.asStateFlow()
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages = _messages.asStateFlow()
    private val _contactName = MutableStateFlow("")
    val contactName = _contactName.asStateFlow()
    val connectionStatus = BleManager.status
    private val _connectedUsers = MutableStateFlow<List<String>>(emptyList())
    val connectedUsers = _connectedUsers.asStateFlow()

    @SuppressLint("StaticFieldLeak")
    private lateinit var appContext: Context

    fun setContext(context: Context) {
        appContext = context.applicationContext
    }

    fun loadConversations(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _conversations.value = NetworkManager.getConversations(context)
            _isLoading.value = false
        }
    }

    fun prepareChat(conversationId: Int, name: String) {
        _contactName.value = name
        _messages.value = NetworkManager.MockStore.getMessages(conversationId)
    }

    fun connectToHub() {
        if (BleManager.status.value is ConnectionStatus.Disconnected) {
            Log.d("ChatViewModel", "Requesting new BLE connection...")
            BleManager.connect()
        } else {
            Log.d("ChatViewModel", "Using existing BLE connection.")
        }
    }

    fun disconnectFromHub() {
        Log.d("ChatViewModel", "Requesting BLE disconnection...")
        BleManager.disconnect()
    }

    fun sendMessage(context: Context, messageText: String, recipientUsername: String) {
        if (messageText.isBlank()) return
        if (connectionStatus.value != ConnectionStatus.Ready) {
            Log.w("ChatViewModel", "Cannot send message, connection not ready.")
            return
        }

        addProvisionalMessage(messageText)

        // --- THIS IS THE FIX ---
        // Use the existing public SessionManager to get the current user's name
        val currentUsername = SessionManager.getUsername(context)
        val messageData = mapOf(
            "action" to "sendMessage",
            "sender_username" to currentUsername,
            "recipient_username" to recipientUsername,
            "text" to messageText
        )
        val jsonString = com.google.gson.Gson().toJson(messageData)

        BleManager.sendJson(jsonString)
    }

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
            val newTimestamp = if (newStatus == "failed") {
                "Failed"
            } else {
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            }
            val updatedMessage = messageToUpdate.copy(
                status = newStatus,
                timestamp = newTimestamp
            )
            updatedList[lastSendingIndex] = updatedMessage
            _messages.value = updatedList
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnectFromHub()
    }

    init {
        BleManager.incomingMessages.onEach { jsonString ->
            try {
                val json = org.json.JSONObject(jsonString)
                when (json.optString("action")) {

                    "userList" -> {
                        val usersArray = json.getJSONArray("users")
                        val userList = mutableListOf<String>()
                        for (i in 0 until usersArray.length()) {
                            userList.add(usersArray.getString(i))
                        }
                        _connectedUsers.value = userList
                    }

                    "sendMessage" -> {
                        val sender = json.getString("sender_username")
                        val recipient = json.getString("recipient_username")
                        val text = json.getString("text")

                        if (::appContext.isInitialized) {
                            // --- THIS IS THE FIX ---
                            // Use the existing public SessionManager
                            val currentUser = SessionManager.getUsername(appContext)

                            if (sender == currentUser) {
                                updateLastMessageStatus("delivered")
                            } else if (recipient == currentUser && sender == _contactName.value) {
                                addIncomingMessage(sender, text)
                            }
                        }
                    }

                    "login" -> {
                        Log.d("ChatViewModel", "Login status update received: ${json.optString("status")}")
                        requestUserList()
                    }

                    "error" -> {
                        val errorMessage = json.getString("message")
                        Log.e("ChatViewModel", "Received error from hub: $errorMessage")
                        if (errorMessage == "Recipient not connected") {
                            updateLastMessageStatus("failed")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error parsing incoming JSON", e)
            }
        }.launchIn(viewModelScope)
    }

    private fun addIncomingMessage(senderUsername: String, messageText: String) {
        val incomingMessage = Message(
            messageId = (_messages.value.lastOrNull()?.messageId ?: 0) + 1,
            senderUsername = senderUsername,
            messageText = messageText,
            timestamp = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()),
            isFromCurrentUser = false,
            status = "delivered"
        )
        _messages.value = _messages.value + incomingMessage
    }

    fun requestUserList() {
        if (connectionStatus.value == ConnectionStatus.Ready) {
            val request = mapOf("action" to "getUserList")
            val jsonString = com.google.gson.Gson().toJson(request)
            BleManager.sendJson(jsonString)
        }
    }
}