package com.example.hopfog

import android.content.Context
import android.widget.Toast

/**
 * A simple extension function to make showing Toasts easier.
 * Now you can just call `context.toast("My message")`.
 */
fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}