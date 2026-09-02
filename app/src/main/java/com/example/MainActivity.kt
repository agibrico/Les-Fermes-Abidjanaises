package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.components.ConnectivityBanner
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FarmViewModel
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize TTS
        tts = TextToSpeech(this, this)

        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                val viewModel: FarmViewModel = viewModel()
                val currentUser by viewModel.currentUser.collectAsState()
                val users by viewModel.allUsers.collectAsState()

                // Initialize the secure embedded API server
                DisposableEffect(viewModel) {
                    val server = com.example.util.EmbeddedOrderServer(viewModel, 8080, "FarmSecureToken2026")
                    server.start()
                    onDispose {
                        server.stop()
                    }
                }

                // State to track current active navigation role
                var activeRoleScreen by remember { mutableStateOf<String?>(null) }

                // Collect speech events from ViewModel
                LaunchedEffect(Unit) {
                    viewModel.speechEvents.collectLatest { text ->
                        speak(text)
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Global top connectivity and synchronization banner
                        ConnectivityBanner(viewModel = viewModel)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        ) {
                            if ((currentUser == null && activeRoleScreen != "CLIENT") || activeRoleScreen == null) {
                                WelcomeScreen(
                                    viewModel = viewModel,
                                    users = users,
                                    onRoleSelected = { role ->
                                        activeRoleScreen = role
                                    }
                                )
                            } else {
                                when (activeRoleScreen) {
                                    "CLIENT" -> ClientScreen(
                                        viewModel = viewModel,
                                        onBack = {
                                            activeRoleScreen = null
                                        }
                                    )
                                    "ADMINISTRATEUR" -> AdminScreen(
                                        viewModel = viewModel,
                                        onLogout = {
                                            viewModel.logout()
                                            activeRoleScreen = null
                                        }
                                    )
                                    "PARTENAIRE" -> PartnerScreen(
                                        viewModel = viewModel,
                                        onLogout = {
                                            viewModel.logout()
                                            activeRoleScreen = null
                                        }
                                    )
                                    "VOLAILLER" -> VolaillerScreen(
                                        viewModel = viewModel,
                                        onLogout = {
                                            viewModel.logout()
                                            activeRoleScreen = null
                                        }
                                    )
                                    "VENDEUR" -> VendeurScreen(
                                        viewModel = viewModel,
                                        onLogout = {
                                            viewModel.logout()
                                            activeRoleScreen = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // TTS INIT Callback
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.FRENCH)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Language French is not supported on this device.")
                isTtsReady = false
            } else {
                isTtsReady = true
                Log.d("TTS", "TTS Engine successfully initialized in French.")
            }
        } else {
            Log.e("TTS", "Initialization failed.")
            isTtsReady = false
        }
    }

    private fun speak(text: String) {
        if (isTtsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "FarmTTSID")
        } else {
            Toast.makeText(this, text, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        if (tts != null) {
            tts?.stop()
            tts?.shutdown()
        }
        super.onDestroy()
    }

}
