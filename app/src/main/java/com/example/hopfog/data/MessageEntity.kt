package com.example.hopfog.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: Int,

    @SerialName("sender_id")
    @ColumnInfo(name = "sender_id")
    val senderId: Int,

    @SerialName("receiver_id")
    @ColumnInfo(name = "receiver_id")
    val receiverId: Int,

    val content: String,

    @SerialName("sent_at")
    @ColumnInfo(name = "sent_at")
    val sentAt: Long
)
