package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CallLog
import com.example.data.model.Contact
import com.example.data.model.Message
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.webrtc.*

sealed interface CallState {
    object Idle : CallState
    data class Ringing(
        val contact: Contact,
        val isOutgoing: Boolean,
        val callType: String // "Audio" or "Video"
    ) : CallState
    data class Connected(
        val contact: Contact,
        val callType: String, // "Audio" or "Video"
        val durationSeconds: Int,
        val isMuted: Boolean = false,
        val isSpeakerOn: Boolean = true,
        val isCamOn: Boolean = true,
        val selectedFilter: String = "Normal" // "Normal", "Cinematic", "Sepia", "Vibrant", "Noir"
    ) : CallState
}

enum class HomeTab {
    CHATS, CALLS, CONTACTS
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChatRepository(application)

    // UI state flows
    val contacts: StateFlow<List<Contact>> = repository.allContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callLogs: StateFlow<List<CallLog>> = repository.allCallLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Tab Navigation
    private val _currentTab = MutableStateFlow(HomeTab.CHATS)
    val currentTab: StateFlow<HomeTab> = _currentTab.asStateFlow()

    // Selected Contact ID for current active Chat (null if on list screens)
    private val _activeContactId = MutableStateFlow<Long?>(null)
    val activeContactId: StateFlow<Long?> = _activeContactId.asStateFlow()

    // Observable flow of messages for the active conversation
    val activeMessages: Flow<List<Message>> = _activeContactId.flatMapLatest { id ->
        if (id != null) {
            repository.getMessagesForContact(id)
        } else {
            flowOf(emptyList())
        }
    }

    // Active contact detailing for headers
    val activeContact: Flow<Contact?> = _activeContactId.flatMapLatest { id ->
        if (id != null) repository.getContactById(id) else flowOf(null)
    }

    // Active Call State
    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _isVideoCallMinimized = MutableStateFlow(false)
    val isVideoCallMinimized: StateFlow<Boolean> = _isVideoCallMinimized.asStateFlow()

    fun setVideoCallMinimized(minimized: Boolean) {
        _isVideoCallMinimized.value = minimized
    }

    // Typing field input state
    private val _typedMessage = MutableStateFlow("")
    val typedMessage: StateFlow<String> = _typedMessage.asStateFlow()

    // My Firebase UID for Identity
    private val _myUid = MutableStateFlow<String?>(null)
    val myUid: StateFlow<String?> = _myUid.asStateFlow()

    // Firebase Auth Connection State
    private val _isFirebaseActive = MutableStateFlow(false)
    val isFirebaseActive: StateFlow<Boolean> = _isFirebaseActive.asStateFlow()

    // Firebase Sync Status Logs & Dialog helper message
    private val _syncStatus = MutableStateFlow("Local Mode (Click Go Online to link)")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    val isFirebaseLibraryInitialized: Boolean
        get() = com.example.data.firebase.FirebaseManager.isFirebaseAvailable

    // Ringtone sound system
    private val soundPlayer = CallSoundPlayer(application)

    // WebRTC Core components
    val rootEglBaseContext: EglBase.Context = EglBase.create().eglBaseContext
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var localVideoSource: VideoSource? = null
    internal var localVideoTrack: VideoTrack? = null
    private var localAudioSource: AudioSource? = null
    internal var localAudioTrack: AudioTrack? = null
    internal var remoteVideoTrack: VideoTrack? = null
    private var webrtcManager: com.example.data.webrtc.WebRTCManager? = null
    private var iceCandidatesListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private var attachedLocalRenderer: SurfaceViewRenderer? = null
    private var attachedRemoteRenderer: SurfaceViewRenderer? = null

    // For call timers
    private var callTimerJob: Job? = null
    private var ringTimeoutJob: Job? = null

    // For active Firestore real-time message queries
    private var activeMessageListener: com.google.firebase.firestore.ListenerRegistration? = null

    private var activeFirebaseCallId: String? = null
    private var activeFirebaseListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var incomingCallsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var signalingJob: Job? = null
    private var signalingListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        // Pre-seed the database if it's currently empty
        repository.preseedDatabaseIfEmpty(viewModelScope)
        
        // Initialize Firebase safely
        com.example.data.firebase.FirebaseManager.initialize(application)

        // Check if already signed in
        val currentUser = com.example.data.firebase.FirebaseManager.getAuth()?.currentUser
        if (currentUser != null) {
            _myUid.value = currentUser.uid
            _isFirebaseActive.value = true
            startListeningForCalls()
        }
    }

    private fun startListeningForCalls() {
        incomingCallsListener?.remove()
        incomingCallsListener = com.example.data.firebase.FirebaseManager.listenForIncomingCalls(repository) { callerUid, type, callId ->
            viewModelScope.launch {
                val caller = repository.getContactByFirebaseUid(callerUid)
                if (caller != null && _callState.value is CallState.Idle) {
                    activeFirebaseCallId = callId
                    _callState.value = CallState.Ringing(
                        contact = caller,
                        isOutgoing = false,
                        callType = type
                    )
                    // Ring on incoming call
                    soundPlayer.startRinging()
                    startListeningForIncomingCallStatus(caller, callId)
                }
            }
        }
    }

    private fun startListeningForIncomingCallStatus(caller: Contact, callId: String) {
        activeFirebaseListener?.remove()
        val firestore = com.example.data.firebase.FirebaseManager.getFirestore() ?: return
        val myUid = _myUid.value ?: return

        activeFirebaseListener = firestore.collection("users").document(myUid)
            .collection("calls").document(callId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatViewModel", "Error listening for incoming call status", error)
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists() || snapshot.getString("status") == "ended") {
                    val current = _callState.value
                    if (current is CallState.Ringing || current is CallState.Connected) {
                        resetCallEngine()
                    }
                }
            }
    }

    private fun startListeningForCallStatus(contact: Contact, callId: String, callType: String) {
        activeFirebaseListener?.remove()
        val firestore = com.example.data.firebase.FirebaseManager.getFirestore() ?: return

        activeFirebaseListener = firestore.collection("users").document(contact.firebaseUid)
            .collection("calls").document(callId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatViewModel", "Error listening for call acceptance status", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("status")
                    if (status == "answered") {
                        val current = _callState.value
                        if (current !is CallState.Connected) {
                            connectCall(contact, callType, isCaller = true)
                        }
                    } else if (status == "ended") {
                        val current = _callState.value
                        if (current is CallState.Ringing || current is CallState.Connected) {
                            resetCallEngine()
                        }
                    }
                } else {
                    val current = _callState.value
                    if (current is CallState.Ringing || current is CallState.Connected) {
                        resetCallEngine()
                    }
                }
            }
    }

    fun loginOfflineDemo() {
        viewModelScope.launch {
            _myUid.value = "local_demo_uid"
            _isFirebaseActive.value = true
            _syncStatus.value = "Demo Offline Mode"
            seedDemoContacts()
        }
    }

    fun seedDemoContacts() {
        viewModelScope.launch {
            val dbContacts = repository.allContacts.first()
            if (dbContacts.isEmpty()) {
                val demoContacts = listOf(
                    Contact(name = "Tahsin Ahmed", avatarUrl = "", status = "Online", lastActive = "Now", bio = "CircleChat Developer • Ping me for call tests!", firebaseUid = ""),
                    Contact(name = "Sara Islam", avatarUrl = "", status = "Away", lastActive = "5m ago", bio = "Design Lead • Enjoys video chats ✨", firebaseUid = ""),
                    Contact(name = "Aurnob Roy", avatarUrl = "", status = "Offline", lastActive = "Yesterday", bio = "DevOps Advocate • Ping for voice calls", firebaseUid = "")
                )
                demoContacts.forEach {
                    repository.insertContact(it)
                }
            }
        }
    }

    fun setTab(tab: HomeTab) {
        _currentTab.value = tab
    }

    // Login with Phone
    fun signInWithPhone(phone: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _syncStatus.value = "Authenticating..."
            val uid = com.example.data.firebase.FirebaseManager.signInWithPhoneAndPassword(phone, pass)
            if (uid != null) {
                _myUid.value = uid
                _isFirebaseActive.value = true
                _syncStatus.value = "Online. Signed in with $phone"
                onResult(true, "Success")
            } else {
                _syncStatus.value = "Authentication failed."
                _isFirebaseActive.value = false
                onResult(false, "Login failed. Check internet, or ensure account exists.")
            }
        }
    }

    // Sign Up with Phone
    fun signUpWithPhone(phone: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _syncStatus.value = "Creating Account..."
            val uid = com.example.data.firebase.FirebaseManager.signUpWithPhoneAndPassword(phone, pass)
            if (uid != null) {
                _myUid.value = uid
                _isFirebaseActive.value = true
                _syncStatus.value = "Online. Registration complete."
                
                // Save phone to profile
                com.example.data.firebase.FirebaseManager.saveCurrentUserProfile(phone)
                onResult(true, "Success")
            } else {
                _syncStatus.value = "Registration failed."
                _isFirebaseActive.value = false
                onResult(false, "Sign-up failed.")
            }
        }
    }

    // Add / connect a friend by Phone Number
    fun connectFriendByPhone(phone: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _syncStatus.value = "Searching for: $phone"
            val contact = com.example.data.firebase.FirebaseManager.connectFriendByPhone(phone.trim(), repository)
            if (contact != null) {
                _syncStatus.value = "Linked successfully: ${contact.name}"
                onComplete(true)
            } else {
                _syncStatus.value = "Failed: No account found with that number."
                onComplete(false)
            }
        }
    }

    fun handleIntent(intent: android.content.Intent?) {
        if (intent == null) return
        val isCall = intent.getBooleanExtra("incoming_call", false)
        if (isCall) {
            val callerName = intent.getStringExtra("callerName") ?: "Unknown"
            val callType = intent.getStringExtra("callType") ?: "Audio"
            val roomId = intent.getStringExtra("roomId") ?: ""
            val callerUid = intent.getStringExtra("callerUid") ?: "mock_uid"
            val acceptCall = intent.getBooleanExtra("accept_call", false)
            
            viewModelScope.launch {
                // Try to get contact, if null, simulate a contact
                var contact = repository.getContactByFirebaseUid(callerUid)
                if (contact == null) {
                    contact = Contact(
                        name = callerName,
                        avatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                        bio = "Incoming Call...",
                        status = "Online",
                        lastActive = "Now",
                        firebaseUid = callerUid
                    )
                }
                
                activeFirebaseCallId = roomId
                if (acceptCall) {
                     _callState.value = CallState.Ringing(contact, false, callType)
                     com.example.data.firebase.FirebaseManager.acceptCall(callerUid, roomId)
                     connectCall(contact, callType, isCaller = false)
                } else {
                     _callState.value = CallState.Ringing(contact, false, callType)
                     soundPlayer.startRinging()
                     startListeningForIncomingCallStatus(contact, roomId)
                }
            }
        } else {
            val chatUid = intent.getStringExtra("chat_firebase_uid")
            if (chatUid != null) {
                viewModelScope.launch {
                    val contact = repository.getContactByFirebaseUid(chatUid)
                    if (contact != null) {
                        selectContactChat(contact.id)
                    }
                }
            }
        }
    }

    fun selectContactChat(contactId: Long?) {
        _activeContactId.value = contactId
        _typedMessage.value = "" // clear draft

        // Unsubscribe from previous snapshot listeners
        activeMessageListener?.remove()
        activeMessageListener = null

        if (contactId != null) {
            // Subscribe to real-time sync if this contact is a Firebase Live Buddy
            viewModelScope.launch {
                repository.getContactById(contactId).first()?.let { contact ->
                    com.example.data.firebase.FirebaseManager.activeChatFirebaseUid = contact.firebaseUid
                    if (contact.firebaseUid.isNotEmpty() && com.example.data.firebase.FirebaseManager.isFirebaseAvailable) {
                        activeMessageListener = com.example.data.firebase.FirebaseManager.listenToRealtimeMessages(
                            friendFirebaseUid = contact.firebaseUid,
                            localContactId = contactId,
                            repository = repository,
                            scope = viewModelScope
                        )
                    }
                }
            }
        } else {
            com.example.data.firebase.FirebaseManager.activeChatFirebaseUid = null
        }
    }

    fun logout() {
        _isFirebaseActive.value = false
        _myUid.value = null
        activeMessageListener?.remove()
        activeMessageListener = null
    }

    fun updateTypedMessage(text: String) {
        _typedMessage.value = text
    }

    fun getLatestMessage(contactId: Long): Flow<Message?> {
        return repository.getLatestMessage(contactId)
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearAllLogs()
        }
    }

    fun sendMessage() {
        val contactId = _activeContactId.value ?: return
        val text = _typedMessage.value.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            _typedMessage.value = ""

            val contact = repository.getContactById(contactId).first()
            if (contact != null && contact.firebaseUid.isNotEmpty() && com.example.data.firebase.FirebaseManager.isFirebaseAvailable) {
                val success = com.example.data.firebase.FirebaseManager.sendLiveMessage(
                    friendFirebaseUid = contact.firebaseUid,
                    localContactId = contactId,
                    content = text,
                    repository = repository
                )
                if (!success) {
                    // Fallback to local insertion if sending live failed (e.g. offline)
                    repository.insertMessage(
                        Message(
                            contactId = contactId,
                            content = text,
                            timestamp = System.currentTimeMillis(),
                            isFromMe = true,
                            status = "Sent"
                        )
                    )
                }
            } else {
                // Regular local flow
                repository.insertMessage(
                    Message(
                        contactId = contactId,
                        content = text,
                        timestamp = System.currentTimeMillis(),
                        isFromMe = true,
                        status = "Sent"
                    )
                )

                // Simulate quick reactive reply from the contact to show Messenger-like responsive behavior!
                delay(1500)
                val replyText = getMockAutoReply(text)
                repository.insertMessage(
                    Message(
                        contactId = contactId,
                        content = replyText,
                        timestamp = System.currentTimeMillis(),
                        isFromMe = false,
                        status = "Read"
                    )
                )
            }
        }
    }

    private fun getMockAutoReply(query: String): String {
        val q = query.lowercase()
        return when {
            q.contains("hello") || q.contains("hey") || q.contains("hi") -> 
                "Hey! Just checking out Convo's beautiful interface. It feels so premium!"
            q.contains("call") || q.contains("video") || q.contains("audio") || q.contains("talk") -> 
                "Yes! Click the Call option in the top bar. I'm connected and ready to chat!"
            q.contains("how") || q.contains("fine") || q.contains("good") -> 
                "Everything is performing incredibly! Our Room database queries are updating dynamically on the main thread."
            q.contains("design") || q.contains("beautiful") || q.contains("screens") -> 
                "I agree, the color scheme matches material standards, and the typography pairings look amazing! ✨"
            else -> "That is awesome! Let's schedule a call on Convo soon to detail our next release."
        }
    }

    // --- CALLS HANDLING ENGINE ---

    private var isWebRtcInitialized = false

    private fun initWebRtc() {
        if (webrtcManager != null) return
        val context = getApplication<Application>().applicationContext
        
        if (!isWebRtcInitialized) {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                    .setEnableInternalTracer(true)
                    .createInitializationOptions()
            )
            isWebRtcInitialized = true
        }
        
        webrtcManager = com.example.data.webrtc.WebRTCManager(
            context = context,
            eglBaseContext = rootEglBaseContext,
            onIceCandidate = { candidate ->
                sendIceCandidateToFirebase(candidate)
            },
            onAddStream = { stream ->
                if (stream.videoTracks.isNotEmpty()) {
                    remoteVideoTrack = stream.videoTracks[0]
                    // If UI was already attached before track arrived
                    attachedRemoteRenderer?.let { remoteVideoTrack?.addSink(it) }
                }
            }
        )
        peerConnectionFactory = webrtcManager?.getFactory()
        
        // Setup Audio
        val audioConstraints = MediaConstraints()
        localAudioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory?.createAudioTrack("101", localAudioSource)
    }

    fun startCameraCapture() {
        initWebRtc()
        val context = getApplication<Application>().applicationContext
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.w("ChatViewModel", "Camera permission is NOT granted, skipping WebRTC capture")
            return
        }
        if (videoCapturer != null) return

        try {
            surfaceTextureHelper = SurfaceTextureHelper.create("WebRtcCaptureThread", rootEglBaseContext)
            
            val enumerator = Camera2Enumerator(context)
            val deviceNames = enumerator.deviceNames
            var frontCamera: String? = null
            for (name in deviceNames) {
                if (enumerator.isFrontFacing(name)) {
                    frontCamera = name
                    break
                }
            }
            if (frontCamera == null && deviceNames.isNotEmpty()) {
                frontCamera = deviceNames[0]
            }

            if (frontCamera != null) {
                videoCapturer = enumerator.createCapturer(frontCamera, null)
                localVideoSource = peerConnectionFactory?.createVideoSource(videoCapturer!!.isScreencast)
                videoCapturer?.initialize(surfaceTextureHelper, context, localVideoSource?.capturerObserver)
                
                localVideoTrack = peerConnectionFactory?.createVideoTrack("100", localVideoSource)
                videoCapturer?.startCapture(640, 480, 30)
                
                // If view finders are waiting, immediately attach them
                attachedLocalRenderer?.let { localVideoTrack?.addSink(it) }
                attachedRemoteRenderer?.let { localVideoTrack?.addSink(it) }
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error starting camera capture for WebRTC", e)
        }
    }

    fun stopCameraCapture() {
        try {
            attachedLocalRenderer?.let { localVideoTrack?.removeSink(it) }
            attachedRemoteRenderer?.let { remoteVideoTrack?.removeSink(it) }
            attachedLocalRenderer = null
            attachedRemoteRenderer = null

            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
            videoCapturer = null

            surfaceTextureHelper?.dispose()
            surfaceTextureHelper = null

            localVideoSource?.dispose()
            localVideoSource = null
            localAudioSource?.dispose()
            localAudioSource = null
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error stopping camera capture", e)
        }
    }

    fun attachLocalRenderer(renderer: SurfaceViewRenderer) {
        attachedLocalRenderer = renderer
        localVideoTrack?.addSink(renderer)
    }

    fun attachRemoteRenderer(renderer: SurfaceViewRenderer) {
        attachedRemoteRenderer = renderer
        remoteVideoTrack?.addSink(renderer)
    }

    fun startCall(contact: Contact, isVideo: Boolean) {
        // Stop any running calls
        resetCallEngine()

        _callState.value = CallState.Ringing(
            contact = contact,
            isOutgoing = true,
            callType = if (isVideo) "Video" else "Audio"
        )
        // Play outgoing calling sound dialback
        soundPlayer.startCallingSound()

        // Firebase real-time signaling
        if (contact.firebaseUid.isNotEmpty() && com.example.data.firebase.FirebaseManager.isFirebaseAvailable) {
            viewModelScope.launch {
                val callId = com.example.data.firebase.FirebaseManager.startCall(contact.firebaseUid, if (isVideo) "Video" else "Audio")
                activeFirebaseCallId = callId
                if (callId != null) {
                    startListeningForCallStatus(contact, callId, if (isVideo) "Video" else "Audio")
                }
            }
        }

        // Automatically connect call after 3 seconds of "ringing" simulation if offline
        ringTimeoutJob = viewModelScope.launch {
            delay(3000)
            if (activeFirebaseCallId == null) {
                connectCall(contact, if (isVideo) "Video" else "Audio", isCaller = true)
            }
        }
    }

    fun receiveIncomingCallSimulated(contact: Contact, isVideo: Boolean) {
        resetCallEngine()

        _callState.value = CallState.Ringing(
            contact = contact,
            isOutgoing = false,
            callType = if (isVideo) "Video" else "Audio"
        )
        // Play incoming ringtone sound
        soundPlayer.startRinging()
    }

    fun acceptCall() {
        val current = _callState.value
        if (current is CallState.Ringing) {
            ringTimeoutJob?.cancel()
            
            // Accept on Firebase
            if (activeFirebaseCallId != null && current.contact.firebaseUid.isNotEmpty()) {
                viewModelScope.launch {
                    com.example.data.firebase.FirebaseManager.acceptCall(current.contact.firebaseUid, activeFirebaseCallId!!)
                }
            }

            connectCall(current.contact, current.callType, isCaller = false)
        }
    }

    fun declineOrCancelCall() {
        val current = _callState.value
        if (current is CallState.Ringing) {
            ringTimeoutJob?.cancel()
            
            // Decline on firebase
            if (activeFirebaseCallId != null && current.contact.firebaseUid.isNotEmpty()) {
                viewModelScope.launch {
                    com.example.data.firebase.FirebaseManager.endCall(current.contact.firebaseUid, activeFirebaseCallId!!)
                }
            }

            // Log missed call if incoming, cancelled if outgoing
            viewModelScope.launch {
                val direction = if (current.isOutgoing) "Outgoing" else "Missed"
                repository.insertCallLog(
                    CallLog(
                        contactId = current.contact.id,
                        direction = direction,
                        type = current.callType,
                        timestamp = System.currentTimeMillis(),
                        durationSeconds = 0
                    )
                )
                // Insert a system cancellation log message in their chat
                repository.insertMessage(
                    Message(
                        contactId = current.contact.id,
                        content = if (current.isOutgoing) "Cancelled ${current.callType.lowercase()} call" else "Missed ${current.callType.lowercase()} call",
                        timestamp = System.currentTimeMillis(),
                        isFromMe = current.isOutgoing,
                        type = if (current.callType == "Video") "VideoCall" else "AudioCall",
                        durationSeconds = 0
                    )
                )
            }
        }
        resetCallEngine()
    }

    private fun connectCall(contact: Contact, callType: String, isCaller: Boolean) {
        soundPlayer.stop() // stop ringtone loops immediately

        _callState.value = CallState.Connected(
            contact = contact,
            callType = callType,
            durationSeconds = 0
        )

        initWebRtc()
        if (callType == "Video") {
            startCameraCapture()
        }
        webrtcManager?.createPeerConnection()
        webrtcManager?.addLocalStream(if (callType == "Video") localVideoTrack else null, localAudioTrack)

        startWebRTCSignaling(isCaller)

        // Start duration counter incrementing every second
        callTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _callState.value
                if (current is CallState.Connected) {
                    _callState.value = current.copy(durationSeconds = current.durationSeconds + 1)
                } else {
                    break
                }
            }
        }
    }

    fun endCall() {
        val current = _callState.value
        if (current is CallState.Connected) {
            val duration = current.durationSeconds
            
            // Hangup on Firebase
            if (activeFirebaseCallId != null && current.contact.firebaseUid.isNotEmpty()) {
                viewModelScope.launch {
                    com.example.data.firebase.FirebaseManager.endCall(current.contact.firebaseUid, activeFirebaseCallId!!)
                }
            }

            viewModelScope.launch {
                // Log the completed call
                repository.insertCallLog(
                    CallLog(
                        contactId = current.contact.id,
                        direction = "Outgoing", // outgoing log simulation
                        type = current.callType,
                        timestamp = System.currentTimeMillis(),
                        durationSeconds = duration
                    )
                )

                // Add call completion to the chat system messaging history!
                val minutesStr = "%02d:%02d".format(duration / 60, duration % 60)
                repository.insertMessage(
                    Message(
                        contactId = current.contact.id,
                        content = "${current.callType} call ended • $minutesStr",
                        timestamp = System.currentTimeMillis(),
                        isFromMe = true,
                        type = if (current.callType == "Video") "VideoCall" else "AudioCall",
                        durationSeconds = duration
                    )
                )
            }
        }
        resetCallEngine()
    }

    private fun testAudioAndVideo() {}
    
    private fun startWebRTCSignaling(isCaller: Boolean) {
        val cid = activeFirebaseCallId ?: return
        val firestore = com.example.data.firebase.FirebaseManager.getFirestore() ?: return
        val myUid = _myUid.value ?: return

        signalingJob = viewModelScope.launch {
            if (isCaller) {
                webrtcManager?.createOffer { sessionDescription ->
                    val offerMap = hashMapOf("type" to "offer", "sdp" to sessionDescription.description)
                    firestore.collection("calls").document(cid).update("offer", offerMap)
                }
            }
            
            signalingListenerRegistration = firestore.collection("calls").document(cid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                    if (!isCaller && snapshot.contains("offer") && webrtcManager?.peerConnection?.remoteDescription == null) {
                        val offerMap = snapshot.get("offer") as? Map<String, String>
                        val sdpString = offerMap?.get("sdp")
                        if (sdpString != null) {
                            webrtcManager?.setRemoteDescription(SessionDescription(SessionDescription.Type.OFFER, sdpString))
                            webrtcManager?.createAnswer { sessionDescription ->
                                val ansMap = hashMapOf("type" to "answer", "sdp" to sessionDescription.description)
                                firestore.collection("calls").document(cid).update("answer", ansMap)
                            }
                        }
                    }

                    if (isCaller && snapshot.contains("answer") && webrtcManager?.peerConnection?.remoteDescription == null) {
                        val ansMap = snapshot.get("answer") as? Map<String, String>
                        val sdpString = ansMap?.get("sdp")
                        if (sdpString != null) {
                            webrtcManager?.setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, sdpString))
                        }
                    }
                }

            iceCandidatesListener = firestore.collection("calls").document(cid).collection("candidates")
                .addSnapshotListener { snapshots, error ->
                    if (error != null || snapshots == null) return@addSnapshotListener
                    for (dc in snapshots.documentChanges) {
                        if (dc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            val data = dc.document.data
                            val sender = data["sender"] as? String
                            if (sender != null && sender != myUid) {
                                val sdpMid = data["sdpMid"] as? String ?: ""
                                val sdpMLineIndex = (data["sdpMLineIndex"] as? Long)?.toInt() ?: 0
                                val sdp = data["sdp"] as? String ?: ""
                                webrtcManager?.addRemoteIceCandidate(IceCandidate(sdpMid, sdpMLineIndex, sdp))
                            }
                        }
                    }
                }
        }
    }

    private fun sendIceCandidateToFirebase(candidate: IceCandidate) {
        val cid = activeFirebaseCallId ?: return
        val firestore = com.example.data.firebase.FirebaseManager.getFirestore() ?: return
        val map = hashMapOf(
            "sdpMid" to candidate.sdpMid,
            "sdpMLineIndex" to candidate.sdpMLineIndex,
            "sdp" to candidate.sdp,
            "sender" to (_myUid.value ?: "unknown")
        )
        firestore.collection("calls").document(cid).collection("candidates").add(map)
    }

    // Call settings modifiers in connected mode
    fun toggleMute() {
        val current = _callState.value
        if (current is CallState.Connected) {
            val newState = !current.isMuted
            _callState.value = current.copy(isMuted = newState)
            localAudioTrack?.setEnabled(!newState)
        }
    }

    fun toggleSpeaker() {
        val current = _callState.value
        if (current is CallState.Connected) {
            val newState = !current.isSpeakerOn
            _callState.value = current.copy(isSpeakerOn = newState)
            
            try {
                val context = getApplication<Application>().applicationContext
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
                audioManager.isSpeakerphoneOn = newState
            } catch (e: Exception) {}
        }
    }

    fun toggleCamera() {
        val current = _callState.value
        if (current is CallState.Connected && current.callType == "Video") {
            val nextCam = !current.isCamOn
            _callState.value = current.copy(isCamOn = nextCam)
            localVideoTrack?.setEnabled(nextCam)
        }
    }

    fun setVideoFilter(filter: String) {
        val current = _callState.value
        if (current is CallState.Connected && current.callType == "Video") {
            _callState.value = current.copy(selectedFilter = filter)
        }
    }

    private fun resetCallEngine() {
        soundPlayer.stop() // stop any audio loops
        stopCameraCapture() // stop camera capture

        signalingJob?.cancel()
        signalingJob = null
        signalingListenerRegistration?.remove()
        signalingListenerRegistration = null
        iceCandidatesListener?.remove()
        iceCandidatesListener = null
        webrtcManager?.dispose()
        webrtcManager = null

        callTimerJob?.cancel()
        callTimerJob = null
        ringTimeoutJob?.cancel()
        ringTimeoutJob = null
        activeFirebaseListener?.remove()
        activeFirebaseListener = null
        activeFirebaseCallId = null
        _isVideoCallMinimized.value = false
        _callState.value = CallState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        activeMessageListener?.remove()
        activeMessageListener = null
        resetCallEngine()
    }
}
