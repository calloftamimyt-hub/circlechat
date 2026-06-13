package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Contact
import com.example.data.model.Message
import com.example.ui.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val activeContact by viewModel.activeContact.collectAsStateWithLifecycle(initialValue = null)
    val messages by viewModel.activeMessages.collectAsStateWithLifecycle(initialValue = emptyList())
    val typedMessage by viewModel.typedMessage.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    // Smooth scroll to bottom when a new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val contact = activeContact ?: return

    Scaffold(
        modifier = modifier.background(Color.Transparent),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.selectContactChat(null) },
                        modifier = Modifier.testTag("chat_back_btn")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ConvoColors.TextPrimary)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ContactAvatar(contact = contact, size = 38.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = contact.name,
                                color = ConvoColors.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (contact.status == "Online") "Online" else contact.lastActive,
                                color = if (contact.status == "Online") ConvoColors.AccentMint else ConvoColors.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.startCall(contact, isVideo = false) },
                        modifier = Modifier.testTag("chat_audio_call")
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Audio Call", tint = ConvoColors.ElectricBlue)
                    }
                    IconButton(
                        onClick = { viewModel.startCall(contact, isVideo = true) },
                        modifier = Modifier.testTag("chat_video_call")
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = ConvoColors.ActiveVibe)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConvoColors.HeaderBackground,
                    titleContentColor = ConvoColors.TextPrimary,
                    navigationIconContentColor = ConvoColors.TextPrimary,
                    actionIconContentColor = ConvoColors.TextPrimary
                ),
                modifier = Modifier.border(1.dp, ConvoColors.BorderSlate)
            )
        },
        bottomBar = {
            Surface(
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() // crucial edge-to-edge guideline safeguard!
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ConvoColors.HeaderBackground)
                            .border(1.dp, ConvoColors.BorderSlate, CircleShape)
                            .clickable { /* Attach simulation */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Attach", tint = ConvoColors.TextPrimary, modifier = Modifier.size(20.dp))
                    }

                    Spacer(Modifier.width(6.dp))

                    TextField(
                        value = typedMessage,
                        onValueChange = { viewModel.updateTypedMessage(it) },
                        placeholder = { Text("Write a response...", color = ConvoColors.TextSecondary, fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                            .border(1.dp, ConvoColors.BorderGlassStrong, RoundedCornerShape(20.dp))
                            .testTag("message_input_field"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = ConvoColors.InputBackground,
                            unfocusedContainerColor = ConvoColors.InputBackground,
                            focusedTextColor = ConvoColors.TextPrimary,
                            unfocusedTextColor = ConvoColors.TextPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = { viewModel.sendMessage() }
                        )
                    )

                    Spacer(Modifier.width(6.dp))

                    FloatingActionButton(
                        onClick = { viewModel.sendMessage() },
                        containerColor = ConvoColors.ElectricBlue,
                        contentColor = Color.White,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("send_msg_btn"),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(18.dp), tint = Color.White)
                    }
                }
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                // Info Bio card listing
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ContactAvatar(contact = contact, size = 80.dp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = contact.name,
                            color = ConvoColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = contact.bio,
                            color = ConvoColors.TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 4.dp)
                        )
                    }
                }

                items(messages) { message ->
                    MessageRow(message = message)
                }
            }
        }
    }
}

@Composable
fun MessageRow(message: Message) {
    if (message.type != "Text") {
        // System message for Call completion logs, extremely polished!
        SystemCallLogCell(message = message)
        return
    }

    val isMe = message.isFromMe
    val alignment = if (isMe) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        val bubbleBg = if (isMe) ConvoColors.ElectricBlue else ConvoColors.CardBackground
        val textColor = if (isMe) Color.White else ConvoColors.TextPrimary
        val bubbleShape = if (isMe) {
            RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
        } else {
            RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(bubbleBg)
                .then(
                    if (isMe) Modifier else Modifier.border(1.dp, ConvoColors.BorderSlate, bubbleShape)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = message.content,
                    color = textColor,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(message.timestamp),
                        color = ConvoColors.TextSecondary.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                    if (isMe) {
                        Icon(
                            imageVector = if (message.status == "Read") Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = null,
                            tint = if (message.status == "Read") ConvoColors.AccentMint else ConvoColors.TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SystemCallLogCell(message: Message) {
    val isVideo = message.type == "VideoCall"
    val icon = if (isVideo) Icons.Default.Videocam else Icons.Default.Phone
    val iconColor = if (isVideo) ConvoColors.ActiveVibe else ConvoColors.ElectricBlue

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(ConvoColors.CardBackground)
                .border(1.dp, ConvoColors.BorderSlate, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = message.content,
                    color = ConvoColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
