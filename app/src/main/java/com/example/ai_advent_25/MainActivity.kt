package com.example.ai_advent_25

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ai_advent_25.ui.ChatScreen
import com.example.ai_advent_25.ui.SettingsScreen
import com.example.ai_advent_25.ui.TestingScreen
import com.example.ai_advent_25.ui.theme.AiAdvent25Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AiAdvent25Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf("chat") }
                    var apiKey by remember { mutableStateOf("") }
                    
                    when (currentScreen) {
                        "chat" -> {
                            ChatScreen(
                                apiKey = apiKey,
                                onApiKeySet = { key -> 
                                    apiKey = key 
                                },
                                onSettingsClick = { 
                                    currentScreen = "settings" 
                                },
                                onTestingClick = {
                                    currentScreen = "testing"
                                }
                            )
                        }
                        "settings" -> {
                            SettingsScreen(
                                apiKey = apiKey,
                                onBackPressed = { currentScreen = "chat" }
                            )
                        }
                        "testing" -> {
                            TestingScreen(
                                onBackPressed = { currentScreen = "chat" }
                            )
                        }
                    }
                }
            }
        }
    }
}
