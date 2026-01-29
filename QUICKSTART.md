# Quick Start Guide - AMB82 Mini

This guide will help you quickly set up Mediavision to receive RTSP streams from your AMB82 Mini board.

## Prerequisites

- AMB82 Mini board with camera module
- WiFi network
- Mediavision installed and running

## Step 1: Set Up Your AMB82 Mini

### 1.1 Install Arduino IDE and Board Support

1. Download and install [Arduino IDE](https://www.arduino.cc/en/software)
2. Add Realtek Ameba board support:
   - Open Arduino IDE
   - Go to File → Preferences
   - Add this URL to "Additional Boards Manager URLs":
     ```
     https://github.com/ambiot/ambd_arduino/raw/master/Arduino_package/package_realtek.com_amebad_index.json
     ```
   - Go to Tools → Board → Boards Manager
   - Search for "Realtek Ameba" and install

### 1.2 Upload RTSP Streaming Code

Create a new sketch with the following code:

```cpp
#include <WiFi.h>
#include <StreamIO.h>
#include <VideoStream.h>
#include <RTSP.h>

// WiFi credentials
const char* ssid = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASSWORD";

// Video configuration
// Options: VIDEO_FHD (1920x1080), VIDEO_HD (1280x720), VIDEO_VGA (640x480)
VideoSetting config(VIDEO_FHD, 30, VIDEO_H264, 0);

// RTSP server instance
RTSP rtsp;
StreamIO videoStreamer(1, 1);

void setup() {
  Serial.begin(115200);
  
  // Wait for serial connection
  while (!Serial) {
    delay(100);
  }
  
  Serial.println("Mediavision AMB82 Mini Setup");
  Serial.println("============================");
  
  // Connect to WiFi
  Serial.print("Connecting to WiFi: ");
  Serial.println(ssid);
  WiFi.begin(ssid, password);
  
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  
  Serial.println("\n✓ WiFi connected");
  Serial.print("IP Address: ");
  Serial.println(WiFi.localIP());
  
  // Initialize camera
  Serial.println("Initializing camera...");
  Camera.configVideoChannel(0, config);
  Camera.videoInit();
  Serial.println("✓ Camera initialized");
  
  // Configure and start RTSP server
  Serial.println("Starting RTSP server...");
  rtsp.configPort(554);  // Standard RTSP port
  rtsp.configName("Mediavision Stream");
  rtsp.begin();
  Serial.println("✓ RTSP server started");
  
  // Link camera output to RTSP server
  videoStreamer.registerInput(Camera.getStream(0));
  videoStreamer.registerOutput(rtsp);
  
  if (videoStreamer.begin()) {
    Serial.println("✓ Video streaming started");
  } else {
    Serial.println("✗ Failed to start video streaming");
    return;
  }
  
  // Print connection information
  Serial.println("\n============================");
  Serial.println("Stream Information:");
  Serial.println("============================");
  Serial.print("RTSP URL: rtsp://");
  Serial.print(WiFi.localIP());
  Serial.println(":554/stream");
  Serial.println("Resolution: 1920x1080");
  Serial.println("Frame Rate: 30 fps");
  Serial.println("Codec: H.264");
  Serial.println("============================");
  Serial.println("Ready to stream!");
}

void loop() {
  // Keep the stream running
  delay(1000);
  
  // Optional: Print status every 10 seconds
  static unsigned long lastPrint = 0;
  if (millis() - lastPrint > 10000) {
    Serial.print("Streaming... Uptime: ");
    Serial.print(millis() / 1000);
    Serial.println(" seconds");
    lastPrint = millis();
  }
}
```

### 1.3 Configure and Upload

1. Select your board: Tools → Board → Ameba ARM (32-bits) Boards → AMB82 MINI
2. Select the correct COM port: Tools → Port
3. Update WiFi credentials in the code
4. Click Upload button
5. Open Serial Monitor (115200 baud) to see the RTSP URL

## Step 2: Configure Mediavision

### 2.1 Find Your AMB82 Mini IP Address

After uploading the code, check the Serial Monitor for the IP address. You'll see output like:

```
WiFi connected
IP Address: 192.168.1.100
RTSP URL: rtsp://192.168.1.100:554/stream
```

### 2.2 Create Configuration File

Create a `config.json` file with your AMB82 Mini details:

```json
{
  "devices": [
    {
      "id": "amb82-001",
      "name": "My AMB82 Camera",
      "type": "amb82-mini",
      "rtsp": {
        "url": "rtsp://192.168.1.100:554/stream",
        "transport": "tcp",
        "options": {
          "reconnect": true,
          "reconnectDelay": 5000,
          "timeout": 10000
        }
      },
      "metadata": {
        "location": "Home",
        "tags": ["amb82", "camera"]
      }
    }
  ],
  "notifications": {
    "enabled": true,
    "methods": {
      "webhook": {
        "enabled": true,
        "url": "https://your-webhook-endpoint.com/webhook"
      }
    }
  }
}
```

### 2.3 Test the Connection

Test the RTSP stream with VLC Media Player first:

1. Open VLC
2. Go to Media → Open Network Stream
3. Enter: `rtsp://192.168.1.100:554/stream`
4. Click Play

If the stream plays in VLC, Mediavision will be able to connect.

## Step 3: Start Receiving Streams

### 3.1 Start Mediavision

```bash
# Start with your configuration
mediavision --config config.json
```

### 3.2 Verify Connection

You should see output indicating the stream is connected:

```
[INFO] Connecting to rtsp://192.168.1.100:554/stream
[INFO] Stream connected: My AMB82 Camera
[INFO] Codec: H.264, Resolution: 1920x1080, FPS: 30
```

### 3.3 Receive Notifications

When notifications are enabled, you'll receive webhooks for events like:

- Stream connected
- Stream disconnected
- Motion detected (if motion detection is enabled)
- Stream errors

Example notification payload:

```json
{
  "id": "notif-001",
  "timestamp": "2026-01-29T08:00:00.000Z",
  "type": "stream.connected",
  "severity": "info",
  "source": {
    "deviceId": "amb82-001",
    "streamUrl": "rtsp://192.168.1.100:554/stream",
    "deviceName": "My AMB82 Camera"
  },
  "event": {
    "name": "Stream Connected",
    "description": "RTSP stream successfully connected",
    "data": {
      "codec": "H.264",
      "resolution": "1920x1080",
      "fps": 30
    }
  }
}
```

## Troubleshooting

### Stream Not Found

**Problem**: Cannot connect to RTSP stream

**Solution**:
```bash
# Verify AMB82 Mini is reachable
ping 192.168.1.100

# Check if RTSP port is open
telnet 192.168.1.100 554

# Try accessing stream with curl
curl -v rtsp://192.168.1.100:554/stream
```

### Poor Video Quality

**Problem**: Video is choppy or low quality

**Solution**: Reduce resolution or frame rate on AMB82 Mini:

```cpp
// Change from VIDEO_FHD to VIDEO_HD
VideoSetting config(VIDEO_HD, 25, VIDEO_H264, 0);  // 720p @ 25fps
```

### Connection Keeps Dropping

**Problem**: Stream disconnects frequently

**Solution**: Improve WiFi signal or use wired connection if possible.

In `config.json`:
```json
{
  "rtsp": {
    "transport": "tcp",
    "options": {
      "reconnect": true,
      "reconnectDelay": 3000,
      "keepAlive": true
    }
  }
}
```

### Notifications Not Working

**Problem**: Not receiving webhook notifications

**Solution**: 
1. Verify webhook URL is accessible
2. Check webhook endpoint is accepting POST requests
3. Test webhook manually:

```bash
curl -X POST https://your-webhook-endpoint.com/webhook \
  -H "Content-Type: application/json" \
  -d '{"test": true}'
```

## Advanced Configuration

### Multiple AMB82 Mini Devices

```json
{
  "devices": [
    {
      "id": "amb82-001",
      "name": "Front Door",
      "rtsp": {
        "url": "rtsp://192.168.1.100:554/stream"
      }
    },
    {
      "id": "amb82-002",
      "name": "Back Yard",
      "rtsp": {
        "url": "rtsp://192.168.1.101:554/stream"
      }
    },
    {
      "id": "amb82-003",
      "name": "Garage",
      "rtsp": {
        "url": "rtsp://192.168.1.102:554/stream"
      }
    }
  ]
}
```

### Custom RTSP Port on AMB82 Mini

If you need to use a different port (e.g., 8554):

```cpp
// In Arduino code
rtsp.configPort(8554);  // Custom port
```

```json
// In config.json
{
  "rtsp": {
    "url": "rtsp://192.168.1.100:8554/stream"
  }
}
```

### Enable Motion Detection Notifications

Configure Mediavision to send notifications on motion:

```json
{
  "notifications": {
    "enabled": true,
    "filters": {
      "types": ["detection.motion", "stream.*"]
    }
  }
}
```

## Performance Tips

1. **Use 720p for multiple streams**: If streaming from multiple AMB82 Mini devices, use HD (720p) instead of Full HD to reduce bandwidth.

2. **Optimize frame rate**: 25 fps is usually sufficient for most applications and reduces network load.

3. **Use TCP transport**: More reliable than UDP, especially on WiFi networks.

4. **Position devices strategically**: Place AMB82 Mini devices within good WiFi range.

5. **Separate camera network**: Consider using a separate network VLAN for camera traffic.

## Next Steps

- [Read full documentation](README.md)
- [Learn about notification formats](NOTIFICATIONS.md)
- [Check compatibility guide](COMPATIBILITY.md)
- [Review configuration schema](config.schema.json)

## Support

For issues or questions:
- Check the troubleshooting section
- Review logs in `/var/log/mediavision/`
- Visit [GitHub Issues](https://github.com/sameer07Es/Mediavision/issues)
