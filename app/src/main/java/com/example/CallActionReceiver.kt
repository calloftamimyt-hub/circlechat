package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val roomId = intent.getStringExtra("roomId") ?: return
        val callerUid = intent.getStringExtra("callerUid") ?: return
        
        // Cancel the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(roomId.hashCode())

        when (action) {
            "ACCEPT_CALL" -> {
                Log.d("CallActionReceiver", "Accepting call $roomId")
                // Start MainActivity with the accept flag
                val acceptIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("incoming_call", true)
                    putExtra("accept_call", true)
                    putExtra("callerUid", callerUid)
                    putExtra("roomId", roomId)
                    putExtra("callType", intent.getStringExtra("callType"))
                    putExtra("callerName", intent.getStringExtra("callerName"))
                }
                context.startActivity(acceptIntent)
            }
            "DECLINE_CALL" -> {
                Log.d("CallActionReceiver", "Declining call $roomId")
                // Run decline logic in the background using FirebaseManager
                GlobalScope.launch(Dispatchers.IO) {
                    com.example.data.firebase.FirebaseManager.endCall(callerUid, roomId)
                }
            }
        }
    }
}
