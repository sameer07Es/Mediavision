package com.mediavision.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.rtsp.RtspMediaSource
import com.mediavision.app.databinding.ActivityMainBinding
import com.mediavision.app.service.Esp32MonitorService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var player: ExoPlayer? = null
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null

    companion object {
        private const val REQUEST_AUDIO_PERMISSION = 1001
        private const val REQUEST_NOTIFICATION_PERMISSION = 1002
        const val NOTIFICATION_CHANNEL_ID = "esp32_alerts"
        const val NOTIFICATION_CHANNEL_NAME = "ESP32 Alerts"
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createNotificationChannel()
        setupPlayer()
        setupListeners()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                importance
            ).apply {
                description = "Notifications for ESP32 alerts"
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player

        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        binding.statusText.text = getString(R.string.status_connecting)
                    }
                    Player.STATE_READY -> {
                        binding.statusText.text = getString(R.string.status_playing)
                    }
                    Player.STATE_IDLE -> {
                        binding.statusText.text = getString(R.string.status_idle)
                    }
                    Player.STATE_ENDED -> {
                        binding.statusText.text = getString(R.string.status_idle)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                binding.statusText.text = "${getString(R.string.status_error)}: ${error.message}"
                Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupListeners() {
        // RTSP Connection
        binding.connectButton.setOnClickListener {
            val url = binding.rtspUrlInput.text.toString().trim()
            if (url.isNotEmpty()) {
                connectToRtsp(url)
            } else {
                Toast.makeText(this, "Please enter RTSP URL", Toast.LENGTH_SHORT).show()
            }
        }

        binding.disconnectButton.setOnClickListener {
            disconnectRtsp()
        }

        // Audio Recording
        binding.startAudioButton.setOnClickListener {
            if (checkAudioPermission()) {
                startAudioRecording()
            } else {
                requestAudioPermission()
            }
        }

        binding.stopAudioButton.setOnClickListener {
            stopAudioRecording()
        }

        // ESP32 Monitoring
        binding.startMonitoringButton.setOnClickListener {
            val url = binding.esp32UrlInput.text.toString().trim()
            if (url.isNotEmpty()) {
                if (checkNotificationPermission()) {
                    startEsp32Monitoring(url)
                } else {
                    requestNotificationPermission()
                }
            } else {
                Toast.makeText(this, "Please enter ESP32 URL", Toast.LENGTH_SHORT).show()
            }
        }

        binding.stopMonitoringButton.setOnClickListener {
            stopEsp32Monitoring()
        }
    }

    private fun connectToRtsp(url: String) {
        // Validate URL format
        if (!url.startsWith("rtsp://") && !url.startsWith("rtsps://")) {
            Toast.makeText(this, "Invalid RTSP URL format", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val mediaItem = MediaItem.fromUri(url)
            val rtspMediaSource = RtspMediaSource.Factory()
                .createMediaSource(mediaItem)

            player?.setMediaSource(rtspMediaSource)
            player?.prepare()
            player?.play()

            binding.connectButton.isEnabled = false
            binding.disconnectButton.isEnabled = true
            binding.rtspUrlInput.isEnabled = false
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to connect: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun disconnectRtsp() {
        player?.stop()
        player?.clearMediaItems()

        binding.connectButton.isEnabled = true
        binding.disconnectButton.isEnabled = false
        binding.rtspUrlInput.isEnabled = true
        binding.statusText.text = getString(R.string.status_idle)
    }

    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            AlertDialog.Builder(this)
                .setTitle(R.string.permission_required)
                .setMessage(R.string.audio_permission_required)
                .setPositiveButton(R.string.grant) { _, _ ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        REQUEST_AUDIO_PERMISSION
                    )
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_AUDIO_PERMISSION
            )
        }
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.permission_required)
                    .setMessage(R.string.notification_permission_required)
                    .setPositiveButton(R.string.grant) { _, _ ->
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            REQUEST_NOTIFICATION_PERMISSION
                        )
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    private fun startAudioRecording() {
        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestAudioPermission()
                return
            }

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            audioRecord?.startRecording()
            isRecording = true

            recordingThread = Thread {
                val buffer = ByteArray(bufferSize)
                while (isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (read > 0) {
                        // Process audio data here if needed
                        // For now, we're just capturing it
                    } else if (read < 0) {
                        // Error occurred during read, stop recording
                        isRecording = false
                        break
                    }
                }
            }
            recordingThread?.start()

            binding.startAudioButton.isEnabled = false
            binding.stopAudioButton.isEnabled = true
            binding.audioStatusText.text = getString(R.string.audio_recording)
            Toast.makeText(this, "Audio recording started", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start audio: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopAudioRecording() {
        isRecording = false
        // Join with timeout to prevent ANR
        recordingThread?.join(1000)

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        binding.startAudioButton.isEnabled = true
        binding.stopAudioButton.isEnabled = false
        binding.audioStatusText.text = getString(R.string.audio_stopped)
        Toast.makeText(this, "Audio recording stopped", Toast.LENGTH_SHORT).show()
    }

    private fun startEsp32Monitoring(url: String) {
        // Validate URL format
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Toast.makeText(this, "Invalid URL format. Use http:// or https://", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, Esp32MonitorService::class.java).apply {
            action = Esp32MonitorService.ACTION_START_MONITORING
            putExtra(Esp32MonitorService.EXTRA_ESP32_URL, url)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        binding.startMonitoringButton.isEnabled = false
        binding.stopMonitoringButton.isEnabled = true
        binding.esp32UrlInput.isEnabled = false
        binding.esp32StatusText.text = getString(R.string.monitoring_active)
        Toast.makeText(this, "ESP32 monitoring started", Toast.LENGTH_SHORT).show()
    }

    private fun stopEsp32Monitoring() {
        val intent = Intent(this, Esp32MonitorService::class.java).apply {
            action = Esp32MonitorService.ACTION_STOP_MONITORING
        }
        startService(intent)

        binding.startMonitoringButton.isEnabled = true
        binding.stopMonitoringButton.isEnabled = false
        binding.esp32UrlInput.isEnabled = true
        binding.esp32StatusText.text = getString(R.string.monitoring_stopped)
        Toast.makeText(this, "ESP32 monitoring stopped", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_AUDIO_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startAudioRecording()
                } else {
                    Toast.makeText(this, "Audio permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_NOTIFICATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, user can now start monitoring
                    Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudioRecording()
        player?.release()
        player = null
    }
}
