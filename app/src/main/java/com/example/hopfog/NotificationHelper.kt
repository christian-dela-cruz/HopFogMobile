package com.example.hopfog

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val SERVICE_CHANNEL_ID = "MessageCheckServiceChannel"
        const val MESSAGE_CHANNEL_ID = "NewMessageChannel"
        const val NOTIFICATION_PREFS = "HopFog_Notification_Prefs"
        const val KEY_SHOW_NOTIFICATIONS = "show_notifications"
        const val KEY_SOUND = "sound"
        const val KEY_VIBRATE = "vibrate"
    }

    // Create the notification channels (required for Android 8.0+)
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                "HopFog Background Service",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for the persistent HopFog service notification"
            }

            val messageChannel = NotificationChannel(
                MESSAGE_CHANNEL_ID,
                "New Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for new incoming message alerts"
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(messageChannel)
        }
    }

    // Build the persistent notification for the Foreground Service
    fun getServiceNotification(): Notification {
        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setContentTitle("HopFog Active")
            .setContentText("Checking for new messages in the background.")
            // You must have an icon file in your drawable resources
            .setSmallIcon(R.drawable.ic_notification) // IMPORTANT: Create this icon!
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    // Show a notification for a new message
    fun showNewMessageNotification(sender: String, message: String) {
        // Check user notification preferences
        val prefs = context.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE)
        val showNotifications = prefs.getBoolean(KEY_SHOW_NOTIFICATIONS, true)
        if (!showNotifications) return

        val sound = prefs.getBoolean(KEY_SOUND, true)
        val vibrate = prefs.getBoolean(KEY_VIBRATE, true)

        val builder = NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
            .setContentTitle("New Message from $sender")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification) // IMPORTANT: Create this icon!
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Dismisses the notification when clicked

        if (!sound) {
            builder.setSilent(true)
        }
        if (!vibrate) {
            builder.setVibrate(null)
        }

        // Use a unique ID for each notification to show multiple, or a fixed one to update
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}