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
    //private const val BASE_URL = "http://172.18.7.182/hopfog_api/REST"


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
                // --- THIS IS THE NEW LOGIC ---
                // 1. Get the 'user' object from the response
                val userJson = jsonObject.getJSONObject("user")

                // 2. Extract all the user data
                val userId = userJson.getInt("user_id")
                val userUsername = userJson.getString("username")
                // 3. Get the new boolean flag (default to false if not found)
                val hasAgreed = userJson.optBoolean("has_agreed_sos", false)

                // 4. Save EVERYTHING to the SessionManager
                SessionManager.saveSession(context, userId, userUsername, hasAgreed)
                // --- END OF NEW LOGIC ---

                jsonObject // Return the whole object to the UI to signal success
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


    suspend fun agreeToSos(context: Context): Boolean {
        // This function will return 'true' on success and 'false' on failure.
        return try {
            val userId = SessionManager.getUserId(context)
            if (userId == -1) {
                // Cannot agree if no user is logged in.
                return false
            }

            // Make the POST request to your new PHP script.
            val response: HttpResponse = client.post("$BASE_URL/agree_to_sos.php") {
                contentType(ContentType.Application.Json) // Let the server know we're sending JSON
                setBody(mapOf("user_id" to userId)) // Send the user_id in the body
            }

            // Check the response from the server.
            if (response.status == HttpStatusCode.OK) {
                val body = response.bodyAsText()
                val jsonObject = JSONObject(body)
                val success = jsonObject.optBoolean("success", false)

                if (success) {
                    // If the database was updated, also update our local session to match.
                    SessionManager.setHasAgreedToSos(context, true)
                }
                success // Return the success status (true or false).
            } else {
                // The server returned an error (e.g., 404, 500).
                context.toast("Server error: ${response.status.value}")
                false
            }
        } catch (e: Exception) {
            // A network error occurred.
            e.printStackTrace()
            context.toast("Network error agreeing to SOS: ${e.message}")
            false
        }
    }


    suspend fun getNewMessages(context: Context, lastMessageId: Int): List<Message> {
        return try {

            val currentUserId = SessionManager.getUserId(context)
            if (currentUserId == -1) {
                return emptyList()
            }

            client.get("$BASE_URL/get_new_messages.php") {
                parameter("last_message_id", lastMessageId)
                parameter("user_id", currentUserId)
            }.body()
        } catch (e: Exception) {
            Log.e("NetworkManager", "Error checking for new messages: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getAllUsers(context: Context): List<SelectableUser> {
        return try {
            val currentUserId = SessionManager.getUserId(context)
            if (currentUserId == -1) return emptyList()

            client.get("$BASE_URL/get_all_users.php") {
                parameter("user_id", currentUserId)
            }.body()
        } catch (e: Exception) {
            Log.e("NetworkManager", "Error getting all users: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun findOrCreateChatWithUser(context: Context, otherUserId: Int): SosChatResponse? { // <-- FIX IS HERE
        return try {
            val currentUserId = SessionManager.getUserId(context)
            if (currentUserId == -1) return null

            client.post("$BASE_URL/find_or_create_chat.php") {
                setBody(FormDataContent(Parameters.build {
                    append("user_id_1", currentUserId.toString())
                    append("user_id_2", otherUserId.toString())
                }))
            }.body()
        } catch (e: Exception) {
            Log.e("NetworkManager", "Error finding/creating chat: ${e.message}")
            e.printStackTrace()
            null
        }
    }

}