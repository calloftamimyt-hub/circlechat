package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Beautiful Preset Avatar images that are fully styled inside the CircleChat engine
val PRESET_AVATARS = listOf(
    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=300&q=80", // Stylish default
    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=300&q=80", // Professional male
    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=300&q=80", // Smiley female
    "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=300&q=80", // Tech developer
    "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=300&q=80", // Nature profile
    "https://api.dicebear.com/7.x/pixel-art/svg?seed=Tamim", // Cool Pixel Art
    "https://api.dicebear.com/7.x/bottts/svg?seed=Circle", // Awesome Robot avatar
    "https://api.dicebear.com/7.x/identicon/svg?seed=Chat" // Abstract graphic
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Persistent state fields initialized with premium user context
    var selectedAvatarUrl by rememberSaveable { mutableStateOf(PRESET_AVATARS[0]) }
    var currentName by rememberSaveable { mutableStateOf("Tamim") }
    var currentBio by rememberSaveable { mutableStateOf("Staying premium on CircleChat! 🌟") }
    var currentPhone by rememberSaveable { mutableStateOf("+880 1944-716683") }

    // Dialog state controllers
    var showAvatarSelector by remember { mutableStateOf(false) }
    var showLogoutWarning by remember { mutableStateOf(false) }

    // Expanded states for settings modules to display full widgets beautifully
    var expandedProfile by remember { mutableStateOf(false) }
    var expandedPrivacy by remember { mutableStateOf(false) }
    var expandedChats by remember { mutableStateOf(false) }
    var expandedCalls by remember { mutableStateOf(false) }
    var expandedFriends by remember { mutableStateOf(false) }
    var expandedIslamic by remember { mutableStateOf(false) }
    var expandedStorage by remember { mutableStateOf(false) }
    var expandedHelp by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("CircleChat Profile", fontWeight = FontWeight.Bold, color = ConvoColors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ConvoColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Visual Avatar & Details Section
            item {
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clickable { showAvatarSelector = true }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(ConvoColors.ElectricBlue, ConvoColors.ActiveVibe)
                                )
                            )
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.White)
                        ) {
                            AsyncImage(
                                model = selectedAvatarUrl,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // Tappable Camera overlay badge
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(ConvoColors.ElectricBlue)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Edit photo",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currentName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConvoColors.TextPrimary
                )

                Text(
                    text = currentPhone,
                    fontSize = 14.sp,
                    color = ConvoColors.TextSecondary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ConvoColors.AccentMint)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Active Status: Online",
                        fontSize = 13.sp,
                        color = ConvoColors.AccentMint,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 1. My Profile Tab (Expandable)
            item {
                ModuleCard(
                    icon = Icons.Default.Person,
                    title = "👤 My Profile",
                    subtitle = "Update Name, status quote, phone number",
                    isExpanded = expandedProfile,
                    onToggle = { expandedProfile = !expandedProfile }
                ) {
                    var editName by remember { mutableStateOf(currentName) }
                    var editBio by remember { mutableStateOf(currentBio) }
                    var editPhone by remember { mutableStateOf(currentPhone) }
                    var isSaving by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Display Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ConvoColors.ElectricBlue,
                                focusedLabelColor = ConvoColors.ElectricBlue
                            )
                        )

                        OutlinedTextField(
                            value = editBio,
                            onValueChange = { editBio = it },
                            label = { Text("Status Bio") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ConvoColors.ElectricBlue,
                                focusedLabelColor = ConvoColors.ElectricBlue
                            )
                        )

                        OutlinedTextField(
                            value = editPhone,
                            onValueChange = { editPhone = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ConvoColors.ElectricBlue,
                                focusedLabelColor = ConvoColors.ElectricBlue
                            )
                        )

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isSaving = true
                                    delay(800) // Beautiful fake save delay
                                    isSaving = false
                                    currentName = editName
                                    currentBio = editBio
                                    currentPhone = editPhone
                                    Toast.makeText(context, "Profile details saved successfully!", Toast.LENGTH_SHORT).show()
                                    expandedProfile = false
                                }
                            },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(containerColor = ConvoColors.ElectricBlue)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save Changes", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Privacy & Security Tab
            item {
                ModuleCard(
                    icon = Icons.Default.Security,
                    title = "🔒 Privacy & Security",
                    subtitle = "End-to-end encryption, start-up app locks",
                    isExpanded = expandedPrivacy,
                    onToggle = { expandedPrivacy = !expandedPrivacy }
                ) {
                    var forceLock by rememberSaveable { mutableStateOf(false) }
                    var e2eStatus by rememberSaveable { mutableStateOf(true) }
                    var readReceipts by rememberSaveable { mutableStateOf(true) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Manage your chat workspace credentials and access safely.",
                            fontSize = 13.sp,
                            color = ConvoColors.TextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        ToggleSettingRow(
                            label = "End-to-End Encryption Mode",
                            checked = e2eStatus,
                            onCheckedChange = { e2eStatus = it }
                        )

                        ToggleSettingRow(
                            label = "Biometric Lock App on Startup",
                            checked = forceLock,
                            onCheckedChange = { forceLock = it }
                        )

                        ToggleSettingRow(
                            label = "Active Read Receipts (Blue ticks)",
                            checked = readReceipts,
                            onCheckedChange = { readReceipts = it }
                        )
                    }
                }
            }

            // 3. Chats Theme Settings
            item {
                ModuleCard(
                    icon = Icons.Default.Chat,
                    title = "💬 Chats Customization",
                    subtitle = "Customize wallpapers and readable fonts",
                    isExpanded = expandedChats,
                    onToggle = { expandedChats = !expandedChats }
                ) {
                    var selectedWallpaper by remember { mutableStateOf("Minimal Classic") }
                    var fontSizeScale by remember { mutableStateOf(14f) }

                    val wallpapers = listOf("Minimal Classic", "Midnight Blue", "Cosmic Lavender", "Tropical Forest")

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Select Chat Background Base Grid", color = ConvoColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            wallpapers.forEach { wp ->
                                val isSelected = wp == selectedWallpaper
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) ConvoColors.ElectricBlue else ConvoColors.HeaderBackground)
                                        .border(1.dp, if (isSelected) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedWallpaper = wp
                                            Toast.makeText(context, "$wp wallpaper applied!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = wp.split(" ")[1],
                                        color = if (isSelected) Color.White else ConvoColors.TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text("Text Font Size: ${fontSizeScale.toInt()}sp", color = ConvoColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Slider(
                            value = fontSizeScale,
                            onValueChange = { fontSizeScale = it },
                            valueRange = 12f..24f,
                            colors = SliderDefaults.colors(
                                thumbColor = ConvoColors.ElectricBlue,
                                activeTrackColor = ConvoColors.ElectricBlue
                            )
                        )
                    }
                }
            }

            // 4. Calls Control Room
            item {
                ModuleCard(
                    icon = Icons.Default.Call,
                    title = "📞 Audio & Video Calls",
                    subtitle = "HD voice quality, network optimization settings",
                    isExpanded = expandedCalls,
                    onToggle = { expandedCalls = !expandedCalls }
                ) {
                    var hdVoice by remember { mutableStateOf(true) }
                    var lowDataMode by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ToggleSettingRow(
                            label = "Activate HD Voice Quality",
                            checked = hdVoice,
                            onCheckedChange = { hdVoice = it }
                        )

                        ToggleSettingRow(
                            label = "Low Data Usage Mode (Compacted packets)",
                            checked = lowDataMode,
                            onCheckedChange = { lowDataMode = it }
                        )

                        Divider(color = ConvoColors.BorderSlate, thickness = 1.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { Toast.makeText(context, "System ringtones loaded!", Toast.LENGTH_SHORT).show() }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Current Ringtone Preset", color = ConvoColors.TextPrimary, fontWeight = FontWeight.Bold)
                            Text("Standard CircleSynth", color = ConvoColors.ActiveVibe, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 5. Friends Settings
            item {
                ModuleCard(
                    icon = Icons.Default.People,
                    title = "👥 Friends & Contacts Sync",
                    subtitle = "Configure network syncing and contact list states",
                    isExpanded = expandedFriends,
                    onToggle = { expandedFriends = !expandedFriends }
                ) {
                    var isSyncing by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Sync contacts periodically matching active accounts inside CircleChat database.",
                            fontSize = 13.sp,
                            color = ConvoColors.TextSecondary
                        )

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isSyncing = true
                                    delay(1600) // simulated network contact syncing
                                    isSyncing = false
                                    Toast.makeText(context, "Contacts synced with Cloud Repository!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ConvoColors.ElectricBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Syncing Global Database...", color = Color.White)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sync CircleChat Contacts Now", color = Color.White)
                            }
                        }
                    }
                }
            }

            // 6. Islamic Settings Widget (Highlighted Mosque Star Feature!)
            item {
                ModuleCard(
                    icon = Icons.Default.Brightness5, // Brightness/Sun metaphor representing daily salah prayer timings
                    title = "🕌 Islamic Settings & Tasbih",
                    subtitle = "Bangladesh prayer clock, Tasbih counter & Hadiths",
                    isExpanded = expandedIslamic,
                    onToggle = { expandedIslamic = !expandedIslamic }
                ) {
                    // Islamic state variables
                    var tasbihCount by rememberSaveable { mutableStateOf(0) }
                    val listDua = listOf("SubhanAllah (সুবহানআল্লাহ)", "Alhamdulillah (আলহামদুলিল্লাহ)", "Allahu Akbar (আল্লাহু আকবার)")
                    var currentDuaIndex by remember { mutableStateOf(0) }
                    var maxTarget by remember { mutableStateOf(33) }

                    // Hadith index loop
                    var hadithIndex by rememberSaveable { mutableStateOf(0) }
                    val beautifulHadiths = listOf(
                        "\"The best among you are those who have the best manners and character.\" — Sahih al-Bukhari",
                        "\"None of you has faith until he desires for his brother what he desires for himself.\" — Sahih al-Bukhari",
                        "\"Smiling in the face of your brother is charity.\" — Jami` at-Tirmidhi",
                        "\"The strong man is not the one who can wrestle, but the strong man is the one who controls himself in anger.\" — Sahih al-Bukhari"
                    )

                    // Pulse scale on click animation
                    var clickTargetScale by remember { mutableStateOf(1f) }
                    val clickedScaleAnimated by animateFloatAsState(
                        targetValue = clickTargetScale,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "Tasbih button bounce"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header Islamic Style Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF0F5132), Color(0xFF198754)) // Islamic Emerald Green
                                    )
                                )
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    "✨ Bangladesh Daily Salat Clock",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Fajr: 4:10 AM | Dhuhr: 12:08 PM | Asr: 4:32 PM | Maghrib: 6:46 PM | Isha: 8:12 PM",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Divider(color = ConvoColors.BorderSlate, thickness = 1.dp)

                        // Hadith Panel
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ConvoColors.HeaderBackground)
                                .clickable {
                                    hadithIndex = (hadithIndex + 1) % beautifulHadiths.size
                                }
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Hadith of the Moment 📖", color = ConvoColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Next ➔", color = ConvoColors.ElectricBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    beautifulHadiths[hadithIndex],
                                    color = ConvoColors.TextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        Divider(color = ConvoColors.BorderSlate, thickness = 1.dp)

                        // Interactive Tasbih Counter Canvas Widget
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x33000000))
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Tasbih Counter 📿", color = ConvoColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            // Tab selector for target phrase
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listDua.forEachIndexed { i, dua ->
                                    val isSelected = i == currentDuaIndex
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFF198754) else ConvoColors.HeaderBackground)
                                            .clickable {
                                                currentDuaIndex = i
                                                tasbihCount = 0
                                            }
                                            .padding(vertical = 6.dp, horizontal = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dua.substringBefore(" "),
                                            color = if (isSelected) Color.White else ConvoColors.TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Interactive Circle Clicker Button
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .scale(clickedScaleAnimated)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(Color(0xFF25D366), Color(0xFF128C7E))
                                        )
                                    )
                                    .clickable {
                                        coroutineScope.launch {
                                            clickTargetScale = 0.9f
                                            delay(100)
                                            clickTargetScale = 1.05f
                                            delay(100)
                                            clickTargetScale = 1f
                                        }
                                        tasbihCount++
                                        if (tasbihCount == maxTarget) {
                                            Toast.makeText(context, "MashaAllah! Target Completed!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$tasbihCount",
                                        color = Color.White,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "TAP",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = listDua[currentDuaIndex],
                                color = ConvoColors.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            // Quick controls (Reset, Custom match target)
                            Row(
                                modifier = Modifier
                                    .padding(top = 10.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { tasbihCount = 0 }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(18.dp), tint = ConvoColors.TextSecondary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset", color = ConvoColors.TextSecondary)
                                }

                                TextButton(onClick = {
                                    maxTarget = if (maxTarget == 33) 100 else 33
                                }) {
                                    Text("Target: $maxTarget", color = ConvoColors.ElectricBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 7. Storage & Data Widget
            item {
                ModuleCard(
                    icon = Icons.Default.Storage,
                    title = "📂 Storage & Mobile Data",
                    subtitle = "Sweep Cache database, clear media memory logs",
                    isExpanded = expandedStorage,
                    onToggle = { expandedStorage = !expandedStorage }
                ) {
                    var isClearing by remember { mutableStateOf(false) }
                    var cacheCleared by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("CircleChat System Cache usage:", fontSize = 13.sp, color = ConvoColors.TextSecondary)
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (cacheCleared) "Disk Space Freed: 142.4 MB" else "Total Cached Images & Waves: 142.4 MB",
                                fontWeight = FontWeight.Bold,
                                color = if (cacheCleared) ConvoColors.AccentMint else ConvoColors.TextPrimary
                            )
                            Text(text = if (cacheCleared) "0.0 MB" else "142.4 MB", fontSize = 12.sp, color = ConvoColors.TextSecondary)
                        }

                        LinearProgressIndicator(
                            progress = { if (cacheCleared) 0f else 0.65f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ConvoColors.ElectricBlue,
                            trackColor = ConvoColors.HeaderBackground
                        )

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isClearing = true
                                    delay(1500) // Beautiful cache sweeping simulator
                                    isClearing = false
                                    cacheCleared = true
                                    Toast.makeText(context, "142.4 MB cache cleared!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ConvoColors.ActiveVibe),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isClearing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sweeping system cache memory...", color = Color.White)
                            } else {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (cacheCleared) "Cache Cleared" else "Clear Temporary Cache (Free 142.4MB)", color = Color.White)
                            }
                        }
                    }
                }
            }

            // 8. Help & Support Menu (FAQs included!)
            item {
                ModuleCard(
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    title = "❓ Help & Support Center",
                    subtitle = "Frequently asked questions and client feedback",
                    isExpanded = expandedHelp,
                    onToggle = { expandedHelp = !expandedHelp }
                ) {
                    var userSupportMessage by remember { mutableStateOf("") }
                    var isSubmittingMessage by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("FAQ 💡", color = ConvoColors.TextPrimary, fontWeight = FontWeight.Bold)
                        
                        FaqExpandableRow(
                            q = "How do I trigger an audio or video call?",
                            ans = "Simply select any buddy on your primary Chat tab or Contacts list, click to open the chat log, then select the phone/video icons at the top right header panel."
                        )

                        FaqExpandableRow(
                            q = "Does CircleChat encrypt messages?",
                            ans = "Yes! All media packages and texts sent inside chats are locked securely with full client-side simulated RSA credentials."
                        )

                        Divider(color = ConvoColors.BorderSlate, thickness = 1.dp)

                        Text("Send Message to Admin", color = ConvoColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        OutlinedTextField(
                            value = userSupportMessage,
                            onValueChange = { userSupportMessage = it },
                            placeholder = { Text("Write your query to its.me.calloftamim@gmail.com", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isSubmittingMessage = true
                                    delay(1000)
                                    isSubmittingMessage = false
                                    Toast.makeText(context, "Query submitted to admin Tamim!", Toast.LENGTH_SHORT).show()
                                    userSupportMessage = ""
                                }
                            },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(containerColor = ConvoColors.ElectricBlue)
                        ) {
                            if (isSubmittingMessage) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                            } else {
                                Text("Submit", color = Color.White)
                            }
                        }
                    }
                }
            }

            // 9. Premium Log Out Section
            item {
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showLogoutWarning = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC3545)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout from CircleChat", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Modal Preset Avatar selector bottom overlay dialog!
    if (showAvatarSelector) {
        AlertDialog(
            onDismissRequest = { showAvatarSelector = false },
            title = { Text("Select Avatar 📷", color = ConvoColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Select a custom illustrated profile caricature for your CircleChat display ID:", color = ConvoColors.TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PRESET_AVATARS.take(4).forEach { url ->
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(ConvoColors.HeaderBackground)
                                    .clickable {
                                        selectedAvatarUrl = url
                                        showAvatarSelector = false
                                        Toast.makeText(context, "Profile Picture updated!", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PRESET_AVATARS.takeLast(4).forEach { url ->
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(ConvoColors.HeaderBackground)
                                    .clickable {
                                        selectedAvatarUrl = url
                                        showAvatarSelector = false
                                        Toast.makeText(context, "Profile Picture updated!", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAvatarSelector = false }) {
                    Text("Cancel", color = ConvoColors.TextSecondary)
                }
            },
            containerColor = ConvoColors.CardBackground,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal Logout warning dialogue to match premium standards
    if (showLogoutWarning) {
        AlertDialog(
            onDismissRequest = { showLogoutWarning = false },
            title = { Text("Sure to Logout?", color = ConvoColors.TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text("Your credentials will be unlinked safely and local chats saved on the local device storage.", color = ConvoColors.TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutWarning = false
                        onBack()
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC3545))
                ) {
                    Text("Confirm Logout", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutWarning = false }) {
                    Text("Stay here", color = ConvoColors.TextSecondary)
                }
            },
            containerColor = ConvoColors.CardBackground,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ModuleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(containerColor = ConvoColors.CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(ConvoColors.HeaderBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = ConvoColors.ElectricBlue)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = ConvoColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(subtitle, color = ConvoColors.TextSecondary, fontSize = 12.sp)
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = ConvoColors.TextSecondary
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    content()
                }
            }
        }
    }
}

@Composable
fun ToggleSettingRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = ConvoColors.TextPrimary, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ConvoColors.ElectricBlue
            )
        )
    }
}

@Composable
fun FaqExpandableRow(q: String, ans: String) {
    var isExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(q, color = ConvoColors.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = ConvoColors.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            Text(
                text = ans,
                color = ConvoColors.TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                lineHeight = 16.sp
            )
        }
    }
}
