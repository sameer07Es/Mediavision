# Device Compatibility Guide

## AMB82 Mini (Ameba RTL8735B)

### Overview
The AMB82 Mini is a compact IoT development board from Realtek that includes:
- RTL8735B ARM Cortex-M33 processor
- Built-in WiFi connectivity
- Camera interface support
- RTSP streaming capabilities

### RTSP Streaming Configuration

#### Default Settings
```
Protocol: RTSP
Port: 554 (standard RTSP port)
Video Codec: H.264
Audio Codec: AAC (optional)
Stream Path: /stream or /live
```

#### Arduino Example Code for AMB82 Mini
```cpp
#include <WiFi.h>
#include <StreamIO.h>
#include <VideoStream.h>
#include <RTSP.h>

// WiFi credentials
const char* ssid = "your-wifi-ssid";
const char* password = "your-wifi-password";

// Camera configuration
VideoSetting config(VIDEO_FHD, 30, VIDEO_H264, 0);

// RTSP server
RTSP rtsp;
StreamIO videoStreamer(1, 1);

void setup() {
  Serial.begin(115200);
  
  // Connect to WiFi
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWiFi connected");
  Serial.print("IP Address: ");
  Serial.println(WiFi.localIP());
  
  // Initialize camera
  Camera.configVideoChannel(0, config);
  Camera.videoInit();
  
  // Start RTSP server
  rtsp.configPort(554);
  rtsp.configName("Mediavision Stream");
  rtsp.begin();
  
  // Link camera to RTSP
  videoStreamer.registerInput(Camera.getStream(0));
  videoStreamer.registerOutput(rtsp);
  videoStreamer.begin();
  
  Serial.println("RTSP Server started");
  Serial.print("Stream URL: rtsp://");
  Serial.print(WiFi.localIP());
  Serial.println(":554/stream");
}

void loop() {
  delay(1000);
}
```

### Mediavision Connection Settings

#### Basic Connection
```json
{
  "devices": [
    {
      "name": "AMB82 Mini Camera",
      "type": "amb82-mini",
      "rtsp": {
        "url": "rtsp://192.168.1.100:554/stream",
        "transport": "tcp"
      }
    }
  ]
}
```

#### Advanced Connection with Authentication
```json
{
  "devices": [
    {
      "name": "AMB82 Mini Camera",
      "type": "amb82-mini",
      "rtsp": {
        "url": "rtsp://192.168.1.100:554/stream",
        "transport": "tcp",
        "authentication": {
          "username": "admin",
          "password": "admin123"
        },
        "options": {
          "reconnect": true,
          "reconnectDelay": 5000,
          "timeout": 10000,
          "keepAlive": true
        }
      },
      "video": {
        "codec": "H.264",
        "resolution": "1920x1080",
        "fps": 30
      }
    }
  ]
}
```

### Supported Resolutions
- **1080p (1920x1080)** - Full HD
- **720p (1280x720)** - HD
- **VGA (640x480)** - Standard
- **QVGA (320x240)** - Low bandwidth

### Network Requirements
- **Bandwidth**: 2-8 Mbps depending on resolution and frame rate
- **Latency**: < 100ms for real-time viewing
- **Protocol**: RTSP over TCP recommended for reliability

### Common Issues and Solutions

#### Issue: Stream not found (404 error)
**Cause**: Incorrect stream path
**Solution**: Try common paths:
- `rtsp://ip:554/stream`
- `rtsp://ip:554/live`
- `rtsp://ip:554/`

#### Issue: Connection timeout
**Cause**: Network or firewall blocking
**Solution**:
- Verify AMB82 Mini is on the same network
- Check firewall allows port 554
- Try using device IP instead of hostname

#### Issue: Poor video quality or stuttering
**Cause**: Network bandwidth or device performance
**Solution**:
- Reduce resolution on AMB82 Mini
- Lower frame rate (e.g., from 30fps to 15fps)
- Use TCP transport instead of UDP
- Check WiFi signal strength

## Other Compatible Devices

### IP Cameras
Any IP camera supporting RTSP protocol is compatible:
- Hikvision cameras
- Dahua cameras
- Axis cameras
- Reolink cameras
- Generic ONVIF cameras

### IoT Devices
- ESP32-CAM (with RTSP firmware)
- Raspberry Pi with camera module (using RTSP server)
- Arduino with compatible shields
- Other Ameba boards (RTL8722DM, RTL8720DN)

### Media Servers
- VLC Media Player (RTSP streaming)
- FFmpeg RTSP streams
- GStreamer RTSP pipelines
- OBS Studio with RTSP output

### Configuration Examples

#### ESP32-CAM
```json
{
  "rtsp": {
    "url": "rtsp://192.168.1.101:8554/mjpeg/1"
  }
}
```

#### Raspberry Pi
```json
{
  "rtsp": {
    "url": "rtsp://192.168.1.102:8554/stream"
  }
}
```

#### Generic IP Camera
```json
{
  "rtsp": {
    "url": "rtsp://192.168.1.103:554/cam/realmonitor?channel=1&subtype=0",
    "authentication": {
      "username": "admin",
      "password": "password123"
    }
  }
}
```

## Testing Your Connection

### Using VLC Media Player
1. Open VLC
2. Go to Media → Open Network Stream
3. Enter your RTSP URL: `rtsp://192.168.1.100:554/stream`
4. Click Play

If the stream plays in VLC, it should work with Mediavision.

### Using FFmpeg
```bash
ffmpeg -rtsp_transport tcp -i rtsp://192.168.1.100:554/stream -f null -
```

### Using cURL
```bash
curl -v rtsp://192.168.1.100:554/stream
```

## Performance Optimization

### For AMB82 Mini
1. **Adjust bitrate**: Lower bitrate reduces bandwidth but may reduce quality
2. **Use appropriate resolution**: Match resolution to display requirements
3. **Enable hardware encoding**: Ensure H.264 hardware encoding is enabled
4. **Optimize WiFi**: Place device close to router, use 5GHz band if available

### For Mediavision
1. **Use TCP transport**: More reliable than UDP
2. **Enable buffering**: Reduces stuttering on unstable networks
3. **Hardware acceleration**: Use GPU decoding when available
4. **Connection pooling**: Reuse connections for multiple requests

## Security Considerations

### AMB82 Mini Security
- Change default passwords
- Use strong authentication credentials
- Enable encryption when possible
- Keep firmware updated
- Isolate camera network from main network

### RTSP Security
- Use RTSPS (RTSP over TLS) when supported
- Implement authentication on all streams
- Use VPN for remote access
- Monitor for unauthorized access attempts

## Troubleshooting Checklist

- [ ] Device is powered on and connected to network
- [ ] RTSP server is running on device
- [ ] Correct IP address and port
- [ ] Firewall allows RTSP traffic (port 554)
- [ ] Authentication credentials are correct
- [ ] Stream path is correct
- [ ] Network has sufficient bandwidth
- [ ] Codec is supported (H.264, H.265, MJPEG)
- [ ] Test with VLC or FFmpeg first
