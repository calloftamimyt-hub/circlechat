package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatarUrl: String,
    val status: String, // "Online", "Offline", "Away"
    val lastActive: String,
    val bio: String,
    val firebaseUid: String = ""
) : Serializable

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long,
    val content: String,
    val timestamp: Long,
    val isFromMe: Boolean,
    val status: String = "Read", // "Sent", "Delivered", "Read"
    val type: String = "Text", // "Text", "AudioCall", "VideoCall"
    val durationSeconds: Int = 0, // Used for completed call logs inserted as system messages
    val firebaseId: String = "",
    val senderUid: String = ""
) : Serializable

@Entity(tableName = "call_logs")
data class CallLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long,
    val direction: String, // "Incoming", "Outgoing", "Missed"
    val type: String, // "Audio", "Video"
    val timestamp: Long,
    val durationSeconds: Int = 0
) : Serializable
