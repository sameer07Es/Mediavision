# Frequently Asked Questions (FAQ)

## General Questions

### What is Mediavision?

Mediavision is a custom application designed to receive and handle video streams using RTSP (Real Time Streaming Protocol) and other streaming protocols. It provides real-time notifications for stream events and supports multiple devices.

### Is Mediavision compatible with the AMB82 Mini?

**Yes!** Mediavision is fully compatible with the AMB82 Mini board. The AMB82 Mini can send RTSP streams, and Mediavision can receive them. See the [Quick Start Guide](QUICKSTART.md) for setup instructions.

### Can Mediavision receive RTSP streams?

**Yes!** Mediavision is specifically designed to receive RTSP streams. It supports:
- Multiple video codecs (H.264, H.265, MJPEG)
- Both TCP and UDP transport
- Authentication (Basic and Digest)
- Multiple concurrent streams
- Auto-reconnection on disconnect

## AMB82 Mini Questions

### What RTSP URL format does the AMB82 Mini use?

The AMB82 Mini typically uses:
```
rtsp://<device-ip>:554/stream
```

For example: `rtsp://192.168.1.100:554/stream`

### What video resolution does the AMB82 Mini support?

The AMB82 Mini supports multiple resolutions:
- **1080p (1920x1080)** - Full HD
- **720p (1280x720)** - HD  
- **VGA (640x480)** - Standard
- **QVGA (320x240)** - Low bandwidth

### How do I set up my AMB82 Mini to work with Mediavision?

Follow these steps:
1. Upload RTSP streaming code to your AMB82 Mini (see [QUICKSTART.md](QUICKSTART.md))
2. Connect AMB82 Mini to WiFi
3. Note the IP address from Serial Monitor
4. Configure Mediavision with the RTSP URL
5. Start streaming!

Detailed instructions are in the [Quick Start Guide](QUICKSTART.md).

### Can I connect multiple AMB82 Mini devices?

**Yes!** Mediavision supports multiple concurrent streams. Just add each device to your configuration file:

```json
{
  "devices": [
    {
      "id": "amb82-001",
      "name": "Camera 1",
      "rtsp": {"url": "rtsp://192.168.1.100:554/stream"}
    },
    {
      "id": "amb82-002",
      "name": "Camera 2",
      "rtsp": {"url": "rtsp://192.168.1.101:554/stream"}
    }
  ]
}
```

## RTSP Questions

### What is RTSP?

RTSP (Real Time Streaming Protocol) is a network protocol for controlling streaming media servers. It's used for establishing and controlling media sessions between endpoints, commonly used for IP cameras and streaming devices.

### Does Mediavision support RTSP over TCP and UDP?

**Yes!** Mediavision supports both:
- **TCP**: More reliable, recommended for WiFi networks
- **UDP**: Lower latency, better for wired networks with good connectivity

Configure in your config file:
```json
{
  "rtsp": {
    "transport": "tcp"  // or "udp"
  }
}
```

### Does Mediavision support RTSP authentication?

**Yes!** Mediavision supports both Basic and Digest authentication:

```json
{
  "rtsp": {
    "url": "rtsp://192.168.1.100:554/stream",
    "authentication": {
      "username": "admin",
      "password": "password123"
    }
  }
}
```

### What video codecs are supported?

Mediavision supports:
- **H.264** (most common, recommended)
- **H.265/HEVC** (newer, better compression)
- **MJPEG** (compatibility)

### How can I test if my RTSP stream is working?

Test with VLC Media Player:
1. Open VLC
2. Media → Open Network Stream
3. Enter your RTSP URL: `rtsp://192.168.1.100:554/stream`
4. Click Play

If it works in VLC, it will work in Mediavision.

## Notification Questions

### How do notifications work in Mediavision?

Mediavision sends real-time JSON notifications for events like:
- Stream connected/disconnected
- Motion detected
- Object detected
- System errors

Notifications can be delivered via:
- Webhooks (HTTP POST)
- MQTT (publish to broker)
- Email (SMTP)
- Push notifications

### What format do notifications use?

Notifications use a structured JSON format:

```json
{
  "id": "unique-id",
  "timestamp": "2026-01-29T08:00:00.000Z",
  "type": "event-type",
  "severity": "info|warning|error|critical",
  "source": {
    "deviceId": "device-id",
    "streamUrl": "rtsp://...",
    "deviceName": "Device Name"
  },
  "event": {
    "name": "Event Name",
    "description": "Description",
    "data": {}
  }
}
```

See [NOTIFICATIONS.md](NOTIFICATIONS.md) for complete documentation.

### What notification types are available?

**Stream Events:**
- `stream.connected` - Stream successfully connected
- `stream.disconnected` - Stream disconnected
- `stream.error` - Stream error occurred
- `stream.quality.changed` - Stream quality changed

**Detection Events:**
- `detection.motion` - Motion detected
- `detection.object` - Object detected
- `detection.face` - Face detected

**System Events:**
- `system.started` - System started
- `system.stopped` - System stopped
- `system.storage.warning` - Low storage space

### How do I receive notifications via webhook?

Configure webhook in your config file:

```json
{
  "notifications": {
    "enabled": true,
    "methods": {
      "webhook": {
        "enabled": true,
        "url": "https://your-server.com/webhook",
        "headers": {
          "Authorization": "Bearer your-token"
        }
      }
    }
  }
}
```

Your webhook endpoint should accept POST requests with JSON payloads.

### Can I filter which notifications I receive?

**Yes!** You can filter by:
- **Severity**: Only receive warnings, errors, or critical
- **Type**: Filter by event type (e.g., only motion detection)
- **Device**: Only notifications from specific devices
- **Time**: Schedule when to receive notifications

Example:
```json
{
  "notifications": {
    "filters": {
      "minSeverity": "warning",
      "types": ["detection.motion", "stream.error"],
      "devices": ["amb82-001"]
    }
  }
}
```

### How do I send notifications via MQTT?

Configure MQTT in your config file:

```json
{
  "notifications": {
    "methods": {
      "mqtt": {
        "enabled": true,
        "broker": "mqtt://broker.example.com:1883",
        "topic": "mediavision/notifications",
        "qos": 1,
        "authentication": {
          "username": "mqtt-user",
          "password": "mqtt-pass"
        }
      }
    }
  }
}
```

Notifications will be published to the specified MQTT topic.

## Configuration Questions

### Where do I put the configuration file?

Create a `config.json` file in your working directory or specify the path when starting Mediavision:

```bash
mediavision --config /path/to/config.json
```

### Is there a configuration example?

**Yes!** See [config.example.json](config.example.json) for a complete example with all options.

### How do I validate my configuration?

Your configuration is validated against [config.schema.json](config.schema.json). Use a JSON schema validator to check your configuration before using it.

Online validators:
- https://www.jsonschemavalidator.net/
- https://jsonschemalint.com/

## Troubleshooting Questions

### Why can't I connect to my AMB82 Mini?

**Common causes:**
1. **Wrong IP address** - Check Serial Monitor for correct IP
2. **Network issues** - Ensure devices are on same network
3. **Firewall blocking** - Allow port 554 (RTSP)
4. **AMB82 not streaming** - Verify code uploaded and running
5. **Wrong URL format** - Use `rtsp://IP:554/stream`

**Solutions:**
```bash
# Test connectivity
ping 192.168.1.100

# Test RTSP port
telnet 192.168.1.100 554

# Test with VLC first
# Open VLC → Network Stream → rtsp://192.168.1.100:554/stream
```

### Stream is choppy or disconnects frequently

**Solutions:**
1. **Reduce resolution** - Change from 1080p to 720p
2. **Lower frame rate** - Change from 30fps to 25fps or 15fps
3. **Use TCP transport** - More reliable than UDP
4. **Improve WiFi** - Move closer to router or use 5GHz band
5. **Enable reconnect** - Set `reconnect: true` in config

### Notifications not working

**Check:**
1. ✓ Notifications enabled: `"enabled": true`
2. ✓ Delivery method configured (webhook, MQTT, etc.)
3. ✓ URL/endpoint is accessible
4. ✓ Filters not blocking notifications
5. ✓ Check logs for errors

**Test webhook:**
```bash
curl -X POST https://your-server.com/webhook \
  -H "Content-Type: application/json" \
  -d '{"test": true}'
```

### How do I view logs?

Logs are typically located at:
- `/var/log/mediavision/app.log` (main application log)
- `/var/log/mediavision/notifications.log` (notification log)

Configure log location in config file:
```json
{
  "logging": {
    "level": "debug",
    "file": "/path/to/log/file.log"
  }
}
```

## Performance Questions

### How many streams can Mediavision handle?

This depends on your hardware, but Mediavision can handle multiple concurrent streams. For optimal performance:
- Use 720p instead of 1080p for multiple streams
- Use H.264 codec (better compression than MJPEG)
- Ensure sufficient network bandwidth
- Use adequate hardware (multi-core CPU, 4GB+ RAM)

### How much bandwidth does RTSP streaming use?

Approximate bandwidth per stream:
- **1080p @ 30fps (H.264)**: 4-8 Mbps
- **720p @ 25fps (H.264)**: 2-4 Mbps
- **VGA @ 15fps (H.264)**: 1-2 Mbps

### How can I reduce bandwidth usage?

1. Lower resolution (1080p → 720p → VGA)
2. Reduce frame rate (30fps → 25fps → 15fps)
3. Adjust bitrate on camera
4. Use H.265 codec (better compression than H.264)

## Compatibility Questions

### What other devices are compatible?

Mediavision works with any device that sends RTSP streams:
- **IP Cameras**: Hikvision, Dahua, Axis, Reolink
- **IoT Devices**: ESP32-CAM, Raspberry Pi with camera
- **Media Servers**: VLC, FFmpeg, GStreamer, OBS Studio
- **Other Ameba boards**: RTL8722DM, RTL8720DN

See [COMPATIBILITY.md](COMPATIBILITY.md) for complete list.

### Does Mediavision work with ESP32-CAM?

**Yes!** Any ESP32-CAM device with RTSP firmware is compatible. Configuration is similar to AMB82 Mini - just use the ESP32-CAM's RTSP URL.

### Can I use Mediavision with commercial IP cameras?

**Yes!** Most IP cameras support RTSP. Check your camera's documentation for the RTSP URL format.

Common formats:
- Hikvision: `rtsp://IP:554/Streaming/Channels/101`
- Dahua: `rtsp://IP:554/cam/realmonitor?channel=1&subtype=0`
- Generic: `rtsp://IP:554/stream` or `rtsp://IP:554/live`

## Getting Help

### Where can I find more documentation?

- [README.md](README.md) - Main documentation
- [QUICKSTART.md](QUICKSTART.md) - Quick start guide
- [COMPATIBILITY.md](COMPATIBILITY.md) - Device compatibility
- [NOTIFICATIONS.md](NOTIFICATIONS.md) - Notification system
- [config.example.json](config.example.json) - Configuration example

### Where can I report issues?

Visit the [GitHub Issues](https://github.com/sameer07Es/Mediavision/issues) page to:
- Report bugs
- Request features
- Ask questions
- Share feedback

### How can I contribute?

Contributions are welcome! You can:
- Submit bug fixes
- Add new features
- Improve documentation
- Share configuration examples
- Report compatibility with new devices

Visit the repository to get started!
