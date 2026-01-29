# Mediavision

A Kotlin-based Android application designed to receive and visualize RTSP video streams, capture audio input, and receive custom notifications from ESP32 devices.

## Features

### 1. RTSP Video Streaming
- Real-time RTSP stream playback using ExoPlayer
- Support for various RTSP sources (cameras, servers, etc.)
- Built-in video player controls (play, pause, seek)
- Automatic buffering and connection status display

### 2. Audio Input Handling
- Real-time audio recording from device microphone
- Configurable audio parameters (44.1kHz, 16-bit PCM)
- Start/stop audio capture controls
- Runtime permission handling for audio recording

### 3. ESP32 Alert System
- Continuous monitoring of ESP32 device status
- Custom notification system for ESP32 alerts
- Configurable polling interval (default: 5 seconds)
- Background service for reliable monitoring
- Custom notification layout with timestamp

## Technical Stack

- **Language**: Kotlin
- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **Key Libraries**:
  - ExoPlayer 2.19.1 (RTSP streaming)
  - OkHttp 4.12.0 (HTTP communication)
  - Retrofit 2.9.0 (REST API)
  - Kotlin Coroutines 1.7.3 (Async operations)
  - AndroidX Core & AppCompat

## Setup Instructions

### Prerequisites
1. Android Studio Hedgehog (2023.1.1) or later
2. JDK 8 or higher
3. Android SDK with API level 34

### Building the Project
1. Clone the repository:
   ```bash
   git clone https://github.com/sameer07Es/Mediavision.git
   cd Mediavision
   ```

2. Open the project in Android Studio

3. Sync Gradle files (File → Sync Project with Gradle Files)

4. Build the project (Build → Make Project)

5. Run on device or emulator (Run → Run 'app')

### Gradle Build Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests
./gradlew test
```

## Usage

### RTSP Streaming
1. Enter the RTSP URL in the format: `rtsp://192.168.1.100:8554/stream`
2. Tap "Connect" to start streaming
3. Use the player controls to pause/resume
4. Tap "Disconnect" to stop streaming

### Audio Recording
1. Tap "Start Audio" button
2. Grant microphone permission if prompted
3. Audio recording will begin
4. Tap "Stop Audio" to end recording

### ESP32 Monitoring
1. Enter your ESP32 server URL: `http://192.168.1.101`
2. Grant notification permission if prompted (Android 13+)
3. Tap "Start Monitoring"
4. The app will poll the ESP32 endpoint every 5 seconds
5. Notifications will appear when alerts are detected

## ESP32 Integration

### Expected ESP32 Endpoint
The ESP32 device should expose an HTTP endpoint at `/alert` that returns:

**JSON Response** (Recommended):
```json
{
  "alert": true,
  "message": "Motion detected"
}
```

**Simple Response** (Also supported):
- `1` or `true` for alert
- Any response containing "alert" or "true" (case-insensitive)

### Example ESP32 Code (Arduino)
```cpp
#include <WiFi.h>
#include <WebServer.h>

const char* ssid = "YourWiFiSSID";
const char* password = "YourPassword";

WebServer server(80);
bool alertStatus = false;

void handleAlert() {
  String response = "{\"alert\":";
  response += alertStatus ? "true" : "false";
  response += ",\"message\":\"";
  response += alertStatus ? "Alert detected!" : "No alert";
  response += "\"}";
  
  server.send(200, "application/json", response);
}

void setup() {
  Serial.begin(115200);
  WiFi.begin(ssid, password);
  
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  
  Serial.println("\nConnected to WiFi");
  Serial.println(WiFi.localIP());
  
  server.on("/alert", handleAlert);
  server.begin();
}

void loop() {
  server.handleClient();
  
  // Your alert detection logic here
  // Set alertStatus = true when alert condition is met
}
```

## Permissions

The app requires the following permissions:
- `INTERNET` - For RTSP streaming and ESP32 communication
- `ACCESS_NETWORK_STATE` - To check network connectivity
- `RECORD_AUDIO` - For audio input capture
- `MODIFY_AUDIO_SETTINGS` - For audio configuration
- `POST_NOTIFICATIONS` - For showing ESP32 alerts (Android 13+)
- `WAKE_LOCK` - To keep service running
- `FOREGROUND_SERVICE` - For background ESP32 monitoring
- `FOREGROUND_SERVICE_DATA_SYNC` - For data sync operations

## Architecture

### Components

#### MainActivity
- Main UI controller
- Manages RTSP player lifecycle
- Handles audio recording
- Controls ESP32 monitoring service

#### Esp32MonitorService
- Background service for continuous monitoring
- Polls ESP32 device at regular intervals
- Sends notifications when alerts detected
- Runs as foreground service for reliability

#### Notification System
- Custom notification layout
- High priority notifications
- Vibration and sound alerts
- Timestamp display

## Customization

### Modify Polling Interval
In `Esp32MonitorService.kt`:
```kotlin
private const val POLLING_INTERVAL = 5000L // Change to desired milliseconds
```

### Modify Audio Settings
In `MainActivity.kt`:
```kotlin
private const val SAMPLE_RATE = 44100 // Change sample rate
private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO // or CHANNEL_IN_STEREO
```

## Troubleshooting

### RTSP Connection Issues
- Verify the RTSP URL is correct
- Ensure the RTSP server is accessible from your network
- Check if firewall allows RTSP traffic (port 554)
- Try using VLC player to verify the stream works

### ESP32 Not Detected
- Verify ESP32 is on the same network
- Check ESP32 server is running
- Test the endpoint using a web browser: `http://ESP32_IP/alert`
- Ensure notification permissions are granted

### Audio Recording Fails
- Grant microphone permission
- Check if another app is using the microphone
- Verify device has a working microphone

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues and questions, please open an issue on GitHub.
