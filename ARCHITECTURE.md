# Mediavision Architecture

## Application Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        MainActivity                         │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                    User Interface                   │   │
│  │  - RTSP URL Input & Controls                        │   │
│  │  - Video Player View (ExoPlayer)                    │   │
│  │  - Audio Recording Controls                         │   │
│  │  - ESP32 Monitoring Controls                        │   │
│  └─────────────────────────────────────────────────────┘   │
│                           │                                  │
│  ┌────────────────────────┼────────────────────────────┐   │
│  │                        │                            │   │
│  ▼                        ▼                            ▼   │
│ ┌─────────────┐   ┌──────────────┐   ┌─────────────────┐  │
│ │   ExoPlayer │   │ AudioRecord  │   │ Service Control │  │
│ │   Manager   │   │   Manager    │   │                 │  │
│ └─────────────┘   └──────────────┘   └─────────────────┘  │
│        │                   │                    │           │
└────────┼───────────────────┼────────────────────┼───────────┘
         │                   │                    │
         │                   │                    │
         ▼                   ▼                    ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────────────┐
│ RTSP Stream  │   │  Microphone  │   │ Esp32MonitorService  │
│   Server     │   │              │   │                      │
└──────────────┘   └──────────────┘   │  ┌────────────────┐  │
                                       │  │ HTTP Client    │  │
                                       │  │ (OkHttp)       │  │
                                       │  └────────────────┘  │
                                       │         │            │
                                       │         ▼            │
                                       │  ┌────────────────┐  │
                                       │  │ Polling Loop   │  │
                                       │  │ (5 sec)        │  │
                                       │  └────────────────┘  │
                                       │         │            │
                                       │         ▼            │
                                       │  ┌────────────────┐  │
                                       │  │ Notification   │  │
                                       │  │ Manager        │  │
                                       │  └────────────────┘  │
                                       └──────────────────────┘
                                                │
                                                ▼
                                       ┌──────────────────┐
                                       │ ESP32 Device     │
                                       │ HTTP Server      │
                                       │ /alert endpoint  │
                                       └──────────────────┘
```

## Component Descriptions

### MainActivity
The main activity serves as the central controller for all app features:
- **UI Management**: Handles all user interactions and display updates
- **Permission Handling**: Manages runtime permissions for audio, notifications
- **Lifecycle Management**: Controls ExoPlayer and AudioRecord lifecycles
- **Service Communication**: Starts/stops the ESP32 monitoring service

### ExoPlayer Manager
Handles RTSP video streaming:
- Creates and configures ExoPlayer instance
- Sets up RTSP media source
- Manages playback state (buffering, playing, error)
- Provides UI callbacks for status updates

### AudioRecord Manager
Manages audio input capture:
- Initializes AudioRecord with optimal settings
- Starts/stops audio recording
- Runs recording in background thread
- Captures audio data for processing

### Esp32MonitorService
Background service for continuous ESP32 monitoring:
- **Foreground Service**: Runs reliably in background
- **HTTP Polling**: Periodically checks ESP32 status
- **Notification System**: Sends alerts when detected
- **OkHttp Client**: Handles HTTP communication

## Data Flow

### RTSP Streaming Flow
```
User Input (URL) → MainActivity → ExoPlayer → RTSP Source → Video Display
                                     ↓
                              Status Callbacks
                                     ↓
                               UI Updates
```

### Audio Recording Flow
```
User Action → MainActivity → AudioRecord → Microphone → Audio Buffer
                                                             ↓
                                                    Audio Processing
                                                   (Ready for use)
```

### ESP32 Alert Flow
```
User Input (URL) → MainActivity → Start Service Intent
                                        ↓
                              Esp32MonitorService
                                        ↓
                              Polling Loop (5s)
                                        ↓
                              HTTP GET /alert
                                        ↓
                              Parse Response
                                        ↓
                              Alert Detected?
                                    Yes ↓
                           Notification Manager
                                        ↓
                              Custom Notification
                                        ↓
                              User Notification
```

## Permission Flow

```
App Launch
    ↓
Create Notification Channel
    ↓
User Action Requires Permission
    ↓
Check Permission → Granted? → Proceed
                      ↓ No
                Request Permission
                      ↓
            Show Rationale (if needed)
                      ↓
              Permission Dialog
                      ↓
           User Grants/Denies
                      ↓
          onRequestPermissionsResult
                      ↓
         Proceed or Show Error
```

## Threading Model

### Main Thread (UI Thread)
- UI updates
- User interaction handling
- Permission dialogs

### Background Thread
- Audio recording (dedicated thread)
- ESP32 monitoring (coroutines)
- Network requests (OkHttp)

### ExoPlayer Thread
- Video decoding
- Buffer management
- RTSP protocol handling

## Key Design Decisions

### 1. ExoPlayer for RTSP
- Native RTSP support
- Hardware acceleration
- Robust error handling
- Industry standard

### 2. Foreground Service for Monitoring
- Ensures reliability
- Prevents system kill
- User-visible operation
- Battery efficient

### 3. Polling vs Push
- HTTP polling chosen for simplicity
- Easy ESP32 implementation
- No server infrastructure needed
- Configurable interval

### 4. Custom Notifications
- Branded appearance
- Rich information display
- Timestamp tracking
- Action support ready

## Extensibility Points

### Adding New Features

#### 1. Recording Audio to File
Modify AudioRecord manager to write to file:
```kotlin
val audioFile = File(context.filesDir, "recording.pcm")
FileOutputStream(audioFile).use { output ->
    output.write(buffer, 0, read)
}
```

#### 2. WebSocket Support
Replace HTTP polling with WebSocket:
```kotlin
// Use OkHttp WebSocket
val webSocket = client.newWebSocket(request, listener)
```

#### 3. Multiple Camera Support
Add camera selection:
```kotlin
data class Camera(val name: String, val rtspUrl: String)
val cameras = listOf(...)
```

#### 4. Alert History
Add database for alert tracking:
```kotlin
// Room database
@Entity
data class Alert(
    @PrimaryKey val id: Long,
    val timestamp: Long,
    val message: String
)
```

## Security Considerations

1. **RTSP Credentials**: Support for authenticated streams
2. **HTTPS**: Recommend HTTPS for ESP32 communication
3. **Input Validation**: Validate all URLs
4. **Permission Security**: Runtime permission checks
5. **Network Security**: TLS certificate validation
