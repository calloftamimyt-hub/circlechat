package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactFullScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var phoneInput by remember { mutableStateOf("") }
    var searchMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ConvoColors.DarkSlateBackground,
        topBar = {
            TopAppBar(
                title = { Text("Add Contact", color = ConvoColors.TextPrimary, fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Find people on CircleChat",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = ConvoColors.TextPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Enter a mobile number to connect via Firebase real-time Sync.",
                fontSize = 14.sp,
                color = ConvoColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = phoneInput,
                onValueChange = { phoneInput = it },
                placeholder = { Text("Mobile Number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ConvoColors.TextPrimary,
                    unfocusedTextColor = ConvoColors.TextPrimary,
                    cursorColor = ConvoColors.ElectricBlue,
                    focusedBorderColor = ConvoColors.ElectricBlue,
                    unfocusedBorderColor = ConvoColors.BorderSlate,
                    focusedContainerColor = ConvoColors.InputBackground,
                    unfocusedContainerColor = ConvoColors.InputBackground
                ),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = ConvoColors.TextSecondary)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (searchMessage.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ConvoColors.CardBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ConvoColors.BorderSlate, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(searchMessage, color = ConvoColors.TextPrimary, fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        
                        if (searchMessage.contains("Not found")) {
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                                    intent.data = android.net.Uri.parse("sms:$phoneInput")
                                    intent.putExtra("sms_body", "Let's chat! Download Convo App.")
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        searchMessage = "Could not open SMS app."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ConvoColors.ElectricBlue)
                            ) {
                                Text("Invite with SMS", color = Color.White)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            Button(
                onClick = {
                    if (phoneInput.isNotBlank()) {
                        isLoading = true
                        searchMessage = "Searching..."
                        viewModel.connectFriendByPhone(phoneInput) { success ->
                            isLoading = false
                            if (success) {
                                onBack() // Go back to Home Screen
                            } else {
                                searchMessage = "No account found with this number. You can invite them via SMS."
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ConvoColors.ElectricBlue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Search", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
