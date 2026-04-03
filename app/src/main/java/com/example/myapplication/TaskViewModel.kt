package com.example.myapplication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    // Task logic
    fun addTask(name: String) {
        viewModelScope.launch {
            taskDao.insertTask(Task(name = name))
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
                    val elapsed = now - (task.lastStartTime ?: now)
                    task.copy(
                        isRunning = false,
                        totalTimeMillis = task.totalTimeMillis + elapsed,
                        lastStartTime = null
                    )
                } else {
                    task.copy(
                        isRunning = true,
                        lastStartTime = now
                    )
                }
                taskDao.insertTask(updatedTask)
            }
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
}
