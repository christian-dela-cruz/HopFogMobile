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

object NetworkManager {
    private const val BASE_URL = "http://192.168.254.102/hopfog_api/REST"

    // Create a reusable HttpClient
    private val client = HttpClient(CIO)

    // A simple extension function for showing toasts
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
}