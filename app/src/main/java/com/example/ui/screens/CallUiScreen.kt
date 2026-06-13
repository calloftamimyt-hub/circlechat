package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.SurfaceViewRenderer
import org.webrtc.EglBase
import com.example.ui.viewmodel.CallState
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

@Composable
fun CallUiScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val callState by viewModel.callState.collectAsStateWithLifecycle()

    val state = callState
    if (state is CallState.Idle) return

    GlassBackground(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        when (state) {
            is CallState.Ringing -> RingingUi(state, viewModel)
            is CallState.Connected -> ConnectedUi(state, viewModel)
            else -> {}
        }
    }
}

// --- RINGING SCREEN ---

@Composable
fun RingingUi(
    state: CallState.Ringing,
    viewModel: ChatViewModel
) {
    val contact = state.contact
    val isOutgoing = state.isOutgoing
    val isVideo = state.callType == "Video"

    // Glowing ring animation using infinite transition pulse
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper Title Details
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Icon(
                imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Phone,
                contentDescription = null,
                tint = if (isVideo) ConvoColors.ActiveVibe else ConvoColors.ElectricBlue,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isVideo) "Convo Video Call" else "Convo Voice Call",
                color = ConvoColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = contact.name,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isOutgoing) "Calling..." else "Incoming...",
                color = if (isOutgoing) ConvoColors.ElectricBlue else ConvoColors.AccentMint,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Concentric Glower Ring & Avatar
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            // Pulse circle
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(pulseScale)
                    .graphicsLayer(alpha = pulseAlpha)
                    .clip(CircleShape)
                    .background(if (isVideo) ConvoColors.ActiveVibe else ConvoColors.ElectricBlue)
            )
            // Foreground avatar base
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                    .padding(4.dp)
            ) {
                ContactAvatar(contact = contact, size = 100.dp, showStatusBadge = false)
            }
        }

        // Action controls bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 36.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isOutgoing) {
                // Outgoing cancel button only
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { viewModel.declineOrCancelCall() },
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                            .testTag("cancel_call_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Hang up",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Decline", color = Color.White, fontSize = 12.sp)
                }
            } else {
                // Incoming offer decline and accept choices
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { viewModel.declineOrCancelCall() },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                                .testTag("decline_call_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Decline",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Decline", color = ConvoColors.TextSecondary, fontSize = 12.sp)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { viewModel.acceptCall() },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(ConvoColors.AccentMint)
                                .testTag("accept_call_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Accept",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Accept", color = ConvoColors.TextPrimary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// --- ACTIVE CONNECTED SCREEN ---

@Composable
fun ConnectedUi(
    state: CallState.Connected,
    viewModel: ChatViewModel
) {
    val contact = state.contact
    val isVideo = state.callType == "Video"
    var showAddUserDialog by remember { mutableStateOf(false) }

    // Toggle controls state. In video calls, controls are initially hidden to comply with instructions.
    // In audio calls, they are always shown.
    var showControls by remember { mutableStateOf(!isVideo) }

    // Floating/Sliding controls offset
    val bottomOffset by animateDpAsState(
        targetValue = if (showControls) 0.dp else 300.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (isVideo) {
            // RENDERS PREMIUM DUAL-VIDEO SIMULATION WITH TAP/ADD Handlers
            VideoCallLayout(
                state = state,
                viewModel = viewModel,
                onBackgroundTap = { showControls = !showControls },
                onAddContactClick = { showAddUserDialog = true }
            )
        } else {
            // RENDERS PREMIUM AUDIO CALL PULSATERS
            AudioCallLayout(state, viewModel)
        }

        // Overlay Call control panels pinned to screen base
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .offset(y = bottomOffset)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                    )
                )
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .clickable(enabled = false, onClick = {}) // prevent click-through toggle
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // If video chat, render filters row simulation!
                if (isVideo && state.isCamOn) {
                    FilterSelectorRow(selectedFilter = state.selectedFilter, onChooseFilter = { viewModel.setVideoFilter(it) })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Choice
                    CallCircularButton(
                        icon = if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        isActive = !state.isMuted,
                        onClick = { viewModel.toggleMute() },
                        tag = "toggle_mute"
                    )

                    // End call button (red center focal)
                    IconButton(
                        onClick = { viewModel.endCall() },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .testTag("end_call_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End call",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Speakerphone Toggle
                    CallCircularButton(
                        icon = if (state.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                        isActive = state.isSpeakerOn,
                        onClick = { viewModel.toggleSpeaker() },
                        tag = "toggle_speaker"
                    )

                    if (isVideo) {
                        // Camera on/off modifier
                        CallCircularButton(
                            icon = if (state.isCamOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            isActive = state.isCamOn,
                            onClick = { viewModel.toggleCamera() },
                            tag = "toggle_camera"
                        )

                        // Bottom right minimized arrow button to open the chat view
                        IconButton(
                            onClick = { viewModel.setVideoCallMinimized(true) },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.22f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                .testTag("chat_arrow_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Minimize to Chat View",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Gorgeous Add User Dialog (from list of previously chatted contacts)
    if (showAddUserDialog) {
        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            containerColor = Color(0xFF1E222A),
            title = {
                Text(
                    text = "Add User to Call",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                val allContacts by viewModel.contacts.collectAsStateWithLifecycle()
                
                if (allContacts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        Text("No match found", color = Color.Gray, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "Previously Chatted List",
                                color = ConvoColors.AccentMint,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        items(allContacts) { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        showAddUserDialog = false
                                        // Starts call / they will receive a call on their phone as requested!
                                        viewModel.startCall(contact, isVideo = true)
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ContactAvatar(contact = contact, size = 36.dp, showStatusBadge = false)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (contact.status == "Online") "Online" else contact.lastActive,
                                        color = if (contact.status == "Online") ConvoColors.AccentMint else Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call",
                                    tint = ConvoColors.AccentMint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddUserDialog = false }) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }
}

// --- SUB CALL LAYOUT DETAILS ---

@Composable
fun AudioCallLayout(
    state: CallState.Connected,
    viewModel: ChatViewModel
) {
    // Dynamic Equalizer simulator values using periodic launches
    var waveAmp1 by remember { mutableStateOf(16.dp) }
    var waveAmp2 by remember { mutableStateOf(32.dp) }
    var waveAmp3 by remember { mutableStateOf(24.dp) }
    var waveAmp4 by remember { mutableStateOf(44.dp) }
    var waveAmp5 by remember { mutableStateOf(12.dp) }

    LaunchedEffect(Unit) {
        val rand = java.util.Random()
        while (true) {
            delay(120)
            waveAmp1 = (12 + rand.nextInt(32)).dp
            waveAmp2 = (14 + rand.nextInt(48)).dp
            waveAmp3 = (10 + rand.nextInt(38)).dp
            waveAmp4 = (16 + rand.nextInt(52)).dp
            waveAmp5 = (8 + rand.nextInt(26)).dp
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Core header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Text(
                text = state.contact.name,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Connected • ${formatDuration(state.durationSeconds)}",
                color = ConvoColors.AccentMint,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            if (state.isMuted) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "MUTED",
                    color = Color.Red,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Centered glowing speaker outline & equalizer graphics
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .border(2.dp, ConvoColors.ElectricBlue.copy(alpha = 0.5f), CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                ContactAvatar(contact = state.contact, size = 110.dp, showStatusBadge = false)
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Animated Equalizer Frequency Wavebars
            Row(
                modifier = Modifier
                    .height(64.dp)
                    .width(180.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EqualizerBar(height = waveAmp1, color = ConvoColors.ElectricBlue)
                EqualizerBar(height = waveAmp2, color = ConvoColors.ActiveVibe)
                EqualizerBar(height = waveAmp3, color = ConvoColors.AccentMint)
                EqualizerBar(height = waveAmp4, color = ConvoColors.ElectricBlue)
                EqualizerBar(height = waveAmp5, color = ConvoColors.ActiveVibe)
            }
        }

        // Just empty space to align button row correctly
        Spacer(modifier = Modifier.height(110.dp))
    }
}

@Composable
fun VideoCallLayout(
    state: CallState.Connected,
    viewModel: ChatViewModel,
    onBackgroundTap: () -> Unit,
    onAddContactClick: () -> Unit
) {
    // State to swap full-screen and PiP videos
    var isSwapped by remember { mutableStateOf(false) }

    // Screen and PiP dimension sizes for coordinate constraint calculations
    var parentSize by remember { mutableStateOf(IntSize.Zero) }
    var pipSize by remember { mutableStateOf(IntSize.Zero) }

    // Coordinates of the floating window
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Snap target positions
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)
    )

    // Position PiP automatically to bottom-right corner when parent and child measurements are complete
    LaunchedEffect(parentSize, pipSize) {
        if (parentSize != IntSize.Zero && pipSize != IntSize.Zero && offsetX == 0f && offsetY == 0f) {
            // Default position: Bottom Right corner with margins (respecting control panels insets)
            offsetX = parentSize.width - pipSize.width - 48f
            offsetY = parentSize.height - pipSize.height - 340f
        }
    }

    // Filter backdrop simulation overlay
    val filterColor = when (state.selectedFilter) {
        "Cinematic" -> Color(0xE7D4B6).copy(alpha = 0.18f) // Warm cinematic gold
        "Sepia" -> Color(0x99AFA189).copy(alpha = 0.15f) // Earthy sepia tone
        "Vibrant" -> Color(0x3BFF00D4).copy(alpha = 0.15f) // Neon pink glow
        "Noir" -> Color(0xCC000000).copy(alpha = 0.35f) // Moody monochrome grayscale
        else -> Color.Transparent
    }
    val animatedFilterColor by animateColorAsState(targetValue = filterColor, animationSpec = tween(500))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { parentSize = it }
            .background(Color(0xFF0D0E11))
    ) {
        // --------------------------------------------------------------------
        // 1. FULLSCREEN MAIN VIDEO STREAM
        // --------------------------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    onBackgroundTap()
                }
        ) {
            // Determine what is currently full-screen
            val isMainLocal = isSwapped
            
            if (isMainLocal) {
                // If local stream is shown full-screen, respect camera state
                if (state.isCamOn) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { ctx ->
                                SurfaceViewRenderer(ctx).apply {
                                    init(viewModel.rootEglBaseContext, null)
                                    setScalingType(org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                                    setEnableHardwareScaler(true)
                                    setMirror(true) // local camera feels natural mirrored
                                    viewModel.attachLocalRenderer(this)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        // Local Fullscreen Mic Transmission Status Indicator
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = 100.dp, start = 24.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (state.isMuted) Color.Red else ConvoColors.AccentMint,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (state.isMuted) "Microphone Muted" else "Voice Transmitting • Active",
                                    color = if (state.isMuted) Color.Red else ConvoColors.AccentMint,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // Camera is off, show beautiful centered full screen avatar screen
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF15171C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .border(2.dp, ConvoColors.ElectricBlue.copy(alpha = 0.4f), CircleShape)
                                    .padding(4.dp)
                            ) {
                                ContactAvatar(contact = state.contact, size = 110.dp, showStatusBadge = false)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Your Camera is Off",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Default: Remote stream is full-screen
                RemoteVideoFeed(
                    contact = state.contact,
                    isCompact = false,
                    isMuted = state.isMuted,
                    isSpeakerOn = state.isSpeakerOn,
                    viewModel = viewModel
                )
            }

            // Apply filter overlay matching active chosen option
            if (state.selectedFilter != "Normal") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(animatedFilterColor)
                )
            }
        }

        // --------------------------------------------------------------------
        // 2. PINNED HEADER STATISTICS OVERLAY
        // --------------------------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isSwapped) "Your View" else state.contact.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Connected • ${formatDuration(state.durationSeconds)}",
                    color = ConvoColors.AccentMint,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Top right "add" icon button - replaces the "P2P STABLE • HD" text overlay as requested
            IconButton(
                onClick = { onAddContactClick() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .testTag("add_user_in_call_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Add User to Call",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // --------------------------------------------------------------------
        // 3. FLAGGED PORTRAIT FLOATING PiP VIDEO WINDOW
        // --------------------------------------------------------------------
        Box(
            modifier = Modifier
                .onSizeChanged { pipSize = it }
                .offset {
                    IntOffset(
                        animatedOffsetX.roundToInt(),
                        animatedOffsetY.roundToInt()
                    )
                }
                .width(115.dp)
                .height(175.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black.copy(alpha = 0.85f))
                .border(2.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                // Add double-border for high-end look
                .border(4.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                .pointerInput(parentSize, pipSize) {
                    detectDragGestures(
                        onDragEnd = {
                            if (parentSize == IntSize.Zero || pipSize == IntSize.Zero) return@detectDragGestures

                            // Target horizontal snap guides
                            val leftSnap = 24f
                            val rightSnap = parentSize.width - pipSize.width - 24f
                            
                            // Target vertical snap guides
                            val topSnap = 100f
                            val bottomSnap = parentSize.height - pipSize.height - 340f

                            val midX = (leftSnap + rightSnap) / 2
                            val midY = (topSnap + bottomSnap) / 2

                            // Magnet snap to nearest of the 4 bounds smoothly
                            val targetX = if (offsetX < midX) leftSnap else rightSnap
                            val targetY = if (offsetY < midY) topSnap else bottomSnap

                            offsetX = targetX
                            offsetY = targetY
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y

                            if (parentSize != IntSize.Zero && pipSize != IntSize.Zero) {
                                // Constrain drag dimensions so it cannot be launched entirely off screen
                                val maxX = parentSize.width - pipSize.width - 12f
                                val maxY = parentSize.height - pipSize.height - 180f
                                offsetX = offsetX.coerceIn(12f, maxX)
                                offsetY = offsetY.coerceIn(40f, maxY)
                            }
                        }
                    )
                }
                .clickable {
                    // Tap to swap full screen layouts!
                    isSwapped = !isSwapped
                },
            contentAlignment = Alignment.Center
        ) {
            val isPipLocal = !isSwapped

            if (isPipLocal) {
                // If local camera is on, render it inside PiP
                if (state.isCamOn) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { ctx ->
                                SurfaceViewRenderer(ctx).apply {
                                    init(viewModel.rootEglBaseContext, null)
                                    setScalingType(org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                                    setEnableHardwareScaler(true)
                                    setMirror(true)
                                    viewModel.attachLocalRenderer(this)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        // Local PiP Mic Active Overlay
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (state.isMuted) Color.Red else ConvoColors.AccentMint,
                                modifier = Modifier.size(8.dp)
                            )
                            Text(
                                text = if (state.isMuted) "MUTED" else "MIC ON",
                                color = if (state.isMuted) Color.Red else ConvoColors.AccentMint,
                                fontSize = 6.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Camera off layout within PiP frame
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E2129)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideocamOff,
                                contentDescription = "Camera Off",
                                tint = Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Camera Off",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                // Render remote feedback stream inside PiP frame
                RemoteVideoFeed(
                    contact = state.contact,
                    isCompact = true,
                    isMuted = state.isMuted,
                    isSpeakerOn = state.isSpeakerOn,
                    viewModel = viewModel
                )
            }

            // Overlay label to let the user know they can click to swap
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Swap",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "TAP TO SWAP",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

// --- CONTROLS COMPONENT HELPERS ---

@Composable
fun CallCircularButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val roundedBg = if (isActive) Color.White.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.35f)
    val iconColor = if (isActive) Color.White else Color.Red

    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(roundedBg)
            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            .clickable { onClick() }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun FilterSelectorRow(
    selectedFilter: String,
    onChooseFilter: (String) -> Unit
) {
    val filtersList = listOf("Normal", "Cinematic", "Sepia", "Vibrant", "Noir")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        filtersList.forEach { filter ->
            val isSelected = filter == selectedFilter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) ConvoColors.ElectricBlue else Color.Transparent)
                    .clickable { onChooseFilter(filter) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = filter,
                    color = if (isSelected) Color.White else ConvoColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun EqualizerBar(height: androidx.compose.ui.unit.Dp, color: Color) {
    val animatedHeight by animateDpAsState(
        targetValue = height,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Box(
        modifier = Modifier
            .width(10.dp)
            .height(animatedHeight)
            .clip(RoundedCornerShape(5.dp))
            .background(color)
    )
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(minutes, secs)
}

@Composable
fun RemoteVideoFeed(
    contact: com.example.data.model.Contact,
    isCompact: Boolean,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    viewModel: ChatViewModel
) {
    // Elegant pulsing and breathing animations
    val infiniteTransition = rememberInfiniteTransition(label = "remote_pulse")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    // Animated bounce values for interactive equalizer visualization
    var amp1 by remember { mutableStateOf(10.dp) }
    var amp2 by remember { mutableStateOf(20.dp) }
    var amp3 by remember { mutableStateOf(15.dp) }
    var amp4 by remember { mutableStateOf(28.dp) }
    var amp5 by remember { mutableStateOf(12.dp) }

    LaunchedEffect(Unit) {
        val rand = java.util.Random()
        while (true) {
            delay(100)
            amp1 = (8 + rand.nextInt(18)).dp
            amp2 = (10 + rand.nextInt(26)).dp
            amp3 = (6 + rand.nextInt(20)).dp
            amp4 = (12 + rand.nextInt(30)).dp
            amp5 = (5 + rand.nextInt(15)).dp
        }
    }

    // Colors matching contact name
    val listColors = listOf(
        Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B),
        Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFFEC4899)
    )
    val colorIndex = Math.abs(contact.name.hashCode()) % listColors.size
    val primaryColor = listColors[colorIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF151821),
                        primaryColor.copy(alpha = 0.15f),
                        Color(0xFF0F1116)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Actual remote Video Stream
        AndroidView(
            factory = { ctx ->
                SurfaceViewRenderer(ctx).apply {
                    init(viewModel.rootEglBaseContext, null)
                    setScalingType(org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                    setEnableHardwareScaler(true)
                    viewModel.attachRemoteRenderer(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Digital Camera Viewfinder Overlay
        if (!isCompact) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top elements
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 100.dp, start = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ConvoColors.AccentMint)
                    )
                    Text(
                        text = "AUDIO & VIDEO ACTIVE",
                        color = ConvoColors.AccentMint,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "HD 1080p • 60 FPS • Encrypted P2P",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 120.dp, start = 8.dp)
                )
            }
        }

        if (isCompact) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Text(
                    text = contact.name,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(4.dp)
                )
            }
        }
    }
}

@Composable
fun MinimizedCallWindow(
    state: CallState.Connected,
    viewModel: ChatViewModel
) {
    var parentSize by remember { mutableStateOf(IntSize.Zero) }
    var pipSize by remember { mutableStateOf(IntSize.Zero) }

    // Coordinates of the minimized floating window
    var offsetX by remember { mutableStateOf(30f) }
    var offsetY by remember { mutableStateOf(500f) }

    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)
    )

    // Position automatically once size is layout-decided
    LaunchedEffect(parentSize, pipSize) {
        if (parentSize != IntSize.Zero && pipSize != IntSize.Zero && offsetX == 30f && offsetY == 500f) {
            offsetX = 24f
            offsetY = parentSize.height - pipSize.height - 120f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { parentSize = it }
    ) {
        Box(
            modifier = Modifier
                .onSizeChanged { pipSize = it }
                .offset {
                    IntOffset(
                        animatedOffsetX.roundToInt(),
                        animatedOffsetY.roundToInt()
                    )
                }
                .width(180.dp)
                .height(100.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xE61E222A)) // dark frosted look
                .border(2.dp, ConvoColors.ElectricBlue.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .pointerInput(parentSize, pipSize) {
                    detectDragGestures(
                        onDragEnd = {
                            if (parentSize == IntSize.Zero || pipSize == IntSize.Zero) return@detectDragGestures
                            
                            val leftSnap = 24f
                            val rightSnap = parentSize.width - pipSize.width - 24f
                            val topSnap = 100f
                            val bottomSnap = parentSize.height - pipSize.height - 120f

                            val midX = (leftSnap + rightSnap) / 2
                            val midY = (topSnap + bottomSnap) / 2

                            val targetX = if (offsetX < midX) leftSnap else rightSnap
                            val targetY = if (offsetY < midY) topSnap else bottomSnap

                            offsetX = targetX
                            offsetY = targetY
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y

                            if (parentSize != IntSize.Zero && pipSize != IntSize.Zero) {
                                val maxX = parentSize.width - pipSize.width - 12f
                                val maxY = parentSize.height - pipSize.height - 60f
                                offsetX = offsetX.coerceIn(12f, maxX)
                                offsetY = offsetY.coerceIn(40f, maxY)
                            }
                        }
                    )
                }
                .clickable {
                    // Tap to restore call to full screen
                    viewModel.setVideoCallMinimized(false)
                }
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Dual Avatars side-by-side representing both participants
                Row(
                    horizontalArrangement = Arrangement.spacedBy((-12).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Let's draw other participant avatar
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .border(1.5.dp, Color.White, CircleShape)
                            .clip(CircleShape)
                    ) {
                        ContactAvatar(contact = state.contact, size = 42.dp, showStatusBadge = false)
                    }
                    // Let's draw local participant avatar
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .border(1.5.dp, ConvoColors.AccentMint, CircleShape)
                            .clip(CircleShape)
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "You",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ConvoColors.AccentMint)
                    )
                    Text(
                        text = "Call Active • ${formatDuration(state.durationSeconds)}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
