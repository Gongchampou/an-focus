package com.example.myapplication.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.Track
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadedMusicScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var downloadedTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    
    fun getFileName(track: Track): String {
        return if (track.url.isBlank()) {
            track.fileName
        } else {
            "track_${track.id}.mp3"
        }
    }

    fun loadDownloadedTracks() {
        try {
            val jsonString = context.assets.open("music/music_list.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<Track>>() {}.type
            val allTracks: List<Track> = Gson().fromJson(jsonString, type)
            val musicDir = File(context.filesDir, "music")
            downloadedTracks = allTracks.filter { track ->
                track.url.isNotBlank() && File(musicDir, getFileName(track)).exists()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(Unit) {
        loadDownloadedTracks()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloaded Music") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (downloadedTracks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No downloaded music found.", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    items(downloadedTracks) { track ->
                        ListItem(
                            headlineContent = { Text(track.title, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(track.artist) },
                            trailingContent = {
                                IconButton(onClick = {
                                    val file = File(File(context.filesDir, "music"), getFileName(track))
                                    if (file.exists()) {
                                        file.delete()
                                        loadDownloadedTracks()
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
