package com.example.hopfog

import android.content.Context

/**
 * A singleton object to manage all user session and preference data.
 */
object SessionManager {

    // Private Constants
    private const val PREFS_BASE_NAME = "HopFogPrefs_User_"
    private const val SESSION_PREFS_NAME = "HopFog_Session" // For storing who is logged in
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_HAS_AGREED_SOS = "hasAgreedToSos"

    // --- Session Management (Who is logged in?) ---

    fun saveSession(context: Context, userId: Int, username: String) {
        val editor = context.getSharedPreferences(SESSION_PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.putInt(KEY_USER_ID, userId)
        editor.putString(KEY_USERNAME, username)
        editor.apply()
    }

    fun clearSession(context: Context) {
        val editor = context.getSharedPreferences(SESSION_PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.clear()
        editor.apply()
    }

    fun getUserId(context: Context): Int {
        val prefs = context.getSharedPreferences(SESSION_PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_USER_ID, -1)
    }

    // --- User-Specific Preferences (SOS Agreement) ---

    /**
     * Checks if the CURRENT user has agreed to the SOS terms.
     */
    fun hasAgreedToSos(context: Context): Boolean {
        // --- THIS IS THE CORRECTED LINE ---
        val userId = getUserId(context) // Use the helper function to get the correct user ID
        // ---

        if (userId == -1) return false // No user logged in, so no agreement

        val userPrefsName = "$PREFS_BASE_NAME$userId"
        val prefs = context.getSharedPreferences(userPrefsName, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_HAS_AGREED_SOS, false)
    }

    /**
     * Saves the SOS agreement for the CURRENT user.
     */
    fun setHasAgreedToSos(context: Context, hasAgreed: Boolean) {
        val userId = getUserId(context)
        if (userId == -1) return // Cannot save agreement for a non-existent user

        val userPrefsName = "$PREFS_BASE_NAME$userId"
        val prefs = context.getSharedPreferences(userPrefsName, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean(KEY_HAS_AGREED_SOS, hasAgreed)
        editor.apply()
    }
}