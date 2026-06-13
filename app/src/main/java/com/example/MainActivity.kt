package com.example

import android.os.Build
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CallUiScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CallState
import com.example.ui.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {
  companion object {
      lateinit var appContext: android.content.Context
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    appContext = this.applicationContext

    val permissionsToRequest = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    val missingPermissions = permissionsToRequest.filter {
        ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
    }

    if (missingPermissions.isNotEmpty()) {
        ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 101)
    }

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme(dynamicColor = false) { // enforce our custom rich theme styling
        val viewModel: ChatViewModel = viewModel()
        
        val activeContactId by viewModel.activeContactId.collectAsStateWithLifecycle()
        val callState by viewModel.callState.collectAsStateWithLifecycle()
        val isFirebaseActive by viewModel.isFirebaseActive.collectAsStateWithLifecycle()
        
        val showProfile = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

        com.example.ui.screens.GlassBackground(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = androidx.compose.ui.graphics.Color.Transparent
            ) { innerPadding ->
      // Base orchestrator Layout
      if (!isFirebaseActive) {
          com.example.ui.screens.LoginScreen(
              viewModel = viewModel,
              modifier = Modifier.padding(innerPadding)
          )
      } else if (showProfile.value) {
          com.example.ui.screens.ProfileScreen(
              viewModel = viewModel,
              onBack = { showProfile.value = false },
              modifier = Modifier.padding(innerPadding)
          )
      } else if (activeContactId == null) {
        HomeScreen(
          viewModel = viewModel,
          onProfileClick = { showProfile.value = true },
          modifier = Modifier
        )
      } else {
        ChatScreen(
          viewModel = viewModel,
          modifier = Modifier
        )
      }

              // Fullscreen Overlay for Ringing or Connected States
              AnimatedVisibility(
                visible = callState !is CallState.Idle,
                enter = fadeIn(),
                exit = fadeOut()
              ) {
                CallUiScreen(
                  viewModel = viewModel,
                  modifier = Modifier.fillMaxSize()
                )
              }
            }
        }
      }
    }
  }
}

