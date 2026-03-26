package com.example.hopfog

import androidx.lifecycle.ViewModel
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

/**
 * A ViewModel to hold and manage the state of the currently logged-in user.
 * This allows user data to be shared across different screens of the app.
 */
class UserViewModel : ViewModel() {

    // This is the private, mutable state. Only the ViewModel can change it.
    // It holds the current User object, or null if no one is logged in.
    private val _user = MutableStateFlow<User?>(null)

    // This is the public, read-only state flow that the UI will observe.
    // The UI can collect updates from this flow but cannot change it directly.
    val user: StateFlow<User?> = _user

    /**
     * This function is called from the LoginPage upon a successful login.
     * It takes the JSON response from the server, parses it, creates a User object,
     * and updates the state.
     *
     * @param userJson The full JSONObject received from the login API call.
     */
    fun onLoginSuccess(userJson: JSONObject) {
        // According to your PHP script, the user's details are nested inside a "user" object.
        val userObject = userJson.getJSONObject("user")

        // Create a new User object from the parsed JSON data.
        val loggedInUser = User(
            id = userObject.getInt("user_id"),
            username = userObject.getString("username"),
            email = userObject.getString("email")
        )

        // Update the state flow's value. Any screen observing this flow will now be recomposed.
        _user.value = loggedInUser
    }

    /**
     * Restores user state from SessionManager when the app is reopened
     * without going through the login flow.
     */
    fun restoreFromSession(context: Context) {
        val userId = SessionManager.getUserId(context)
        if (userId != -1 && _user.value == null) {
            _user.value = User(
                id = userId,
                username = SessionManager.getUsername(context),
                email = SessionManager.getEmail(context)
            )
        }
    }

    /**
     * This function is called when the user logs out.
     * It resets the user state to null, effectively clearing the session.
     */
    fun onLogout() {
        _user.value = null
    }
}