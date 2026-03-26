package com.example.hopfog.data

import android.content.Context
import android.util.Log
import com.example.hopfog.NetworkManager
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val dao: MessageDao
) {
    fun getConversation(userId: Int, otherUserId: Int): Flow<List<MessageEntity>> =
        dao.getConversation(userId, otherUserId)

    suspend fun insertAll(messages: List<MessageEntity>) {
        dao.insertAll(messages)
    }

    suspend fun syncConversation(context: Context, userId: Int, otherUserId: Int) {
        try {
            val response = NetworkManager.getConversationHistory(context, otherUserId, userId)
            if (response.isNotEmpty()) {
                dao.insertAll(response)
            }
        } catch (e: Exception) {
            // Network error — fall back to cached data
            Log.w("ChatRepository", "Sync failed, using cached data: ${e.message}")
        }
    }
}
