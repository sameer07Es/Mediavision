package com.mediavision.app.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.mediavision.app.MainActivity
import com.mediavision.app.R
import kotlinx.coroutines.*
import okhttp3.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class Esp32MonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitoringJob: Job? = null
    private var esp32Url: String = ""
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    companion object {
        const val ACTION_START_MONITORING = "com.mediavision.app.action.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.mediavision.app.action.STOP_MONITORING"
        const val EXTRA_ESP32_URL = "com.mediavision.app.extra.ESP32_URL"
        private const val NOTIFICATION_ID = 1001
        private const val FOREGROUND_NOTIFICATION_ID = 1000
        private const val POLLING_INTERVAL = 5000L // 5 seconds
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> {
                esp32Url = intent.getStringExtra(EXTRA_ESP32_URL) ?: ""
                if (esp32Url.isNotEmpty()) {
                    startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification())
                    startMonitoring()
                }
            }
            ACTION_STOP_MONITORING -> {
                stopMonitoring()
                stopForeground(true)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun createForegroundNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            pendingIntentFlags
        )

        return NotificationCompat.Builder(this, MainActivity.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("ESP32 Monitoring")
            .setContentText("Monitoring ESP32 for alerts...")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            while (isActive) {
                try {
                    checkEsp32Alert()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(POLLING_INTERVAL)
            }
        }
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    private suspend fun checkEsp32Alert() {
        withContext(Dispatchers.IO) {
            try {
                // Build the request URL - expecting ESP32 to have an endpoint like /alert or /status
                val alertUrl = if (esp32Url.endsWith("/")) {
                    "${esp32Url}alert"
                } else {
                    "$esp32Url/alert"
                }

                val request = Request.Builder()
                    .url(alertUrl)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        
                        // Check if ESP32 is sending an alert
                        // The ESP32 should return JSON like: {"alert": true, "message": "Motion detected"}
                        // or simply return "1" or "true" for alert
                        if (body.contains("true", ignoreCase = true) ||
                            body.contains("\"alert\":true", ignoreCase = true) ||
                            body.trim() == "1" ||
                            body.contains("alert", ignoreCase = true)
                        ) {
                            sendAlertNotification(body)
                        }
                    }
                }
            } catch (e: IOException) {
                // Network error, will retry on next poll
                e.printStackTrace()
            }
        }
    }

    private fun sendAlertNotification(message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create custom notification layout
        val notificationLayout = RemoteViews(packageName, R.layout.notification_custom).apply {
            setTextViewText(R.id.notificationTitle, getString(R.string.esp32_alert))
            setTextViewText(R.id.notificationMessage, message.ifEmpty { getString(R.string.alert_detected) })
            setTextViewText(
                R.id.notificationTime,
                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            )
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            pendingIntentFlags
        )

        val notification = NotificationCompat.Builder(this, MainActivity.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setCustomContentView(notificationLayout)
            .setCustomBigContentView(notificationLayout)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_LIGHTS)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        serviceScope.cancel()
    }
}
