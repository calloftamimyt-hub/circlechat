package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.model.Contact
import com.example.data.model.Message
import com.example.data.repository.ChatRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class FirestoreUser(
    val uid: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val status: String = "Online",
    val lastActive: String = "Now",
    val bio: String = ""
)

data class FirestoreMessage(
    val id: String = "",
    val senderUid: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val type: String = "Text",
    val durationSeconds: Int = 0
)

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    
    var activeChatFirebaseUid: String? = null

    var isFirebaseAvailable: Boolean = false
        private set

    fun initialize(context: Context) {
        try {
            // First check if already initialized by automatic provider
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                isFirebaseAvailable = true
                Log.d(TAG, "Firebase already initialized by system context.")
                return
            }
            
            // Try automatic init
            FirebaseApp.initializeApp(context)
            isFirebaseAvailable = true
            Log.d(TAG, "Firebase initialized successfully manually.")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase SDK configuration missing or incomplete. Fallback to high-end offline mode active.", e)
            isFirebaseAvailable = false
        }
    }

    fun getAuth(): FirebaseAuth? {
        return if (isFirebaseAvailable) {
            try {
                FirebaseAuth.getInstance()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    fun getFirestore(): FirebaseFirestore? {
        return if (isFirebaseAvailable) {
            try {
                FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    // Sign in anonymously for easy live chatting between two real users
    suspend fun signInAnonymously(): String? = withContext(Dispatchers.IO) {
        val auth = getAuth() ?: return@withContext null
        try {
            val result = auth.signInAnonymously().await()
            val user = result.user
            Log.d(TAG, "Signed in anonymously to Firebase: ${user?.uid}")
            user?.uid
        } catch (e: Exception) {
            Log.e(TAG, "Error in Firebase Anonymous sign in", e)
            null
        }
    }

    // Sign Up with Phone and Password
    suspend fun signUpWithPhoneAndPassword(phone: String, password: String): String? = withContext(Dispatchers.IO) {
        val auth = getAuth() ?: return@withContext null
        try {
            val email = "$phone@dummy.app" // Dummy wrapping to use Email/Pass Auth under the hood
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            Log.d(TAG, "Signed up with phone: ${user?.uid}")
            user?.uid
        } catch (e: Exception) {
            Log.e(TAG, "Error in Sign Up", e)
            null
        }
    }

    // Login with Phone and Password
    suspend fun signInWithPhoneAndPassword(phone: String, password: String): String? = withContext(Dispatchers.IO) {
        val auth = getAuth() ?: return@withContext null
        try {
            val email = "$phone@dummy.app"
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            Log.d(TAG, "Signed in with phone: ${user?.uid}")
            user?.uid
        } catch (e: Exception) {
            Log.e(TAG, "Error in Sign In", e)
            null
        }
    }

    // Save profile to Firestore (Including Phone)
    suspend fun saveCurrentUserProfile(phone: String) = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext
        val auth = getAuth() ?: return@withContext
        val uid = auth.currentUser?.uid ?: return@withContext
        
        var fcmToken = ""
        try {
            fcmToken = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
        } catch(e: Exception) {
            Log.e(TAG, "Error fetching FCM token", e)
        }

        val userDoc = hashMapOf(
            "uid" to uid,
            "phone" to phone,
            "name" to phone, // default name to phone
            "avatarUrl" to "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
            "status" to "Online",
            "lastActive" to "Now",
            "fcmToken" to fcmToken,
            "bio" to "Available"
        )

        try {
            firestore.collection("users").document(uid).set(userDoc).await()
            Log.d(TAG, "Created/Updated profile for $uid in Firestore.")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving profile", e)
        }
    }

    // Search and Connect friend via Phone Number
    suspend fun connectFriendByPhone(phone: String, repository: ChatRepository): Contact? = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext null
        val myUid = getAuth()?.currentUser?.uid ?: return@withContext null

        try {
            // Find friend's profile document by phone
            val usersRef = firestore.collection("users")
            val querySnapshot = usersRef.whereEqualTo("phone", phone).limit(1).get().await()
            
            if (querySnapshot.isEmpty) {
                Log.w(TAG, "No user found with phone: $phone")
                return@withContext null
            }

            val doc = querySnapshot.documents[0]
            val friendUid = doc.getString("uid") ?: return@withContext null
            
            val name = doc.getString("name") ?: phone
            val avatarUrl = doc.getString("avatarUrl") ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
            val status = doc.getString("status") ?: "Online"
            val lastActive = doc.getString("lastActive") ?: "Now"
            val bio = doc.getString("bio") ?: "Connected via Firebase Auth"

            // Insert locally to Room
            val contact = Contact(
                name = name,
                avatarUrl = avatarUrl,
                status = status,
                lastActive = lastActive,
                bio = bio,
                firebaseUid = friendUid
            )

            // Let's check if contact already exists
            val existing = repository.getContactByFirebaseUid(friendUid)
            val finalId = if (existing != null) {
                existing.id
            } else {
                repository.insertContact(contact)
            }

            // Save friend relation in Firestore
            val relationDoc = hashMapOf("connectedAt" to System.currentTimeMillis())
            firestore.collection("users").document(myUid)
                .collection("contacts").document(friendUid)
                .set(relationDoc).await()

            contact.copy(id = finalId)
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting friend by UID", e)
            null
        }
    }

    // Send a message to Firestore & Local database
    suspend fun sendLiveMessage(
        friendFirebaseUid: String,
        localContactId: Long,
        content: String,
        repository: ChatRepository
    ): Boolean = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext false
        val myUid = getAuth()?.currentUser?.uid ?: return@withContext false

        // Deterministic Room ID
        val convoId = if (myUid < friendFirebaseUid) "${myUid}_${friendFirebaseUid}" else "${friendFirebaseUid}_${myUid}"
        val messageId = java.util.UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val msgDoc = hashMapOf(
            "id" to messageId,
            "senderUid" to myUid,
            "content" to content,
            "timestamp" to timestamp,
            "type" to "Text",
            "durationSeconds" to 0
        )

        try {
            // Write to global conversations document messages
            firestore.collection("chats").document(convoId)
                .collection("messages").document(messageId)
                .set(msgDoc).await()

            // Save our own copy locally in Room right away
            repository.insertMessage(
                Message(
                    contactId = localContactId,
                    content = content,
                    timestamp = timestamp,
                    isFromMe = true,
                    status = "Sent",
                    firebaseId = messageId,
                    senderUid = myUid
                )
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending live message to Firestore", e)
            false
        }
    }

    // Subscribe to snapshot listener for a friend's conversation in real-time
    fun listenToRealtimeMessages(
        friendFirebaseUid: String,
        localContactId: Long,
        repository: ChatRepository,
        scope: CoroutineScope
    ): ListenerRegistration? {
        val firestore = getFirestore() ?: return null
        val myUid = getAuth()?.currentUser?.uid ?: return null

        val convoId = if (myUid < friendFirebaseUid) "${myUid}_${friendFirebaseUid}" else "${friendFirebaseUid}_${myUid}"
        
        Log.d(TAG, "Connecting real-time Firestore listener for room: $convoId")

        return firestore.collection("chats").document(convoId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Snapshot Listener error in Room $convoId", error)
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    scope.launch {
                        for (doc in snapshots.documentChanges) {
                            val changeType = doc.type
                            val docSnapshot = doc.document
                            val fId = docSnapshot.id
                            
                            val senderUid = docSnapshot.getString("senderUid") ?: ""
                            val content = docSnapshot.getString("content") ?: ""
                            val timestamp = docSnapshot.getLong("timestamp") ?: System.currentTimeMillis()
                            val type = docSnapshot.getString("type") ?: "Text"
                            val durationSeconds = docSnapshot.getLong("durationSeconds")?.toInt() ?: 0

                            // Skip if we sent this (we inserted it instantly locally upon send)
                            if (senderUid == myUid) continue

                            // Check local Room db to verify if we already saved this message
                            val existing = repository.getMessageByFirebaseId(fId)
                            if (existing == null) {
                                // Add to Room Db
                                repository.insertMessage(
                                    Message(
                                        contactId = localContactId,
                                        content = content,
                                        timestamp = timestamp,
                                        isFromMe = false,
                                        status = "Read",
                                        type = type,
                                        durationSeconds = durationSeconds,
                                        firebaseId = fId,
                                        senderUid = senderUid
                                    )
                                )
                                Log.d(TAG, "Synced new real-time message: $content from $senderUid")
                            }
                        }
                    }
                }
            }
    }

    // --- CALL SIGNALING --- 

    // Subscribe to incoming calls globally
    fun listenForIncomingCalls(repository: ChatRepository, onIncomingCall: (String, String, String) -> Unit): ListenerRegistration? {
        val firestore = getFirestore() ?: return null
        val myUid = getAuth()?.currentUser?.uid ?: return null

        return firestore.collection("users").document(myUid)
            .collection("calls")
            .whereEqualTo("status", "ringing")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Call Listener error", error)
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    for (doc in snapshots.documentChanges) {
                        val docSnapshot = doc.document
                        val callerUid = docSnapshot.getString("callerUid") ?: ""
                        val type = docSnapshot.getString("type") ?: "Audio"
                        val callId = docSnapshot.id
                        
                        // We also need to map callerUid to contact or at least pass it to ViewModel
                        onIncomingCall(callerUid, type, callId)
                    }
                }
            }
    }

    suspend fun startCall(friendFirebaseUid: String, type: String): String? = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext null
        val myUid = getAuth()?.currentUser?.uid ?: return@withContext null
        
        val callId = java.util.UUID.randomUUID().toString()
        val callDoc = hashMapOf(
            "callerUid" to myUid,
            "status" to "ringing",
            "type" to type,
            "timestamp" to System.currentTimeMillis()
        )

        try {
            // Write to receiver's calls subcollection
            firestore.collection("users").document(friendFirebaseUid)
                .collection("calls").document(callId)
                .set(callDoc).await()
            callId
        } catch (e: Exception) {
            Log.e(TAG, "Error starting call", e)
            null
        }
    }

    suspend fun acceptCall(callerFirebaseUid: String, callId: String) = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext
        val myUid = getAuth()?.currentUser?.uid ?: return@withContext
        
        try {
            firestore.collection("users").document(myUid)
                .collection("calls").document(callId)
                .update("status", "answered").await()
            
            // Also create a global call room for WebRTC signaling
            firestore.collection("calls").document(callId)
                .set(hashMapOf("status" to "answered", "timestamp" to System.currentTimeMillis())).await()
        } catch(e: Exception) {}
    }

    suspend fun endCall(friendFirebaseUid: String, callId: String) = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext
        val myUid = getAuth()?.currentUser?.uid ?: return@withContext
        
        try {
            // Mark ended in global
            firestore.collection("calls").document(callId).update("status", "ended").await()
            // Delete from receiver
            firestore.collection("users").document(friendFirebaseUid)
                .collection("calls").document(callId).update("status", "ended").await()
                
            // And delete from me just in case
            firestore.collection("users").document(myUid)
                 .collection("calls").document(callId).update("status", "ended").await()
        } catch(e: Exception) {}
    }

}
