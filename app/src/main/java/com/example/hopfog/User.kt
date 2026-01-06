package com.example.hopfog

/**
 * A simple data class to represent the logged-in user.
 * This structure holds the user's information that we get from the server.
 */
data class User(
    val id: Int,
    val username: String,
    val email: String
)