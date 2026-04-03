package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            MyApplicationTheme(darkTheme = isDarkMode) {
                MainScreen(viewModel)
            }
        }
    }
}

sealed class Screen(val route: String, val icon: ImageVector, val label: String) {
    object Timer : Screen("timer", Icons.Default.Timer, "Timer")
    object Todo : Screen("todo", Icons.Default.List, "Todo")
    object Music : Screen("music", Icons.Default.MusicNote, "Focus")
    object Relax : Screen("relax", Icons.Default.SelfImprovement, "Relax")
    object Settings : Screen("settings", Icons.Default.Settings, "Settings")
}

@Composable
fun MainScreen(viewModel: TaskViewModel) {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Timer.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Timer.route) { TimerScreen(viewModel) }
            composable(Screen.Todo.route) { TodoScreen(viewModel) }
            composable(Screen.Music.route) { MusicScreen() }
            composable(Screen.Relax.route) { RelaxScreen() }
            composable(Screen.Settings.route) { SettingsScreen(viewModel) }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(Screen.Timer, Screen.Todo, Screen.Music, Screen.Relax, Screen.Settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        tonalElevation = 8.dp,
        modifier = Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF007AFF), // iOS Blue
                    selectedTextColor = Color(0xFF007AFF)
                )
            )
        }
    }
}

@Composable
fun TimerScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Focus Timer", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(40.dp))
        
        CartoonTimerAnimation(isRunning = tasks.any { it.isRunning })
        
        Spacer(modifier = Modifier.height(40.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(tasks, key = { it.id }) { task ->
                TaskCard(task, onToggle = { viewModel.toggleTask(task.id) })
            }
        }
    }
}

@Composable
fun CartoonTimerAnimation(isRunning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "timer")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(200.dp)
            .background(
                Brush.radialGradient(listOf(Color(0xFF007AFF).copy(alpha = 0.1f), Color.Transparent)),
                CircleShape
            )
    ) {
        Icon(
            imageVector = Icons.Default.Face,
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .rotate(if (isRunning) rotation else 0f)
                .padding(if (isRunning) (scale * 2).dp else 0.dp),
            tint = if (isRunning) Color(0xFF007AFF) else Color.Gray
        )
    }
}

@Composable
fun TaskCard(task: Task, onToggle: () -> Unit) {
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    if (task.isRunning) {
        LaunchedEffect(task.id) {
            while (true) {
                delay(1000)
                currentTimeMillis = System.currentTimeMillis()
            }
        }
    }

    val elapsedSinceStart = if (task.isRunning && task.lastStartTime != null) {
        currentTimeMillis - task.lastStartTime
    } else {
        0L
    }
    val totalDisplayTime = task.totalTimeMillis + elapsedSinceStart

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(task.name, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text(formatDuration(totalDisplayTime), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onToggle,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (task.isRunning) Color(0xFFFF3B30) else Color(0xFF34C759)
                )
            ) {
                Text(if (task.isRunning) "Stop" else "Start")
            }
        }
    }
}

@Composable
fun TodoScreen(viewModel: TaskViewModel) {
    var text by remember { mutableStateOf("") }
    val todos by viewModel.todos.collectAsState()
    
    Column(modifier = Modifier.padding(24.dp)) {
        Text("To-Do List", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        
        Row(modifier = Modifier.padding(vertical = 16.dp)) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("What needs to be done?") },
                colors = TextFieldDefaults.colors(unfocusedContainerColor = Color.Transparent)
            )
            IconButton(onClick = { viewModel.addTodo(text); text = "" }) {
                Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = Color(0xFF007AFF))
            }
        }
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(todos, key = { it.id }) { todo ->
                TodoItem(todo, 
                    onToggle = { viewModel.toggleTodo(todo.id) },
                    onDelete = { viewModel.removeTodo(todo.id) }
                )
            }
        }
    }
}

@Composable
fun TodoItem(todo: Todo, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = todo.isCompleted, onCheckedChange = { onToggle() })
        Text(
            text = todo.text,
            modifier = Modifier.weight(1f),
            style = if (todo.isCompleted) MaterialTheme.typography.bodyLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else MaterialTheme.typography.bodyLarge
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF3B30))
        }
    }
}

@Composable
fun MusicScreen() {
    val tracks = remember {
        listOf(
            Track(1, "Deep Focus", "Nature"),
            Track(2, "Ambient Study", "Lo-Fi"),
            Track(3, "Binaural Beats", "Zen"),
            Track(4, "Rainy Night", "Atmosphere"),
            Track(5, "Ocean Waves", "Nature"),
            Track(6, "Library Silence", "Study"),
            Track(7, "Alpha Waves", "Neuro"),
            Track(8, "Soft Piano", "Relax"),
            Track(9, "White Noise", "Focus"),
            Track(10, "Morning Dew", "Calm")
        )
    }
    
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
                    Text("Now Playing: Deep Focus", color = Color.White, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {}) { Icon(Icons.Default.SkipPrevious, "", tint = Color.White) }
                        IconButton(onClick = {}, modifier = Modifier.size(64.dp)) { Icon(Icons.Default.PlayArrow, "", tint = Color.White, modifier = Modifier.size(48.dp)) }
                        IconButton(onClick = {}) { Icon(Icons.Default.SkipNext, "", tint = Color.White) }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn {
            items(tracks) { track ->
                ListItem(
                    headlineContent = { Text(track.title) },
                    supportingContent = { Text(track.artist) },
                    trailingContent = { Icon(Icons.Default.PlayArrow, null) }
                )
            }
        }
    }
}

@Composable
fun RelaxScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Relaxation Mode", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(40.dp))
        
        Box(
            modifier = Modifier.size(250.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text("Breath In... Out...", style = MaterialTheme.typography.headlineMedium)
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RelaxButton("Meditation", Icons.Default.SelfImprovement)
            RelaxButton("Yoga", Icons.Default.AccessibilityNew)
        }
    }
}

@Composable
fun RelaxButton(label: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = {},
            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(icon, null, modifier = Modifier.size(40.dp), tint = Color(0xFF007AFF))
        }
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun SettingsScreen(viewModel: TaskViewModel) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    
    Column(modifier = Modifier.padding(24.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        SettingsToggle("Dark Mode", isDarkMode) { viewModel.setDarkMode(it) }
        SettingsToggle("Notifications", true) { }
        SettingsToggle("Sound Effects", true) { }
        SettingsToggle("iCloud Sync", false) { }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Account", color = Color.Gray, fontWeight = FontWeight.Bold)
        ListItem(headlineContent = { Text("Profile") }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
        ListItem(headlineContent = { Text("Subscription") }, trailingContent = { Icon(Icons.Default.ChevronRight, null) })
    }
}

@Composable
fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}
