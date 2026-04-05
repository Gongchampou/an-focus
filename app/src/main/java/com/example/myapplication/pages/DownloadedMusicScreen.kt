package com.example.myapplication.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import android.content.ComponentName
import androidx.media3.common.Player
import java.io.File
import com.example.myapplication.PlaybackService
import com.example.myapplication.Track
import com.google.common.util.concurrent.MoreExecutors
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * DOWNLOADED MUSIC SCREEN
 * This page is like a "File Manager" specifically for your music.
 * It shows only the songs you've downloaded and lets you delete them to save space.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadedMusicScreen(onBack: () -> Unit) {
    
    // --- APP TOOLS ---
    val context = LocalContext.current // Helps the app find the hidden "music" folder on your phone
    val scope = rememberCoroutineScope() // Allows us to do work in the background
    
    // --- DATA HOLDERS ---
    val controllerFuture = remember {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        MediaController.Builder(context, sessionToken).buildAsync()
    }
    var mediaController by remember { mutableStateOf<MediaController?>(null) }

    var allTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var downloadedTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Connects the screen to the background player "engine"
    LaunchedEffect(controllerFuture) {
        controllerFuture.addListener({
            mediaController = controllerFuture.get()
        }, MoreExecutors.directExecutor())
    }


    // Safely disconnects when you leave the page to save battery
    DisposableEffect(Unit) {
        onDispose {
            MediaController.releaseFuture(controllerFuture)
        }
    }
    
    // --- FILE HELPERS ---
    // Logic to name the files correctly (e.g., "track_1.mp3")
    fun getFileName(track: Track): String {
        return if (track.url.isBlank()) {
            track.fileName
        } else {
            "track_${track.id}.mp3"
        }
    }

    /**
     * loadTracks: This is the "Audit" function. 
     * It runs in the background to avoid slowing down the UI.
     */
    fun loadTracks() {
        scope.launch(Dispatchers.IO) {
            try {
                // 1. Open the master list of all possible songs
                val jsonString = context.assets.open("music_list.json").bufferedReader().use { it.readText() }
                val type = object : TypeToken<List<Track>>() {}.type
                val allTracksList: List<Track> = Gson().fromJson(jsonString, type)
                
                // 2. Find the folder where music is stored
                val musicDir = File(context.filesDir, "music")
                if (!musicDir.exists()) musicDir.mkdirs()

                // 3. CLEANUP: Delete any files that are broken (0 bytes) or shouldn't be there
                val validFileNames = allTracksList.map { getFileName(it) }.toSet()
                musicDir.listFiles()?.forEach { file ->
                    if (!validFileNames.contains(file.name) || file.length() <= 0L) {
                        file.delete()
                    }
                }

                // 4. FILTER: Find which tracks are actually on the phone
                val filtered = allTracksList.filter { track ->
                    if (track.url.isBlank()) true // Local assets always count
                    else {
                        val file = File(musicDir, getFileName(track))
                        file.exists() && file.length() > 0
                    }
                }

                // 5. UPDATE UI: Switch back to the main thread to update the screen
                withContext(Dispatchers.Main) {
                    allTracks = allTracksList
                    downloadedTracks = filtered
                    isLoading = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    // Runs the "Audit" as soon as you open this page
    LaunchedEffect(Unit) {
        loadTracks()
    }

    // --- UI DESIGN SECTION ---
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            
            // TIP: Small back button since the header is gone
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }


            if (isLoading) {
                // Show a loading spinner while checking files
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (downloadedTracks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No downloaded music found.", color = Color.Gray)
                }
            } else {
                // THE LIST: Shows only downloaded songs
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(downloadedTracks) { track ->
                        // One individual song row
                        ListItem(
                            headlineContent = {
                                Text(
                                    track.title,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            supportingContent = {
                                Text(
                                    track.artist,
                                    color = Color.Unspecified
                                )
                            },
                            trailingContent = {
                                if (track.url.isNotBlank()) {
                                    // THE DELETE BUTTON (TRASH CAN) - Only for downloaded web tracks
                                    IconButton(onClick = {
                                        scope.launch {
                                            val controller = mediaController

                                            // SAFETY: If deleting the current song, try to play the next one first
                                            if (controller != null && controller.currentMediaItem?.mediaId == track.id.toString()) {
                                                if (controller.hasNextMediaItem()) {
                                                    controller.seekToNextMediaItem()
                                                    controller.play()
                                                } else {
                                                    controller.stop()
                                                }
                                            }

                                            // DELETE: Physically erase the file (Switch to IO for disk operations)
                                            withContext(Dispatchers.IO) {
                                                val file = File(File(context.filesDir, "music"), getFileName(track))
                                                if (file.exists()) {
                                                    file.delete()
                                                }
                                            }
                                            
                                            // REFRESH: Run the audit again to update the UI
                                            loadTracks()
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                    }
                                } else {
                                    // Local asset icon
                                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF4CAF50))
                                }
                            }
                        )
                        HorizontalDivider() // Draws a thin line between songs
                    }
                }
            }
        }
    }
}
