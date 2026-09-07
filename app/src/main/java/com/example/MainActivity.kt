package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.MainScreen
import com.example.ui.image.ImageScreen
import com.example.ui.theme.DarkBg
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.video.VideoScreen

enum class AppScreen {
  MAIN,
  IMAGE,
  VIDEO
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = DarkBg
        ) {
          CleanMarkApp()
        }
      }
    }
  }
}

@Composable
fun CleanMarkApp() {
  var currentScreen by remember { mutableStateOf(AppScreen.MAIN) }

  BackHandler(enabled = currentScreen != AppScreen.MAIN) {
    currentScreen = AppScreen.MAIN
  }

  when (currentScreen) {
    AppScreen.MAIN -> {
      MainScreen(
        onNavigateToImage = { currentScreen = AppScreen.IMAGE },
        onNavigateToVideo = { currentScreen = AppScreen.VIDEO }
      )
    }
    AppScreen.IMAGE -> {
      ImageScreen(
        onBack = { currentScreen = AppScreen.MAIN }
      )
    }
    AppScreen.VIDEO -> {
      VideoScreen(
        onBack = { currentScreen = AppScreen.MAIN }
      )
    }
  }
}

