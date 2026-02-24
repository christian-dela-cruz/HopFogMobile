package com.example.hopfog

import android.content.Context
import android.widget.Toast
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.net.ConnectException

object NetworkManager {

    private const val BASE_URL = "http://hopfog.com"
    private const val CONNECTION_ERROR_MSG = "Cannot reach HopFog server. Make sure you're connected to HopFog-Network WiFi."

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    private fun Context.toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    suspend fun registerUser(context: Context, username: String, email: String, password: String): Boolean {
        context.toast("Registration is handled at the Data Center")
        return false
    }

    suspend fun loginUser(context: Context, usernameOrEmail: String, password: String): JSONObject? {
        return try {
            val body = JSONObject().apply {
                put("username", usernameOrEmail)
                put("password", password)
            }.toString()
            val response: HttpResponse = client.post("$BASE_URL/login") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            val json = JSONObject(response.bodyAsText())
            if (json.optBoolean("success", false)) {
                val userObj = json.getJSONObject("user")
                SessionManager.saveSession(
                    context = context,
                    userId = userObj.getInt("user_id"),
                    username = userObj.getString("username"),
                    hasAgreedSos = userObj.optBoolean("has_agreed_sos", false)
                )
                json
            } else {
                context.toast("Login failed: ${json.optString("message", "Invalid credentials")}")
                null
            }
        } catch (e: ConnectException) {
            context.toast(CONNECTION_ERROR_MSG)
            null
        } catch (e: Exception) {
            context.toast("Network error: ${e.message}")
            null
        }
    }

    suspend fun getConversations(context: Context): List<ChatConversation> {
        return try {
            val userId = SessionManager.getUserId(context)
            client.get("$BASE_URL/conversations") {
                parameter("user_id", userId)
            }.body()
        } catch (e: ConnectException) {
            context.toast(CONNECTION_ERROR_MSG)
            emptyList()
        } catch (e: Exception) {
            context.toast("Failed to load conversations: ${e.message}")
            emptyList()
        }
    }

    suspend fun getMessages(context: Context, conversationId: Int): List<Message> {
        return try {
            val userId = SessionManager.getUserId(context)
            client.get("$BASE_URL/messages") {
                parameter("conversation_id", conversationId)
                parameter("user_id", userId)
            }.body()
        } catch (e: ConnectException) {
            context.toast(CONNECTION_ERROR_MSG)
            emptyList()
        } catch (e: Exception) {
            context.toast("Failed to load messages: ${e.message}")
            emptyList()
        }
    }

    suspend fun sendMessage(context: Context, conversationId: Int, messageText: String): SendMessageResponse? {
        return try {
            val senderId = SessionManager.getUserId(context)
            val body = JSONObject().apply {
                put("conversation_id", conversationId)
                put("sender_id", senderId)
                put("message_text", messageText)
            }.toString()
            client.post("$BASE_URL/send") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()
        } catch (e: ConnectException) {
            context.toast(CONNECTION_ERROR_MSG)
            null
        } catch (e: Exception) {
            context.toast("Failed to send message: ${e.message}")
            null
        }
    }

    suspend fun getAllUsers(context: Context): List<SelectableUser> {
        return try {
            val userId = SessionManager.getUserId(context)
            client.get("$BASE_URL/users") {
                parameter("user_id", userId)
            }.body()
        } catch (e: ConnectException) {
            context.toast(CONNECTION_ERROR_MSG)
            emptyList()
        } catch (e: Exception) {
            context.toast("Failed to load users: ${e.message}")
            emptyList()
        }
    }

    suspend fun findOrCreateChatWithUser(context: Context, otherUserId: Int): SosChatResponse? {
        return try {
            val userId = SessionManager.getUserId(context)
            val body = JSONObject().apply {
                put("user1_id", userId)
                put("user2_id", otherUserId)
            }.toString()
            client.post("$BASE_URL/create-chat") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()
        } catch (e: ConnectException) {
            context.toast(CONNECTION_ERROR_MSG)
            null
        } catch (e: Exception) {
            context.toast("Failed to create chat: ${e.message}")
            null
        }
    }

    suspend fun findOrCreateSosChat(context: Context): SosChatResponse? {
        return try {
            val userId = SessionManager.getUserId(context)
            val body = JSONObject().apply {
                put("user_id", userId)
            }.toString()
            client.post("$BASE_URL/sos") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()
        } catch (e: ConnectException) {
            context.toast(CONNECTION_ERROR_MSG)
            null
        } catch (e: Exception) {
            context.toast("Failed to create SOS chat: ${e.message}")
            null
        }
    }

    suspend fun getNewMessages(context: Context, lastMessageId: Int): List<Message> {
        return try {
            val userId = SessionManager.getUserId(context)
            client.get("$BASE_URL/new-messages") {
                parameter("last_id", lastMessageId)
                parameter("user_id", userId)
            }.body()
        } catch (e: ConnectException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun agreeToSos(context: Context): Boolean {
        return try {
            val userId = SessionManager.getUserId(context)
            val body = JSONObject().apply {
                put("user_id", userId)
            }.toString()
            val response: HttpResponse = client.post("$BASE_URL/agree-sos") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            val json = JSONObject(response.bodyAsText())
            val success = json.optBoolean("success", false)
            if (success) {
                SessionManager.setHasAgreedToSos(context, true)
            } else {
                context.toast("Failed to save SOS agreement")
            }
            success
        } catch (e: ConnectException) {
            context.toast(CONNECTION_ERROR_MSG)
            false
        } catch (e: Exception) {
            context.toast("Failed to agree to SOS: ${e.message}")
            false
        }
    }

    suspend fun changePassword(context: Context, oldPass: String, newPass: String): Boolean {
        return try {
            val userId = SessionManager.getUserId(context)
            val body = JSONObject().apply {
                put("user_id", userId)
                put("old_password", oldPass)
                put("new_password", newPass)
            }.toString()
            val response: HttpResponse = client.post("$BASE_URL/change-password") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            val json = JSONObject(response.bodyAsText())
            val success = json.optBoolean("success", false)
            if (success) {
                context.toast("Password updated successfully")
            } else {
                context.toast("Failed to change password: ${json.optString("message", "Unknown error")}")
            }
            success
        } catch (e: ConnectException) {
            context.toast(CONNECTION_ERROR_MSG)
            false
        } catch (e: Exception) {
            context.toast("Network error: ${e.message}")
            false
        }
    }

    suspend fun runMessageCleanup(context: Context): Boolean {
        return true
    }

    suspend fun checkStatus(): Boolean {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/status")
            val json = JSONObject(response.bodyAsText())
            json.optBoolean("online", false)
        } catch (e: Exception) {
            false
        }
    }
}
