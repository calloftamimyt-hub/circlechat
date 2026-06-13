package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.firebase.FirebaseManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            // Fallback for notification payload
            sendNotification(it.title ?: "Convo", it.body ?: "", "")
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"]
        when (type) {
            "call" -> {
                // Incoming call data
                val callerName = data["callerName"] ?: "Unknown"
                val callType = data["callType"] ?: "Audio"
                val roomId = data["roomId"] ?: ""
                val callerUid = data["callerUid"] ?: ""
                sendFullScreenNotification(callerName, callType, roomId, callerUid)
            }
            "message" -> {
                val senderName = data["senderName"] ?: "Someone"
                val text = data["text"] ?: ""
                val senderUid = data["senderUid"] ?: ""
                
                // Smart Chat Notification Logic
                if (FirebaseManager.activeChatFirebaseUid == senderUid) {
                    // We are currently in the chat so skip notification
                    Log.d(TAG, "Skipping notification as user is in the chat")
                    return
                }
                
                sendNotification(senderName, text, senderUid)
            }
        }
    }

    private fun sendRegistrationToServer(token: String?) {
        // Handled by FirebaseManager when user signs up/in
    }

    private fun sendNotification(title: String, messageBody: String, senderUid: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("chat_firebase_uid", senderUid)
        }
        val pendingIntent = PendingIntent.getActivity(this, senderUid.hashCode() /* Request code */, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val channelId = "convo_messages"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(senderUid.hashCode() /* ID of notification */, notificationBuilder.build())
    }

    private fun sendFullScreenNotification(callerName: String, callType: String, roomId: String, callerUid: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("incoming_call", true)
            putExtra("callerName", callerName)
            putExtra("callType", callType)
            putExtra("roomId", roomId)
            putExtra("callerUid", callerUid)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, roomId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Accept intent
        val acceptIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("incoming_call", true)
            putExtra("accept_call", true)
            putExtra("roomId", roomId)
            putExtra("callerUid", callerUid)
            putExtra("callType", callType)
            putExtra("callerName", callerName)
        }
        val acceptPendingIntent = PendingIntent.getActivity(this, roomId.hashCode() + 1, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Decline intent
        val declineIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = "DECLINE_CALL"
            putExtra("roomId", roomId)
            putExtra("callerUid", callerUid)
        }
        val declinePendingIntent = PendingIntent.getBroadcast(this, 2, declineIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val channelId = "convo_calls"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Incoming $callType call")
            .setContentText("$callerName is calling...")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_call, "Accept", acceptPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Decline", declinePendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                enableVibration(true)
                // Set ringtone
                var ringtoneUri: android.net.Uri? = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                setSound(ringtoneUri, android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(roomId.hashCode() /* ID of notification */, notificationBuilder.build())
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}
