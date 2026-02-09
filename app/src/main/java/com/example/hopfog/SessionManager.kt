package com.example.hopfog

import android.content.Context

object SessionManager {
    private const val PREFS_NAME = "HopFog_User_Session"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_HAS_AGREED_SOS = "has_agreed_sos"

    // Saves ALL user data on login
    fun saveSession(context: Context, userId: Int, username: String, hasAgreedSos: Boolean) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.putInt(KEY_USER_ID, userId)
        editor.putString(KEY_USERNAME, username)
        editor.putBoolean(KEY_HAS_AGREED_SOS, hasAgreedSos)
        editor.apply()
    }

    // Clears all data on logout
    fun clearSession(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    // Gets the user's ID
    fun getUserId(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_USER_ID, -1)
    }

    // Gets the locally stored username (empty if none)
    fun getUsername(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_USERNAME, "") ?: ""
    }

    // Checks the locally stored agreement flag
    fun hasAgreedToSos(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_HAS_AGREED_SOS, false)
    }

    // Manually updates the local flag after the user agrees
    fun setHasAgreedToSos(context: Context, hasAgreed: Boolean) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.putBoolean(KEY_HAS_AGREED_SOS, hasAgreed)
        editor.apply()
    }
}