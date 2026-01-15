package com.example.hopfog

import android.content.Context
import android.widget.Toast
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.json.JSONObject


import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import io.ktor.client.call.body
import android.util.Log

object NetworkManager {
    private const val BASE_URL = "http://192.168.254.102/hopfog_api/REST"

    private val client = HttpClient(CIO){
        // For automatically handling JSON
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true // Important for flexibility
            })
        }
        // For automatically handling session cookies
        install(HttpCookies)
    }

    private fun Context.toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    suspend fun registerUser(context: Context, username: String, email: String, password: String): Boolean {
        return try {
            val response: HttpResponse = client.post("$BASE_URL/register.php") {
                setBody(FormDataContent(Parameters.build {
                    append("username", username)
                    append("email", email)
                    append("password", password)
                }))
            }
            val body = response.bodyAsText()
            val jsonObject = JSONObject(body)
            val success = jsonObject.optBoolean("success", false)
            val message = jsonObject.optString("message", "An unknown error occurred.")

            context.toast(message)
            success
        } catch (e: Exception) {
            e.printStackTrace()
            context.toast("Network error: ${e.message}")
            false
        }
    }

    suspend fun loginUser(context: Context, username: String, password: String): JSONObject? {
        return try {
            val response: HttpResponse = client.post("$BASE_URL/login.php") {
                setBody(FormDataContent(Parameters.build {
                    append("username", username)
                    append("password", password)
                }))
            }
            val body = response.bodyAsText()
            val jsonObject = JSONObject(body)
            val success = jsonObject.optBoolean("success", false)

            if (success) {
                jsonObject // Return the whole object on success
            } else {
                val message = jsonObject.optString("message", "An unknown error occurred.")
                context.toast(message)
                null // Return null on failure
            }
        } catch (e: Exception) {
            e.printStackTrace()
            context.toast("Network error: ${e.message}")
            null
        }
    }

    suspend fun getConversations(context: Context): List<ChatConversation> {
        return try {
            // --- REVERT THIS BACK ---
            // It will now work because the PHP script is fixed
            client.get("$BASE_URL/get_conversations.php").body()
        } catch (e: Exception) {
            e.printStackTrace()
            context.toast("Error fetching chats: ${e.message}")
            emptyList()
        }
    }

    suspend fun getMessages(context: Context, conversationId: Int): List<Message> {
        return try {
            client.get("$BASE_URL/get_messages.php") {
                parameter("conversation_id", conversationId)
            }.body()
        } catch (e: Exception) {
            e.printStackTrace()
            context.toast("Error fetching messages: ${e.message}")
            emptyList()
        }
    }

    suspend fun sendMessage(context: Context, conversationId: Int, messageText: String): Boolean {
        return try {
            val response: HttpResponse = client.post("$BASE_URL/send_message.php") {
                contentType(ContentType.Application.Json)
                // --- THIS IS THE FIX ---
                // Create an instance of our new request class
                val requestBody = SendMessageRequest(
                    conversationId = conversationId,
                    messageText = messageText
                )
                // Set the body using the typed object
                setBody(requestBody)
            }
            response.status == HttpStatusCode.Created
        } catch (e: Exception) {
            // It's helpful to log the specific error here too
            Log.e("NetworkManager", "Error sending message: ${e.message}", e)
            context.toast("Error sending message: ${e.message}")
            false
        }
    }

    suspend fun findOrCreateSosChat(context: Context): SosChatResponse? {
        return try {
            val response: HttpResponse = client.post("$BASE_URL/create_sos_chat.php")

            if (response.status == HttpStatusCode.OK) {
                response.body<SosChatResponse>()
            } else {
                val errorBody = response.bodyAsText()
                Log.e("NetworkManager", "Failed to create SOS chat: $errorBody")
                context.toast("Failed to start SOS chat. See logs.")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            context.toast("Network error starting SOS chat: ${e.message}")
            null
        }
    }

}