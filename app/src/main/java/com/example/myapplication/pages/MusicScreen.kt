package com.example.myapplication.pages

import android.content.ComponentName
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
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

import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.myapplication.Screen

/**
 * MUSIC SCREEN
 * This is the page where you see and play your downloaded music.
 * Think of this as your "Library" or "Player" page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(navController: NavController) {
    
    // --- APP SMART TOOLS (Don't worry too much about these) ---
    val context = LocalContext.current // Helps the app find files on your phone
    val lifecycleOwner = LocalLifecycleOwner.current // Tells the app if the screen is open or hidden
    
    // --- DATA HOLDERS (These store information while the app is running) ---
    // 'remember' makes the app keep this info even if the screen rotates or updates.
    val allTracks = remember { mutableStateListOf<Track>() } // A list of every song the app knows about
    var currentTrack by remember { mutableStateOf<Track?>(null) } // The song that is currently selected
    var isPlaying by remember { mutableStateOf(false) } // Is the music playing right now? (True or False)
    
    // --- CATEGORIES (THE TABS) ---
    // HOW TO CHANGE: You can add more words inside these quotes to add new tabs!
    val categories = listOf("All", "Study", "Sleep", "Relaxation", "Work", "Focus")
    
    // SVG Backgrounds for each category
    // CHANGE: You can paste your SVG code here for each category!
    val categoryBackgrounds = remember {
        mapOf(
            "Study" to """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600" width="400" height="600"><defs><linearGradient id="skyGrad" x1="0%" y1="0%" x2="0%" y2="100%"><stop offset="0%" stop-color="#b8e0d2" /><stop offset="100%" stop-color="#e8f4f8" /></linearGradient><linearGradient id="oceanGrad1" x1="0%" y1="0%" x2="0%" y2="100%"><stop offset="0%" stop-color="#00a8e8" /><stop offset="100%" stop-color="#006994" /></linearGradient><linearGradient id="oceanGrad2" x1="0%" y1="0%" x2="0%" y2="100%"><stop offset="0%" stop-color="#0096d6" /><stop offset="100%" stop-color="#005b82" /></linearGradient><path id="wave" d="M 0 0 Q 100 15 200 0 T 400 0 T 600 0 T 800 0 T 1000 0 T 1200 0 T 1400 0 T 1600 0 L 1600 200 L 0 200 Z" /><path id="leaf" d="M 0 0 Q 30 -20 80 0 Q 30 20 0 0" fill="#5c7a3b" /><path id="leaf-light" d="M 0 0 Q 30 -20 80 0 Q 30 20 0 0" fill="#759c44" /></defs><rect width="800" height="600" fill="url(#skyGrad)" /><path d="M 0 420 Q 150 390 300 420 T 600 420 L 800 420 L 800 600 L 0 600 Z" fill="#3b5930" /><rect x="0" y="440" width="800" height="160" fill="#005b82" /><g opacity="0.8"><use href="#wave" x="0" y="440" fill="url(#oceanGrad2)"><animate attributeName="x" from="0" to="-800" dur="7s" repeatCount="indefinite" /></use></g><g><use href="#wave" x="0" y="450" fill="white" opacity="0.6"><animate attributeName="x" from="-400" to="-1200" dur="5s" repeatCount="indefinite" /></use><use href="#wave" x="0" y="455" fill="url(#oceanGrad1)"><animate attributeName="x" from="-400" to="-1200" dur="5s" repeatCount="indefinite" /></use></g><g><use href="#wave" x="0" y="470" fill="white" opacity="0.8"><animate attributeName="x" from="0" to="-800" dur="3.5s" repeatCount="indefinite" /></use><use href="#wave" x="0" y="475" fill="url(#oceanGrad1)"><animate attributeName="x" from="0" to="-800" dur="3.5s" repeatCount="indefinite" /></use></g><path d="M 0 490 Q 150 480 300 520 T 700 540 Q 750 550 800 530 L 800 600 L 0 600 Z" fill="#e3b778" /><path d="M 0 530 Q 100 520 250 550 T 600 580 L 800 570 L 800 600 L 0 600 Z" fill="#d4a35e" /><g><path d="M 80 600 Q 110 380 200 200" stroke="#b06c49" stroke-width="20" fill="none" stroke-linecap="round" /><g transform="translate(200, 200)"><animateTransform attributeName="transform" type="rotate" values="0; 3; 0; -2; 0" dur="6s" repeatCount="indefinite" additive="sum" /><use href="#leaf" transform="rotate(-150)" /><use href="#leaf-light" transform="rotate(-110)" /><use href="#leaf" transform="rotate(-70)" /><use href="#leaf-light" transform="rotate(-30)" /><use href="#leaf" transform="rotate(10)" /><use href="#leaf-light" transform="rotate(50)" /><use href="#leaf" transform="rotate(90)" /></g></g><g><path d="M 30 600 Q 50 350 140 150" stroke="#94583a" stroke-width="24" fill="none" stroke-linecap="round" /><g transform="translate(140, 150)"><animateTransform attributeName="transform" type="rotate" values="0; -4; 0; 3; 0" dur="5s" repeatCount="indefinite" additive="sum" /><use href="#leaf" transform="scale(1.2) rotate(-160)" /><use href="#leaf-light" transform="scale(1.3) rotate(-120)" /><use href="#leaf" transform="scale(1.2) rotate(-80)" /><use href="#leaf-light" transform="scale(1.4) rotate(-40)" /><use href="#leaf" transform="scale(1.3) rotate(0)" /><use href="#leaf-light" transform="scale(1.2) rotate(40)" /><use href="#leaf" transform="scale(1.1) rotate(80)" /><use href="#leaf-light" transform="scale(1.1) rotate(120)" /></g></g></svg>""",
            "Sleep" to """<svg viewBox="0 0 800 500" xmlns="http://www.w3.org/2000/svg"><!-- Definitions --><defs><!-- Night sky gradient --><linearGradient id="nightSky" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#0b1d3a"/><stop offset="100%" stop-color="#1c355e"/></linearGradient><!-- Glow effect --><radialGradient id="moonGlow"><stop offset="0%" stop-color="#ffffff" stop-opacity="0.9"/><stop offset="100%" stop-color="#ffffff" stop-opacity="0"/></radialGradient></defs><!-- Background --><rect width="800" height="500" fill="url(#nightSky)"/><!-- Moon glow --><circle cx="650" cy="100" r="80" fill="url(#moonGlow)"/><!-- Moon --><circle cx="650" cy="100" r="40" fill="#f5f3ce"><animate attributeName="opacity" values="0.95;1;0.95" dur="6s" repeatCount="indefinite"/></circle><!-- Stars --><g fill="#ffffff"><circle cx="100" cy="80" r="2"><animate attributeName="opacity" values="1;0.3;1" dur="4s" repeatCount="indefinite"/></circle><circle cx="200" cy="50" r="1.5"><animate attributeName="opacity" values="0.8;0.2;0.8" dur="5s" repeatCount="indefinite"/></circle><circle cx="300" cy="120" r="2"><animate attributeName="opacity" values="1;0.4;1" dur="6s" repeatCount="indefinite"/></circle><circle cx="500" cy="60" r="1.5"><animate attributeName="opacity" values="0.9;0.3;0.9" dur="5s" repeatCount="indefinite"/></circle><circle cx="720" cy="180" r="2"><animate attributeName="opacity" values="1;0.5;1" dur="7s" repeatCount="indefinite"/></circle></g><!-- Floating clouds --><g fill="#ffffff" opacity="0.08"><ellipse cx="200" cy="200" rx="120" ry="40"><animateTransform attributeName="transform" type="translate" values="0 0; 40 0; 0 0" dur="20s" repeatCount="indefinite"/></ellipse><ellipse cx="500" cy="250" rx="150" ry="50"><animateTransform attributeName="transform" type="translate" values="0 0; -50 0; 0 0" dur="25s" repeatCount="indefinite"/></ellipse></g><!-- Hills --><path d="M0 350 Q200 300 400 350 T800 350 V500 H0 Z" fill="#0a1a33"/><path d="M0 400 Q300 350 800 400 V500 H0 Z" fill="#081426"/></svg>""",
            "Relaxation" to """<svg viewBox="0 0 400 600"><rect width="400" height="600" fill="#FFF3E0"/><path d="M0 300 Q200 100 400 300" fill="#FFE0B2" opacity="0.5"/></svg>""",
            "Work" to """<svg viewBox="0 0 400 600"><rect width="400" height="600" fill="#ECEFF1"/><rect x="50" y="50" width="300" height="500" fill="#CFD8DC" opacity="0.3"/></svg>""",
            "Focus" to """<svg viewBox="0 0 400 600"><rect width="400" height="600" fill="#F3E5F5"/><circle cx="200" cy="300" r="100" fill="none" stroke="#E1BEE7" stroke-width="5"/></svg>"""
        )
    }

    val imageLoader = remember {
        ImageLoader.Builder(context).components { add(SvgDecoder.Factory()) }.build()
    }
    
    // Tracks which tab you clicked on. Default is "All".
    var selectedCategory by remember { mutableStateOf("All") }
    
    // A simple trigger to refresh the list when you come back from the download page
    var refreshTrigger by remember { mutableStateOf(0) }

    // --- MUSIC PLAYER ENGINE (Advanced Stuff) ---
    // This connects this screen to the 'PlaybackService' which plays music in the background.
    val controllerFuture = remember {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        MediaController.Builder(context, sessionToken).buildAsync()
    }
    var mediaController by remember { mutableStateOf<MediaController?>(null) }

    // This part handles the lifecycle. We've removed the auto-refresh on resume
    // to keep the UI consistent and prevent flickering.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ -> }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Connects the 'Play' button to the actual music engine
    LaunchedEffect(controllerFuture) {
        controllerFuture.addListener({
            mediaController = controllerFuture.get()
        }, MoreExecutors.directExecutor())
    }

    // Cleans up the connection when you leave the page to save battery
    DisposableEffect(Unit) {
        onDispose { MediaController.releaseFuture(controllerFuture) }
    }

    // Syncs the UI: If the music starts playing, the Play button changes to Pause automatically.
    DisposableEffect(mediaController) {
        val controller = mediaController ?: return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.containsAny(Player.EVENT_PLAY_WHEN_READY_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                    isPlaying = player.playWhenReady
                    val mediaId = player.currentMediaItem?.mediaId
                    if (mediaId != null) {
                        val track = allTracks.find { it.id.toString() == mediaId }
                        if (track != null) currentTrack = track
                    }
                }
            }
        }
        controller.addListener(listener)
        isPlaying = controller.playWhenReady
        controller.currentMediaItem?.mediaId?.let { id ->
            currentTrack = allTracks.find { it.id.toString() == id }
        }
        onDispose { controller.removeListener(listener) }
    }

    // --- FILE HELPERS ---
    // Finds where the music file is stored on your phone.
    fun getFileName(track: Track): String {
        return if (track.url.isBlank()) track.fileName else "track_${track.id}.mp3"
    }

    // Checks if the music file actually exists (so we don't try to play a missing file).
    fun isDownloaded(track: Track): Boolean {
        if (track.url.isBlank()) return true 
        val file = File(File(context.filesDir, "music"), getFileName(track))
        return file.exists() && file.length() > 0
    }

    // Filter logic: Only shows songs that belong to the category you selected.
    val visibleTracks = remember(allTracks.size, refreshTrigger) { allTracks.filter { isDownloaded(it) } }
    val filteredTracks = remember(selectedCategory, visibleTracks) {
        if (selectedCategory == "All") visibleTracks else visibleTracks.filter { it.category == selectedCategory }
    }

    // --- PLAYBACK LOGIC ---
    
    // 1. Sync the player's queue with all downloaded tracks (only when the list changes)
    // We use the IDs of the tracks to detect actual changes and avoid unnecessary refreshes.
    val trackIds = remember(visibleTracks) { visibleTracks.map { it.id } }
    LaunchedEffect(mediaController, trackIds) {
        val controller = mediaController ?: return@LaunchedEffect
        val currentMediaIds = (0 until controller.mediaItemCount).map { controller.getMediaItemAt(it).mediaId }
        val newMediaIds = visibleTracks.map { it.id.toString() }
        
        if (currentMediaIds != newMediaIds) {
            val playlist = visibleTracks.map { t ->
                val uri = if (t.url.isNotBlank()) {
                    val f = File(File(context.filesDir, "music"), getFileName(t))
                    if (f.exists() && f.length() > 0) f.toURI().toString() else t.url
                } else {
                    "asset:///music/${t.fileName}"
                }
                val metadata = MediaMetadata.Builder()
                    .setTitle(t.title)
                    .setArtist(t.artist)
                    .setDisplayTitle(t.title)
                    .build()
                MediaItem.Builder()
                    .setUri(uri)
                    .setMediaId(t.id.toString())
                    .setMediaMetadata(metadata)
                    .build()
            }
            
            // SMART SYNC: Update the list but stay on the same song at the same second
            val currentId = controller.currentMediaItem?.mediaId
            val currentIndex = visibleTracks.indexOfFirst { it.id.toString() == currentId }
            
            if (currentIndex != -1) {
                controller.setMediaItems(playlist, currentIndex, controller.currentPosition)
            } else {
                controller.setMediaItems(playlist)
            }
            
            controller.repeatMode = Player.REPEAT_MODE_ALL
            controller.prepare()
        }
    }

    // REMOVED the old "Consolidated Control" LaunchedEffect that was causing resets.
    // Playback is now handled directly by button clicks for better stability.

    // --- LOADING THE LIST ---
    // Reads your 'music_list.json' file once when the screen starts.
    LaunchedEffect(Unit) {
        if (allTracks.isNotEmpty()) return@LaunchedEffect
        try {
            val jsonString = context.assets.open("music_list.json").bufferedReader().use { it.readText() }
            val loadedTracks: List<Track> = Gson().fromJson(jsonString, object : TypeToken<List<Track>>() {}.type)
            allTracks.addAll(loadedTracks)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Sync currentTrack with what's actually playing in the mediaController
    // This runs as soon as the controller is ready or the tracks are loaded
    LaunchedEffect(mediaController, allTracks.size) {
        val controller = mediaController ?: return@LaunchedEffect
        if (allTracks.isEmpty()) return@LaunchedEffect

        val playingMediaId = controller.currentMediaItem?.mediaId
        if (playingMediaId != null) {
            val playingTrack = allTracks.find { it.id.toString() == playingMediaId }
            if (playingTrack != null) {
                currentTrack = playingTrack
                isPlaying = controller.playWhenReady
            }
        } else if (currentTrack == null) {
            // Only set a default if nothing is already playing
            currentTrack = allTracks.firstOrNull { isDownloaded(it) }
        }
    }



    // --- UI DESIGN SECTION (This is where you change how things LOOK) ---
    // -----------------------------------------------------------------------------------------

    // --- UI DESIGN SECTION (This is where you change how things LOOK) ---
    // -----------------------------------------------------------------------------------------
    
    Column(modifier = Modifier.padding(24.dp)) {

        // --- THE HEADER (TOP ROW) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // HOW TO CHANGE: Change the text inside " " to rename your screen.
            Text("Focus Music", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            
            // This is the little Download icon button.
            IconButton(onClick = { navController.navigate(Screen.OnlineMusic.route) }) {
                Icon(Icons.Default.Download, "Online Library")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp)) // Adds a small gap
        
        // --- THE NOW PLAYING CARD (THE TRANSLUCENT BOX) ---
        Card(
            modifier = Modifier.fillMaxWidth().height(180.dp), // Increased height for trackline
            shape = RoundedCornerShape(20.dp), 
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // CATEGORY BACKGROUND
                currentTrack?.category?.let { cat ->
                    categoryBackgrounds[cat]?.let { svg ->
                        val painter = rememberAsyncImagePainter(
                            model = svg.toByteArray(),
                            imageLoader = imageLoader
                        )
                        Image(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.6f
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Now Playing: ${currentTrack?.title ?: "Select a track"}", 
                            color = MaterialTheme.colorScheme.onSurfaceVariant, 
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isPlaying) {
                            Spacer(Modifier.width(8.dp))
                            PlayingAnimation()
                        }
                    }

                    // --- PLAYER CONTROLS (Above tracking now) ---
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                        IconButton(onClick = { mediaController?.seekToPreviousMediaItem() }) { 
                            Icon(Icons.Default.SkipPrevious, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) 
                        }
                        
                        IconButton(onClick = { 
                            val controller = mediaController ?: return@IconButton
                            if (controller.playWhenReady) controller.pause() else controller.play()
                        }, modifier = Modifier.size(56.dp)) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                                null, 
                                tint = MaterialTheme.colorScheme.primary, 
                                modifier = Modifier.size(40.dp) 
                            ) 
                        }
                        
                        IconButton(onClick = { mediaController?.seekToNextMediaItem() }) { 
                            Icon(Icons.Default.SkipNext, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) 
                        }
                    }

                    // --- THE TRACKLINE (SLIDER) ---
                    if (currentTrack != null) {
                        PlaybackProgress(mediaController, isPlaying)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // --- CATEGORY ROW (THE FILTER TABS) ---
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories, key = { it }) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        // --- SONG LIST AREA ---
        if (visibleTracks.isEmpty()) {
            // This shows if you have NO music downloaded.
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No local tracks found.", color = Color.Gray)
                    Button(onClick = { navController.navigate(Screen.OnlineMusic.route) }) {
                        Text("Download from Online Library")
                    }
                }
            }
        } else {
            LazyColumn {
                items(filteredTracks, key = { it.id }) { track ->
                    // Individual Music Item in the list
                    ListItem(
                        headlineContent = { Text(track.title) },
                        supportingContent = { Text(track.artist) },
                        trailingContent = { 
                            IconButton(onClick = {
                                val controller = mediaController ?: return@IconButton
                                if (currentTrack?.id == track.id) {
                                    if (isPlaying) controller.pause() else controller.play()
                                } else {
                                    val index = visibleTracks.indexOfFirst { it.id == track.id }
                                    if (index != -1) {
                                        controller.seekTo(index, 0L)
                                        controller.play()
                                    }
                                }
                            }) {
                                Icon(if (currentTrack?.id == track.id && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * A small animated visualizer that shows when music is playing.
 */
@Composable
fun PlayingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "bars")
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.height(16.dp)
    ) {
        listOf(0.4f, 0.8f, 0.5f).forEach { initialHeight ->
            val height by infiniteTransition.animateFloat(
                initialValue = initialHeight,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "barHeight"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(height)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp))
            )
        }
    }
}

/**
 * A self-contained progress bar to prevent flickering on the main screen.
 */
@Composable
fun PlaybackProgress(mediaController: MediaController?, isPlaying: Boolean) {
    var currentPosition by remember { mutableStateOf(0L) }
    var totalDuration by remember { mutableStateOf(0L) }

    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    LaunchedEffect(mediaController, isPlaying) {
        val controller = mediaController ?: return@LaunchedEffect
        while (isPlaying) {
            currentPosition = controller.currentPosition
            totalDuration = controller.duration.coerceAtLeast(0L)
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Slider(
            value = if (totalDuration > 0) currentPosition.toFloat() / totalDuration.toFloat() else 0f,
            onValueChange = { ratio ->
                val newPos = (ratio * totalDuration).toLong()
                currentPosition = newPos
                mediaController?.seekTo(newPos)
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            ),
            modifier = Modifier.height(20.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(currentPosition), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatTime(totalDuration), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
