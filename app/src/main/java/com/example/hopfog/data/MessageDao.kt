package com.example.hopfog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("""
        SELECT * FROM messages
        WHERE (sender_id = :userId AND receiver_id = :otherUserId)
           OR (sender_id = :otherUserId AND receiver_id = :userId)
        ORDER BY sent_at ASC
    """)
    fun getConversation(userId: Int, otherUserId: Int): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("""
        SELECT MAX(id) FROM messages
        WHERE (sender_id = :userId AND receiver_id = :otherUserId)
           OR (sender_id = :otherUserId AND receiver_id = :userId)
    """)
    suspend fun getMaxMessageId(userId: Int, otherUserId: Int): Int?

    @Query("""
        DELETE FROM messages
        WHERE (sender_id = :userId AND receiver_id = :otherUserId)
           OR (sender_id = :otherUserId AND receiver_id = :userId)
    """)
    suspend fun deleteConversation(userId: Int, otherUserId: Int)
}
