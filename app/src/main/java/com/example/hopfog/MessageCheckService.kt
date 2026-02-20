package com.example.hopfog

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.*
import java.util.Timer
import java.util.TimerTask

class MessageCheckService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private var timer: Timer? = null
    private lateinit var notificationHelper: NotificationHelper
    private var lastCheckedMessageId: Int = 0 // To track the last message we've seen

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notificationHelper.getServiceNotification()
        startForeground(1, notification) // Start the service in the foreground

        // Start a timer that runs every 15 seconds
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                scope.launch {
                    checkForNewMessages()
                }
            }
        }, 0, 15000) // 0 delay, 15000ms (15 sec) interval

        return START_STICKY // If the service is killed, restart it
    }

    private suspend fun checkForNewMessages() {
        // Don't check if no user is logged in
        val userId = SessionManager.getUserId(this)
        if (userId == -1) return

        val newMessages = NetworkManager.getNewMessages(this, lastCheckedMessageId)

        for (message in newMessages) {
            notificationHelper.showNewMessageNotification(message.senderUsername, message.messageText)
            // Update the last seen message ID
            if (message.messageId > lastCheckedMessageId) {
                lastCheckedMessageId = message.messageId
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // We don't need binding
    }
}