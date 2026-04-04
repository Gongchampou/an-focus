package com.example.myapplication

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val taskDao = db.taskDao()
    private val todoDao = db.todoDao()
    val settingsManager = SettingsManager(application)

    val tasks: StateFlow<List<Task>> = taskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todos: StateFlow<List<Todo>> = todoDao.getAllTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isDarkMode: StateFlow<Boolean> = settingsManager.darkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isNotificationsEnabled: StateFlow<Boolean> = settingsManager.notificationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isVibrationEnabled: StateFlow<Boolean> = settingsManager.vibrationFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isSoundEnabled: StateFlow<Boolean> = settingsManager.soundEffectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _characters = MutableStateFlow<List<FocusCharacter>>(emptyList())
    val characters = _characters.asStateFlow()

    init {
        loadCharacters()
    }

    private fun loadCharacters() {
        viewModelScope.launch {
            try {
                val jsonString = getApplication<Application>().assets.open("characters.json")
                    .bufferedReader().use { it.readText() }
                val listType = object : TypeToken<List<FocusCharacter>>() {}.type
                val loadedCharacters: List<FocusCharacter> = Gson().fromJson(jsonString, listType)
                _characters.value = loadedCharacters
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback or empty list
            }
        }
    }

    // Task logic
    fun addTask(name: String, hours: Int = 0, minutes: Int = 25, seconds: Int = 0, characterImageName: String = "study_default") {
        val millis = (hours * 3600 + minutes * 60 + seconds) * 1000L
        viewModelScope.launch {
            taskDao.insertTask(Task(
                name = name, 
                initialTimeMillis = millis, 
                remainingTimeMillis = millis,
                characterImageName = characterImageName
            ))
        }
    }

    fun removeTask(taskId: UUID) {
        viewModelScope.launch {
            taskDao.deleteTaskById(taskId)
        }
    }

    fun toggleTask(taskId: UUID) {
        viewModelScope.launch {
            val currentTasks = tasks.value
            val task = currentTasks.find { it.id == taskId }
            if (task != null) {
                val now = System.currentTimeMillis()
                val updatedTask = if (task.isRunning) {
                    val elapsedSinceStart = now - (task.lastStartTime ?: now)
                    val newRemaining = (task.remainingTimeMillis - elapsedSinceStart).coerceAtLeast(0)
                    stopTimerService()
                    task.copy(
                        isRunning = false,
                        remainingTimeMillis = newRemaining,
                        lastStartTime = null
                    )
                } else {
                    if (task.remainingTimeMillis <= 0) {
                        // Reset if it reached zero
                        startTimerService(task.name, task.initialTimeMillis)
                        task.copy(
                            isRunning = true,
                            remainingTimeMillis = task.initialTimeMillis,
                            lastStartTime = now
                        )
                    } else {
                        startTimerService(task.name, task.remainingTimeMillis)
                        task.copy(
                            isRunning = true,
                            lastStartTime = now
                        )
                    }
                }
                taskDao.insertTask(updatedTask)
            }
        }
    }

    fun updateTaskProgress(taskId: UUID, remaining: Long) {
        viewModelScope.launch {
            val task = tasks.value.find { it.id == taskId }
            if (task != null && task.isRunning) {
                if (remaining <= 0) {
                    toggleTask(taskId) // Stop it
                } else {
                    taskDao.insertTask(task.copy(remainingTimeMillis = remaining))
                }
            }
        }
    }

    fun onEnterBackground() {
        val runningTask = tasks.value.find { it.isRunning }
        if (runningTask != null) {
            val intent = Intent(getApplication(), TimerService::class.java).apply {
                action = "UPDATE_VISIBILITY"
                putExtra("IS_VISIBLE", false)
            }
            getApplication<Application>().startService(intent)
        }
    }

    fun onEnterForeground() {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = "UPDATE_VISIBILITY"
            putExtra("IS_VISIBLE", true)
        }
        getApplication<Application>().startService(intent)
    }

    private fun startTimerService(taskName: String, remainingTime: Long) {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = "START"
            putExtra("TASK_NAME", taskName)
            putExtra("REMAINING_TIME", remainingTime)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }

    private fun stopTimerService() {
        val intent = Intent(getApplication(), TimerService::class.java).apply {
            action = "STOP"
        }
        getApplication<Application>().stopService(intent)
        triggerAlert()
    }

    private fun triggerAlert() {
        viewModelScope.launch {
            if (settingsManager.vibrationFlow.first()) {
                vibrate()
            }
            if (settingsManager.soundEffectsFlow.first()) {
                playSound()
            }
        }
    }

    fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getApplication<Application>().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(500)
        }
    }

    fun playSound() {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(getApplication(), notification)
            r.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Todo logic
    fun addTodo(text: String) {
        if (text.isNotBlank()) {
            viewModelScope.launch {
                todoDao.insertTodo(Todo(text = text))
            }
        }
    }

    fun toggleTodo(todoId: UUID) {
        viewModelScope.launch {
            val currentTodos = todos.value
            val todo = currentTodos.find { it.id == todoId }
            if (todo != null) {
                todoDao.insertTodo(todo.copy(isCompleted = !todo.isCompleted))
            }
        }
    }

    fun removeTodo(todoId: UUID) {
        viewModelScope.launch {
            val currentTodos = todos.value
            val todo = currentTodos.find { it.id == todoId }
            if (todo != null) {
                todoDao.deleteTodo(todo)
            }
        }
    }

    // Settings logic
    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setDarkMode(enabled)
        }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setNotifications(enabled)
        }
    }

    fun setVibration(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setVibration(enabled)
        }
    }

    fun setSound(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setSoundEffects(enabled)
        }
    }
}
