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

  private lateinit var viewModel: ChatViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    appContext = this.applicationContext
    
    // Add flags for Call Full Screen Intents
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
    } else {
        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }
    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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

    viewModel = androidx.lifecycle.ViewModelProvider(this)[ChatViewModel::class.java]

    // Handle initial intent
    viewModel.handleIntent(intent)

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme(dynamicColor = false) { // enforce our custom rich theme styling
        
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

              val isMinimized by viewModel.isVideoCallMinimized.collectAsStateWithLifecycle()

              // Fullscreen Overlay for Ringing or Connected States
              AnimatedVisibility(
                visible = callState !is CallState.Idle && !isMinimized,
                enter = fadeIn(),
                exit = fadeOut()
              ) {
                CallUiScreen(
                  viewModel = viewModel,
                  modifier = Modifier.fillMaxSize()
                )
              }

              // Draggable floating minimized call window for both participants
              if (callState is CallState.Connected && isMinimized) {
                com.example.ui.screens.MinimizedCallWindow(
                  state = callState as CallState.Connected,
                  viewModel = viewModel
                )
              }
            }
        }
      }
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
      super.onNewIntent(intent)
      setIntent(intent) // update the intent so LaunchedEffect sees it
      viewModel.handleIntent(intent)
  }
}

