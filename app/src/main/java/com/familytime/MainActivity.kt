package com.familytime

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import co.daily.CallClient
import co.daily.CallClientListener
import co.daily.model.Participant
import co.daily.model.ParticipantLeftReason
import co.daily.view.VideoView
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.i("FamilyTime", "Permissions granted: $permissions")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestPermissionLauncher.launch(arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        ))

        setContent {
            FamilyTimeApp()
        }
    }
}

@Composable
fun DailyVideoView(modifier: Modifier = Modifier, participant: Participant?) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            VideoView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { videoView ->
            videoView.track = participant?.media?.camera?.track
        }
    )
}

@Composable
fun FamilyTimeApp() {
    var isLoading by remember { mutableStateOf(true) }
    var isStartingCall by remember { mutableStateOf(false) }
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var selectedEmails by remember { mutableStateOf<Set<String>>(emptySet()) }
    var roomUrl by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Daily SDK State
    val context = LocalContext.current
    val callClient = remember { CallClient(context) }
    var localParticipant by remember { mutableStateOf<Participant?>(null) }
    var remoteParticipant by remember { mutableStateOf<Participant?>(null) }

    val scope = rememberCoroutineScope()

    // Listen to Daily events
    DisposableEffect(callClient) {
        val listener = object : CallClientListener {
            override fun onParticipantJoined(participant: Participant) {
                if (participant.info.isLocal) {
                    localParticipant = participant
                } else {
                    remoteParticipant = participant
                }
            }
            override fun onParticipantUpdated(participant: Participant) {
                if (participant.info.isLocal) {
                    localParticipant = participant
                } else {
                    remoteParticipant = participant
                }
            }
            override fun onParticipantLeft(participant: Participant, reason: ParticipantLeftReason) {
                if (!participant.info.isLocal) {
                    remoteParticipant = null
                }
            }
        }
        callClient.addListener(listener)
        onDispose {
            callClient.removeListener(listener)
            callClient.release()
        }
    }

    // Fetch contacts on mount
    LaunchedEffect(Unit) {
        try {
            contacts = Network.api.getContacts()
        } catch (e: Exception) {
            errorMessage = "Failed to load contacts: ${e.message}"
            Log.e("FamilyTime", "Error fetching contacts", e)
        } finally {
            isLoading = false
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when {
                roomUrl != null -> {
                    // Call UI (DailyVideoView rendering)
                    Row(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                        // Remote Video (Left side)
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            if (remoteParticipant != null) {
                                DailyVideoView(modifier = Modifier.fillMaxSize(), participant = remoteParticipant)
                            } else {
                                Text(
                                    "Waiting for family to join...", 
                                    color = Color.White, 
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                        
                        // Local Video & Controls (Right side)
                        Box(modifier = Modifier.weight(0.5f).fillMaxHeight().background(Color.DarkGray)) {
                            if (localParticipant != null) {
                                DailyVideoView(modifier = Modifier.fillMaxSize(), participant = localParticipant)
                            } else {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
                            }
                            
                            Button(
                                onClick = {
                                    callClient.leave()
                                    roomUrl = null
                                    localParticipant = null
                                    remoteParticipant = null
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(32.dp)
                            ) {
                                Text("End Call", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
                isLoading || isStartingCall -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isStartingCall) "Starting call and emailing family..." else "Loading contacts from Google Sheets...",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                errorMessage != null -> {
                    Text(errorMessage ?: "Unknown error", color = MaterialTheme.colorScheme.error)
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp)
                    ) {
                        Text("Select family members to invite:", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LazyColumn(
                            modifier = Modifier.weight(1f)
                        ) {
                            items(contacts) { contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val newSet = selectedEmails.toMutableSet()
                                            if (newSet.contains(contact.email)) {
                                                newSet.remove(contact.email)
                                            } else {
                                                newSet.add(contact.email)
                                            }
                                            selectedEmails = newSet
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectedEmails.contains(contact.email),
                                        onCheckedChange = null
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("${contact.name} (${contact.email})", style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Button(
                                onClick = { selectedEmails = contacts.map { it.email }.toSet() },
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text("Select All")
                            }
                            
                            Button(
                                onClick = {
                                    isStartingCall = true
                                    scope.launch {
                                        try {
                                            val response = Network.api.startCall(InviteRequest(selectedEmails.toList()))
                                            if (response.error != null) {
                                                errorMessage = response.error
                                            } else if (response.url != null) {
                                                roomUrl = response.url
                                                // Join the Daily call directly!
                                                callClient.join(url = response.url) { result ->
                                                    result.error?.let { err ->
                                                        Log.e("FamilyTime", "Failed to join: ${err.msg}")
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Failed to start call: ${e.message}"
                                            Log.e("FamilyTime", "Error starting call", e)
                                        } finally {
                                            isStartingCall = false
                                        }
                                    }
                                },
                                enabled = selectedEmails.isNotEmpty(),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text("Start Call (${selectedEmails.size})")
                            }
                        }
                    }
                }
            }
        }
    }
}
