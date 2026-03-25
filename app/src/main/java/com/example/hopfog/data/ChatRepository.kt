package com.example.hopfog.data

import android.content.Context
import com.example.hopfog.NetworkManager
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val dao: MessageDao
) {
    fun getConversation(userId: Int, otherUserId: Int): Flow<List<MessageEntity>> =
        dao.getConversation(userId, otherUserId)

    suspend fun syncConversation(context: Context, userId: Int, otherUserId: Int) {
        try {
            val response = NetworkManager.getConversationHistory(context, otherUserId, userId)
            if (response.isNotEmpty()) {
                dao.insertAll(response)
            }
        } catch (_: Exception) {
            // Network error — fall back to cached data
        }
    }
}
