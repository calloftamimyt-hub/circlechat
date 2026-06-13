package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: ChatViewModel, modifier: Modifier = Modifier) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Simple Logo Representation
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(ConvoColors.ElectricBlue),
            contentAlignment = Alignment.Center
        ) {
            Text("C", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = if (isSignUp) "Create Account" else "Welcome Back",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = ConvoColors.TextPrimary
        )
        Text(
            text = if (isSignUp) "Sign up with your mobile number" else "Log in with your mobile number",
            fontSize = 14.sp,
            color = ConvoColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Mobile Number") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ConvoColors.TextPrimary,
                unfocusedTextColor = ConvoColors.TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ConvoColors.TextPrimary,
                unfocusedTextColor = ConvoColors.TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (message.isNotEmpty()) {
            Text(text = message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                if (phone.isNotBlank() && password.isNotBlank()) {
                    isLoading = true
                    message = ""
                    if (isSignUp) {
                        viewModel.signUpWithPhone(phone, password) { success, msg ->
                            isLoading = false
                            if (!success) message = msg
                        }
                    } else {
                        viewModel.signInWithPhone(phone, password) { success, msg ->
                            isLoading = false
                            if (!success) message = msg
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
                Text(text = if (isSignUp) "Sign Up" else "Log In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { 
            isSignUp = !isSignUp
            message = ""
        }) {
            Text(
                text = if (isSignUp) "Already have an account? Log In" else "Don't have an account? Sign Up",
                color = ConvoColors.ElectricBlue
            )
        }
    }
}
