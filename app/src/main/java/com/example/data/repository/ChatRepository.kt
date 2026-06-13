package com.example.data.repository

import android.content.Context
import androidx.room.Room
import com.example.data.local.ChatDatabase
import com.example.data.model.CallLog
import com.example.data.model.Contact
import com.example.data.model.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatRepository(context: Context) {

    private val db: ChatDatabase = Room.databaseBuilder(
        context.applicationContext,
        ChatDatabase::class.java,
        "convo_chat_database"
    ).fallbackToDestructiveMigration().build()

    private val contactDao = db.contactDao()
    private val messageDao = db.messageDao()
    private val callLogDao = db.callLogDao()

    val allContacts: Flow<List<Contact>> = contactDao.getAllContacts()
    val allCallLogs: Flow<List<CallLog>> = callLogDao.getAllCallLogs()

    fun getMessagesForContact(contactId: Long): Flow<List<Message>> {
        return messageDao.getMessagesForContact(contactId)
    }

    fun getLatestMessage(contactId: Long): Flow<Message?> {
        return messageDao.getLatestMessageForContact(contactId)
    }

    fun getContactById(contactId: Long): Flow<Contact?> {
        return contactDao.getContactById(contactId)
    }

    suspend fun getContactByFirebaseUid(firebaseUid: String): Contact? = withContext(Dispatchers.IO) {
        contactDao.getContactByFirebaseUid(firebaseUid)
    }

    suspend fun getMessageByFirebaseId(firebaseId: String): Message? = withContext(Dispatchers.IO) {
        messageDao.getMessageByFirebaseId(firebaseId)
    }

    suspend fun insertMessage(message: Message): Long = withContext(Dispatchers.IO) {
        messageDao.insertMessage(message)
    }

    suspend fun insertCallLog(callLog: CallLog): Long = withContext(Dispatchers.IO) {
        callLogDao.insertCallLog(callLog)
    }

    suspend fun insertContact(contact: Contact): Long = withContext(Dispatchers.IO) {
        contactDao.insertContact(contact)
    }

    suspend fun deleteContact(contactId: Long) = withContext(Dispatchers.IO) {
        contactDao.deleteContactById(contactId)
        messageDao.deleteMessagesForContact(contactId)
    }

    suspend fun clearAllLogs() = withContext(Dispatchers.IO) {
        callLogDao.deleteAllCallLogs()
    }

    // Seeds beautiful, premium data if currently empty
    fun preseedDatabaseIfEmpty(scope: CoroutineScope) {
        // Removed demo data generation as per user request
    }
}
