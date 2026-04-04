package com.example.myapplication.pages

import android.content.ComponentName
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.myapplication.PlaybackService
import com.example.myapplication.Track
import com.google.common.util.concurrent.MoreExecutors
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

@Composable
fun MusicScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tracks = remember { mutableStateListOf<Track>() }
    var currentTrack by remember { mutableStateOf<Track?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    
    // Track download status
    val downloadedFiles = remember { mutableStateMapOf<String, Boolean>() }

    val controllerFuture = remember {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        MediaController.Builder(context, sessionToken).buildAsync()
    }
    var mediaController by remember { mutableStateOf<MediaController?>(null) }

    LaunchedEffect(controllerFuture) {
        controllerFuture.addListener({
            mediaController = controllerFuture.get()
        }, MoreExecutors.directExecutor())
    }

    DisposableEffect(Unit) {
        onDispose {
            MediaController.releaseFuture(controllerFuture)
        }
    }

    // Handle isPlaying state from controller
    DisposableEffect(mediaController) {
        val controller = mediaController ?: return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                isPlaying = isPlayingChanged
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val mediaId = mediaItem?.mediaId
                if (mediaId != null) {
                    val track = tracks.find { it.id.toString() == mediaId }
                    if (track != null && track != currentTrack) {
                        currentTrack = track
                    }
                }
            }
        }
        controller.addListener(listener)
        isPlaying = controller.isPlaying
        onDispose {
            controller.removeListener(listener)
        }
    }

    fun getFileName(track: Track): String {
        return if (track.url.isBlank()) {
            track.fileName
        } else {
            "track_${track.id}.mp3"
        }
    }

    fun isDownloaded(track: Track): Boolean {
        if (track.url.isBlank()) return true // Assets are "downloaded"
        return File(File(context.filesDir, "music"), getFileName(track)).exists()
    }

    suspend fun downloadTrack(track: Track): File? {
        if (track.url.isBlank()) return null
        val musicDir = File(context.filesDir, "music")
        if (!musicDir.exists()) musicDir.mkdirs()
        
        val fileName = getFileName(track)
        val file = File(musicDir, fileName)
        
        // If file already exists and is not empty, don't download again
        if (file.exists() && file.length() > 0) return file

        return withContext(Dispatchers.IO) {
            try {
                // Delete if exists but empty
                if (file.exists()) file.delete()
                
                URL(track.url).openConnection().apply {
                    connectTimeout = 10000
                    readTimeout = 10000
                }.getInputStream().use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Verify download
                if (file.exists() && file.length() > 0) {
                    downloadedFiles[track.id.toString()] = true
                    file
                } else {
                    file.delete()
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (file.exists()) file.delete()
                null
            }
        }
    }

    LaunchedEffect(currentTrack, mediaController) {
        val controller = mediaController ?: return@LaunchedEffect
        currentTrack?.let { track ->
            try {
                // If the controller is already playing this track, don't reset it
                if (controller.currentMediaItem?.mediaId == track.id.toString()) {
                    return@LaunchedEffect
                }

                val mediaItem = if (track.url.isNotBlank()) {
                    val fileName = getFileName(track)
                    val file = File(File(context.filesDir, "music"), fileName)
                    val uri = if (file.exists() && file.length() > 0) {
                        file.toURI().toString()
                    } else {
                        track.url
                    }
                    MediaItem.Builder()
                        .setUri(uri)
                        .setMediaId(track.id.toString())
                        .build()
                } else {
                    MediaItem.Builder()
                        .setUri("asset:///music/${track.fileName}")
                        .setMediaId(track.id.toString())
                        .build()
                }

                controller.setMediaItem(mediaItem)
                controller.prepare()
                if (isPlaying) {
                    controller.play()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(isPlaying, mediaController) {
        val controller = mediaController ?: return@LaunchedEffect
        if (isPlaying) {
            if (currentTrack == null && tracks.isNotEmpty()) {
                currentTrack = tracks[0]
            }
            if (controller.playbackState == Player.STATE_IDLE) {
                controller.prepare()
            }
            controller.play()
        } else {
            controller.pause()
        }
    }

    LaunchedEffect(Unit) {
        try {
            val jsonString = context.assets.open("music/music_list.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<Track>>() {}.type
            val loadedTracks: List<Track> = Gson().fromJson(jsonString, type)
            tracks.clear()
            tracks.addAll(loadedTracks)
            
            // Initialise download status
            loadedTracks.forEach { track ->
                downloadedFiles[track.id.toString()] = isDownloaded(track)
            }
            
            if (tracks.isNotEmpty()) {
                currentTrack = tracks[0]
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Deleted old listener effect as it's now handled by MediaController effect above

    
    Column(modifier = Modifier.padding(24.dp)) {
        Text("Deep Learning Music", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth().height(150.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF5856D6))
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Now Playing: ${currentTrack?.title ?: "Select a track"}", color = Color.White, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val currentIndex = tracks.indexOf(currentTrack)
                            if (currentIndex > 0) {
                                currentTrack = tracks[currentIndex - 1]
                            } else if (tracks.isNotEmpty()) {
                                currentTrack = tracks.last()
                            }
                        }) { Icon(Icons.Default.SkipPrevious, "", tint = Color.White) }
                        
                        IconButton(onClick = { isPlaying = !isPlaying }, modifier = Modifier.size(64.dp)) { 
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                                "", 
                                tint = Color.White, 
                                modifier = Modifier.size(48.dp)
                            ) 
                        }
                        
                        IconButton(onClick = {
                            val currentIndex = tracks.indexOf(currentTrack)
                            if (currentIndex < tracks.size - 1) {
                                currentTrack = tracks[currentIndex + 1]
                            } else if (tracks.isNotEmpty()) {
                                currentTrack = tracks.first()
                            }
                        }) { Icon(Icons.Default.SkipNext, "", tint = Color.White) }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn {
            items(tracks) { track ->
                val isDownloaded = downloadedFiles[track.id.toString()] ?: false
                ListItem(
                    headlineContent = { Text(track.title) },
                    supportingContent = { Text(track.artist) },
                    trailingContent = { 
                        Row {
                            if (track.url.isNotBlank()) {
                                IconButton(onClick = {
                                    if (!isDownloaded) {
                                        scope.launch {
                                            downloadTrack(track)
                                        }
                                    }
                                }) {
                                    Icon(
                                        if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                                        contentDescription = "Download",
                                        tint = if (isDownloaded) Color.Green else LocalContentColor.current
                                    )
                                }
                            }
                            
                            IconButton(onClick = {
                                if (currentTrack == track) {
                                    isPlaying = !isPlaying
                                } else {
                                    currentTrack = track
                                    isPlaying = true
                                }
                            }) {
                                Icon(
                                    if (currentTrack == track && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                                    null
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

