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

import io.ktor.client.request.forms.submitForm
import io.ktor.http.parameters


object NetworkManager {
    private const val BASE_URL = "http://192.168.254.102/hopfog_api/REST"
    //private const val BASE_URL = "http://26.166.235.63/hopfog_api/REST"


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

    suspend fun sendMessage(context: Context, conversationId: Int, messageText: String): SendMessageResponse? {
        return try {
            val response: HttpResponse = client.post("$BASE_URL/send_message.php") {
                contentType(ContentType.Application.Json)
                setBody(SendMessageRequest(conversationId = conversationId, messageText = messageText))
            }

            // Check if the HTTP status code indicates success (2xx)
            if (response.status.isSuccess()) {
                response.body<SendMessageResponse>()
            } else {
                // If the server returned an error code (like 429), try to parse the error body
                val errorBody = response.body<ErrorResponse>()
                context.toast(errorBody.error)
                null // Indicate failure
            }
        } catch (e: Exception) {
            Log.e("NetworkManager", "Exception in sendMessage: ${e.message}")
            context.toast("Network request failed.")
            null
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

    suspend fun changePassword(context: Context, oldPass: String, newPass: String): Boolean {
        return try {
            val response: HttpResponse = client.submitForm(
                url = "$BASE_URL/change_password.php",
                formParameters = Parameters.build {
                    append("old_password", oldPass)
                    append("new_password", newPass)
                }
            )

            if (response.status == HttpStatusCode.OK) {
                // Password changed successfully
                true
            } else {
                // --- THIS IS THE FIX ---
                // Decode the error using our new serializable class
                val errorResponse = response.body<GenericErrorResponse>()
                // Use the error message from the decoded response
                context.toast(errorResponse.error ?: "An unknown server error occurred.")
                false
            }
        } catch (e: Exception) {
            // This will catch serialization errors or network failures
            Log.e("NetworkManager", "Exception in changePassword: ${e.message}")
            e.printStackTrace()
            context.toast("Network request failed: ${e.message}")
            false
        }
    }

    suspend fun runMessageCleanup(context: Context): Boolean {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/delete_old_messages.php")
            // A 204 No Content response is a success.
            if (response.status == HttpStatusCode.NoContent) {
                true
            } else {
                Log.e("NetworkManager", "Cleanup failed with status: ${response.status}")
                false
            }
        } catch (e: Exception) {
            Log.e("NetworkManager", "Exception during message cleanup: ${e.message}")
            false
        }
    }


}