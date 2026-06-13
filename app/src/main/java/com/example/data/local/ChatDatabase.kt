package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import com.example.data.model.Contact
import com.example.data.model.Message
import com.example.data.model.CallLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    fun getContactById(id: Long): Flow<Contact?>

    @Query("SELECT * FROM contacts WHERE firebaseUid = :firebaseUid LIMIT 1")
    suspend fun getContactByFirebaseUid(firebaseUid: String): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<Contact>)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContactById(id: Long)

    @Query("DELETE FROM contacts")
    suspend fun deleteAllContacts()
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE contactId = :contactId ORDER BY timestamp ASC")
    fun getMessagesForContact(contactId: Long): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE contactId = :contactId ORDER BY timestamp DESC LIMIT 1")
    fun getLatestMessageForContact(contactId: Long): Flow<Message?>

    @Query("SELECT * FROM messages WHERE firebaseId = :firebaseId LIMIT 1")
    suspend fun getMessageByFirebaseId(firebaseId: String): Message?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)

    @Query("DELETE FROM messages WHERE contactId = :contactId")
    suspend fun deleteMessagesForContact(contactId: Long)
}

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCallLogs(): Flow<List<CallLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(callLog: CallLog): Long

    @Query("DELETE FROM call_logs WHERE id = :id")
    suspend fun deleteCallLogById(id: Long)

    @Query("DELETE FROM call_logs")
    suspend fun deleteAllCallLogs()
}

@Database(entities = [Contact::class, Message::class, CallLog::class], version = 2, exportSchema = false)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun messageDao(): MessageDao
    abstract fun callLogDao(): CallLogDao
}
