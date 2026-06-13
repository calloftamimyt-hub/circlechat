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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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

    Box(modifier = Modifier.fillMaxSize()) {
        if (isVideo) {
            // RENDERS PREMIUM DUAL-VIDEO SIMULATION
            VideoCallLayout(state, viewModel)
        } else {
            // RENDERS PREMIUM AUDIO CALL PULSATERS
            AudioCallLayout(state, viewModel)
        }

        // Overlay Call control panels pinned to screen base
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                    )
                )
                .padding(horizontal = 24.dp, vertical = 32.dp)
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
                    }
                }
            }
        }
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
    viewModel: ChatViewModel
) {
    // Generate beautiful color backgrounds depending on chosen video filter simulation!
    val filterColor = when (state.selectedFilter) {
        "Cinematic" -> Color(0xE7D4B6) // Warm gold
        "Sepia" -> Color(0x99AFA189) // Earth dust sepia
        "Vibrant" -> Color(0x3BFF00D4) // Radiant neon electric magenta
        "Noir" -> Color(0xCC000000) // Pure High Contrast black/white grayscale
        else -> Color.Transparent
    }

    val animatedFilterColor by animateColorAsState(targetValue = filterColor, animationSpec = tween(500))

    Box(modifier = Modifier.fillMaxSize()) {
        // remote participant
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // WEBRTC REMOTE SURFACE RENDERER
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        init(EglBase.create().eglBaseContext, null)
                        setScalingType(org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                        setEnableHardwareScaler(true)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Pinned Header statistics overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = state.contact.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Connected • ${formatDuration(state.durationSeconds)}",
                    color = ConvoColors.AccentMint,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Status Badge pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "LIVE • HD",
                    color = ConvoColors.ActiveVibe,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Local Video (PiP)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 140.dp, end = 20.dp)
                .width(100.dp)
                .height(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.72f))
                .border(2.dp, ConvoColors.ElectricBlue, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (state.isCamOn) {
                 // WEBRTC LOCAL SURFACE RENDERER
                 AndroidView(
                    factory = { ctx ->
                        SurfaceViewRenderer(ctx).apply {
                            init(EglBase.create().eglBaseContext, null)
                            setScalingType(org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                            setEnableHardwareScaler(true)
                            setMirror(true)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VideocamOff,
                        contentDescription = "Camera Off",
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Camera Off",
                        color = Color.Red,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
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
