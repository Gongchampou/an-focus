package com.example.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val name: String,
    val totalTimeMillis: Long = 0L,
    val isRunning: Boolean = false,
    val lastStartTime: Long? = null
)

@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val text: String,
    val isCompleted: Boolean = false
)

data class Track(
    val id: Int,
    val title: String,
    val artist: String,
    val url: String = ""
)
