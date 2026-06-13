package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Contact
import com.example.data.model.CallLog
import com.example.data.model.Message
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.HomeTab
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

// Frosted Glass Palette Helper
object ConvoColors {
    val DarkSlateBackground = Color(0xFFE9EEF9) // Frosted light canvas
    val CardBackground = Color(0xB3FFFFFF) // white/70 frosted glass panel
    val BorderSlate = Color(0x3BFFFFFF) // white/23 high-glass thin border
    val ElectricBlue = Color(0xFF0B57D0) // Rich premium brand blue
    val AccentMint = Color(0xFF10B981) // Active online green
    val ActiveVibe = Color(0xFFEC4899) // Video pink accents
    val TextPrimary = Color(0xFF0F172A) // slate-900 high contrast
    val TextSecondary = Color(0xFF475569) // slate-600 soft reader text

    // Extra components for mesh gradients
    val MeshBlue = Color(0x2B3B82F6) // Soft blurred blue mesh
    val MeshPurple = Color(0x2B9333EA) // Soft blurred purple mesh
    val HeaderBackground = Color(0x66FFFFFF) // white/40 frosted header
    val InputBackground = Color(0xCCFFFFFF) // white/80 frosted pill
    val BorderGlassStrong = Color(0x59FFFFFF) // white/35 prominent border
}

@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ConvoColors.DarkSlateBackground)
    ) {
        // Draw Mesh Gradient Circles
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Blue radial circle top-left
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(ConvoColors.MeshBlue, Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(-w * 0.1f, -h * 0.1f),
                    radius = w * 1.1f
                ),
                size = size
            )
            // Purple radial circle bottom-right
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(ConvoColors.MeshPurple, Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(w * 1.1f, h * 1.1f),
                    radius = w * 0.9f
                ),
                size = size
            )
        }
        
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ChatViewModel,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val callLogs by viewModel.callLogs.collectAsStateWithLifecycle()

    var showSimulationDialog by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    Box(modifier = modifier) {
        Scaffold(
            modifier = Modifier.background(Color.Transparent),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onProfileClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Profile",
                                tint = ConvoColors.ElectricBlue,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Text(
                            text = "CircleChat",
                            color = ConvoColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                },
                actions = {
                    // Add contact by phone
                    IconButton(
                        onClick = { showAddContactDialog = true },
                        modifier = Modifier.testTag("add_contact_trigger")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Contact",
                            tint = ConvoColors.ElectricBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConvoColors.HeaderBackground,
                    titleContentColor = ConvoColors.TextPrimary,
                    navigationIconContentColor = ConvoColors.TextPrimary,
                    actionIconContentColor = ConvoColors.TextPrimary
                ),
                // modifier = Modifier.border(1.dp, ConvoColors.BorderSlate)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = ConvoColors.HeaderBackground,
                tonalElevation = 0.dp,
                modifier = Modifier.border(1.dp, ConvoColors.BorderSlate, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                NavigationBarItem(
                    selected = currentTab == HomeTab.CHATS,
                    onClick = { viewModel.setTab(HomeTab.CHATS) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == HomeTab.CHATS) Icons.Default.Chat else Icons.Outlined.Chat,
                            contentDescription = "Chats"
                        )
                    },
                    label = { Text("Chats", fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ConvoColors.ElectricBlue,
                        unselectedIconColor = ConvoColors.TextSecondary,
                        selectedTextColor = ConvoColors.ElectricBlue,
                        unselectedTextColor = ConvoColors.TextSecondary,
                        indicatorColor = ConvoColors.BorderSlate
                    ),
                    modifier = Modifier.testTag("nav_chats")
                )
                NavigationBarItem(
                    selected = currentTab == HomeTab.CALLS,
                    onClick = { viewModel.setTab(HomeTab.CALLS) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == HomeTab.CALLS) Icons.Default.Call else Icons.Outlined.Call,
                            contentDescription = "Calls"
                        )
                    },
                    label = { Text("Calls", fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ConvoColors.ElectricBlue,
                        unselectedIconColor = ConvoColors.TextSecondary,
                        selectedTextColor = ConvoColors.ElectricBlue,
                        unselectedTextColor = ConvoColors.TextSecondary,
                        indicatorColor = ConvoColors.BorderSlate
                    ),
                    modifier = Modifier.testTag("nav_calls")
                )
                NavigationBarItem(
                    selected = currentTab == HomeTab.CONTACTS,
                    onClick = { viewModel.setTab(HomeTab.CONTACTS) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == HomeTab.CONTACTS) Icons.Default.People else Icons.Outlined.People,
                            contentDescription = "Contacts"
                        )
                    },
                    label = { Text("People", fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ConvoColors.ElectricBlue,
                        unselectedIconColor = ConvoColors.TextSecondary,
                        selectedTextColor = ConvoColors.ElectricBlue,
                        unselectedTextColor = ConvoColors.TextSecondary,
                        indicatorColor = ConvoColors.BorderSlate
                    ),
                    modifier = Modifier.testTag("nav_contacts")
                )
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                HomeTab.CHATS -> ChatsTabContent(viewModel, contacts)
                HomeTab.CALLS -> CallsTabContent(viewModel, callLogs, contacts)
                HomeTab.CONTACTS -> ContactsTabContent(viewModel, contacts)
            }
        }
    }

    AnimatedVisibility(
        visible = showAddContactDialog,
        enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
        exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it })
    ) {
        AddContactFullScreen(
            viewModel = viewModel,
            onBack = { showAddContactDialog = false },
            modifier = Modifier.fillMaxSize()
        )
    }
}

}


// --- CHATS SUB-TAB ---

@Composable
fun ChatsTabContent(viewModel: ChatViewModel, contacts: List<Contact>) {
    if (contacts.isEmpty()) {
        EmptyStateView(icon = Icons.Default.ChatBubbleOutline, message = "No active conversations yet.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(0.dp)
    ) {
        items(contacts) { contact ->
            ChatContactListItem(
                contact = contact,
                viewModel = viewModel,
                onClick = { viewModel.selectContactChat(contact.id) }
            )
        }
    }
}

@Composable
fun ChatContactListItem(
    contact: Contact,
    viewModel: ChatViewModel,
    onClick: () -> Unit
) {
    // Collect last message for this contact
    val lastMsgFlow = remember(contact.id) { viewModel.getLatestMessage(contact.id) }
    val lastMsg by lastMsgFlow.collectAsStateWithLifecycle(initialValue = null)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ContactAvatar(contact = contact, size = 52.dp)

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = contact.name,
                            color = ConvoColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (lastMsg != null) formatTime(lastMsg!!.timestamp) else "",
                            color = ConvoColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val previewText = lastMsg?.content ?: "Start a standard conversation..."
                        val isUnreadSim = contact.status == "Online" && lastMsg != null && !lastMsg!!.isFromMe

                        Text(
                            text = previewText,
                            color = if (isUnreadSim) ConvoColors.TextPrimary else ConvoColors.TextSecondary,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (isUnreadSim) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )

                        if (isUnreadSim) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(ConvoColors.AccentMint),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "1",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (lastMsg != null && lastMsg!!.isFromMe) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Read status",
                                tint = ConvoColors.AccentMint,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(start = 82.dp),
                thickness = 1.dp,
                color = ConvoColors.BorderSlate
            )
        }
    }
}

// --- CALLS SUB-TAB ---

@Composable
fun CallsTabContent(viewModel: ChatViewModel, callLogs: List<CallLog>, contacts: List<Contact>) {
    if (callLogs.isEmpty()) {
        EmptyStateView(icon = Icons.Outlined.PhoneMissed, message = "Call history empty. Start a call from the contacts list.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Calls",
                    color = ConvoColors.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { viewModel.clearAllLogs() }) {
                    Text("Clear All", color = ConvoColors.ActiveVibe, fontSize = 12.sp)
                }
            }
        }

        items(callLogs) { log ->
            val contact = contacts.find { it.id == log.contactId }
            CallLogCard(log = log, contact = contact, onRedialAudio = {
                contact?.let { viewModel.startCall(it, isVideo = false) }
            }, onRedialVideo = {
                contact?.let { viewModel.startCall(it, isVideo = true) }
            })
        }
    }
}

@Composable
fun CallLogCard(
    log: CallLog,
    contact: Contact?,
    onRedialAudio: () -> Unit,
    onRedialVideo: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ConvoColors.CardBackground)
            .border(1.dp, ConvoColors.BorderSlate, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (contact != null) {
            ContactAvatar(contact = contact, size = 46.dp)
        } else {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(ConvoColors.BorderSlate)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact?.name ?: "Unknown Caller",
                color = ConvoColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                val (arrowIcon, arrowColor) = when (log.direction) {
                    "Incoming" -> Icons.Default.CallReceived to ConvoColors.AccentMint
                    "Outgoing" -> Icons.Default.CallMade to ConvoColors.ElectricBlue
                    else -> Icons.Default.CallMissed to ConvoColors.ActiveVibe
                }
                Icon(
                    imageVector = arrowIcon,
                    contentDescription = log.direction,
                    tint = arrowColor,
                    modifier = Modifier.size(14.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "${log.direction} • ${log.type}",
                    color = ConvoColors.TextSecondary,
                    fontSize = 12.sp
                )

                if (log.durationSeconds > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(${log.durationSeconds / 60}m ${log.durationSeconds % 60}s)",
                        color = ConvoColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onRedialAudio) {
                Icon(Icons.Default.Phone, contentDescription = "Audio Call", tint = ConvoColors.ElectricBlue)
            }
            IconButton(onClick = onRedialVideo) {
                Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = ConvoColors.ActiveVibe)
            }
        }
    }
}

// --- CONTACTS SUB-TAB ---

@Composable
fun ContactsTabContent(viewModel: ChatViewModel, contacts: List<Contact>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PaddingLabel(text = "Contacts Directory")
        }

        items(contacts) { contact ->
            ContactDirectoryCard(
                contact = contact,
                onMessage = { viewModel.selectContactChat(contact.id) },
                onAudioCall = { viewModel.startCall(contact, isVideo = false) },
                onVideoCall = { viewModel.startCall(contact, isVideo = true) }
            )
        }
    }
}

@Composable
fun ContactDirectoryCard(
    contact: Contact,
    onMessage: () -> Unit,
    onAudioCall: () -> Unit,
    onVideoCall: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ConvoColors.CardBackground),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ConvoColors.BorderSlate, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ContactAvatar(contact = contact, size = 50.dp)

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.name,
                        color = ConvoColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Status: ${contact.status} • ${contact.lastActive}",
                        color = if (contact.status == "Online") ConvoColors.AccentMint else ConvoColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = contact.bio,
                color = ConvoColors.TextSecondary,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onMessage,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ConvoColors.CardBackground,
                        contentColor = ConvoColors.TextPrimary
                    ),
                    modifier = Modifier.border(1.dp, ConvoColors.BorderSlate, RoundedCornerShape(12.dp)),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = ConvoColors.TextPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text("Chat", fontSize = 12.sp, color = ConvoColors.TextPrimary)
                }

                Button(
                    onClick = onAudioCall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ConvoColors.ElectricBlue.copy(alpha = 0.15f),
                        contentColor = ConvoColors.ElectricBlue
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, ConvoColors.ElectricBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = ConvoColors.ElectricBlue)
                    Spacer(Modifier.width(6.dp))
                    Text("Audio", fontSize = 12.sp, color = ConvoColors.ElectricBlue)
                }

                Button(
                    onClick = onVideoCall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ConvoColors.ActiveVibe.copy(alpha = 0.15f),
                        contentColor = ConvoColors.ActiveVibe
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, ConvoColors.ActiveVibe.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp), tint = ConvoColors.ActiveVibe)
                    Spacer(Modifier.width(6.dp))
                    Text("Video", fontSize = 12.sp, color = ConvoColors.ActiveVibe)
                }
            }
        }
    }
}

// --- SHARED REUSABLE SUB-VIEWS ---

@Composable
fun StoriesRow(contacts: List<Contact>, onStoryClicked: (Contact) -> Unit) {
    Column {
        PaddingLabel(text = "Active Profiles (Ring with Video)")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            contacts.take(4).forEach { contact ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onStoryClicked(contact) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .border(2.dp, Brush.sweepGradient(listOf(ConvoColors.ActiveVibe, ConvoColors.ElectricBlue)), CircleShape)
                            .padding(3.dp)
                    ) {
                        ContactAvatar(contact = contact, size = 52.dp, showStatusBadge = false)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = contact.name.split(" ").first(),
                        color = ConvoColors.TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ContactAvatar(
    contact: Contact,
    size: androidx.compose.ui.unit.Dp,
    showStatusBadge: Boolean = true
) {
    val char = contact.name.firstOrNull()?.toString() ?: "U"
    val animDegrees by animateFloatAsState(targetValue = if (contact.status == "Online") 360f else 0f, animationSpec = tween(1000))

    // Seed backcolor based on contact name
    val listColors = listOf(
        Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B),
        Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFFEC4899)
    )
    val colorIndex = Math.abs(contact.name.hashCode()) % listColors.size
    val avatarBg = listColors[colorIndex]

    Box(modifier = Modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(avatarBg, avatarBg.copy(alpha = 0.6f)))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = char,
                color = Color.White,
                fontSize = (size.value * 0.4f).sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (showStatusBadge) {
            val badgeColor = when (contact.status) {
                "Online" -> ConvoColors.AccentMint
                "Busy" -> Color(0xFFF59E0B)
                else -> Color(0xFF64748B)
            }
            Box(
                modifier = Modifier
                    .size(size * 0.28f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(size * 0.04f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(badgeColor)
                )
            }
        }
    }
}

@Composable
fun PaddingLabel(text: String) {
    Text(
        text = text,
        color = ConvoColors.TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun EmptyStateView(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ConvoColors.BorderSlate,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = message,
            color = ConvoColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
