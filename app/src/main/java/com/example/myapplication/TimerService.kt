package com.example.myapplication

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.TimeUnit

class TimerService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var timerJob: Job? = null
    private var startTimeMillis: Long = 0
    private var initialRemainingMillis: Long = 0
    private var taskName: String = ""

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "START") {
            taskName = intent.getStringExtra("TASK_NAME") ?: "Task"
            initialRemainingMillis = intent.getLongExtra("REMAINING_TIME", 0L)
            startTimeMillis = System.currentTimeMillis()
            
            // Always call startForeground when action is START to avoid ForegroundServiceDidNotStartInTimeException
            startForegroundService(taskName)
            
            val isVisible = intent.getBooleanExtra("IS_VISIBLE", false)
            if (!isVisible) {
                startTimerUpdates()
            } else {
                // If app is visible, we might not want to show the notification, 
                // but we ALREADY called startForeground to satisfy the OS.
                // We can stop it now if we really want, but it's safer to just let it run.
                startTimerUpdates()
            }
        } else if (action == "UPDATE_VISIBILITY") {
            val isVisible = intent.getBooleanExtra("IS_VISIBLE", true)
            if (isVisible) {
                stopTimerUpdates()
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                // Only start if a task is actually supposed to be running
                // We'll rely on the ViewModel to only send this if needed
                startForegroundService(taskName)
                startTimerUpdates()
            }
        } else if (action == "STOP") {
            stopTimerUpdates()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startTimerUpdates() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTimeMillis
                val remaining = (initialRemainingMillis - elapsed).coerceAtLeast(0L)
                
                updateNotification(remaining)
                
                if (remaining <= 0) break
                delay(1000)
            }
        }
    }

    private fun stopTimerUpdates() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun updateNotification(remainingMillis: Long) {
        val timeStr = formatDigitalClock(remainingMillis)
        val notification = createNotification(taskName, timeStr)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }

    private fun createNotification(taskName: String, timeText: String): Notification {
        val channelId = "timer_channel"
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Focusing: $taskName")
            .setContentText("Time remaining: $timeText")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    private fun startForegroundService(taskName: String) {
        val channelId = "timer_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Focus Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification = createNotification(taskName, formatDigitalClock(initialRemainingMillis))

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Even if startForeground fails, we should try to stopSelf to avoid ANR/Crash
            stopSelf()
        }
    }

    private fun formatDigitalClock(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
