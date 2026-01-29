# Mediavision

A custom application designed to receive and handle video streams using RTSP (Real Time Streaming Protocol) and other streaming protocols.

## Features

- **RTSP Stream Reception**: Receive and process RTSP video streams from compatible devices
- **Device Compatibility**: Supports various RTSP-enabled devices including the AMB82 Mini
- **Real-time Notifications**: Configurable notification system for stream events
- **Multi-stream Support**: Handle multiple RTSP streams simultaneously

## AMB82 Mini Compatibility

### Overview
**Yes, Mediavision is compatible with the AMB82 Mini board.**

The AMB82 Mini (Ameba Arduino RTL8735B) is a compact IoT development board that supports RTSP streaming. Mediavision can receive RTSP streams from the AMB82 Mini board.

### AMB82 Mini RTSP Configuration
The AMB82 Mini typically sends RTSP streams on the following format:
```
rtsp://<device-ip>:<port>/stream
```

Default configuration:
- **Protocol**: RTSP (Real Time Streaming Protocol)
- **Default Port**: 554
- **Stream Path**: Usually `/stream` or `/live`
- **Encoding**: H.264 (most common)
- **Audio**: AAC (if enabled)

### Example Connection
```
rtsp://192.168.1.100:554/stream
```

## RTSP Stream Reception

### Supported RTSP Features
- **Transport Protocols**: TCP and UDP
- **Video Codecs**: H.264, H.265/HEVC, MJPEG
- **Audio Codecs**: AAC, PCM, G.711
- **Authentication**: Basic and Digest authentication
- **Stream Types**: Live streams and recorded streams

### Configuration

To receive RTSP streams, configure the following parameters:

```json
{
  "rtsp": {
    "url": "rtsp://192.168.1.100:554/stream",
    "transport": "tcp",
    "authentication": {
      "username": "admin",
      "password": "password"
    },
    "reconnect": true,
    "timeout": 5000
  }
}
```

### Connection Parameters

| Parameter | Description | Default | Required |
|-----------|-------------|---------|----------|
| `url` | RTSP stream URL | - | Yes |
| `transport` | Transport protocol (tcp/udp) | tcp | No |
| `username` | Authentication username | - | No |
| `password` | Authentication password | - | No |
| `reconnect` | Auto-reconnect on disconnect | true | No |
| `timeout` | Connection timeout (ms) | 5000 | No |

## Notification System

### Overview
The notification system provides real-time alerts for stream events, motion detection, and system status changes.

### Notification Types

1. **Stream Events**
   - Stream connected
   - Stream disconnected
   - Stream error
   - Stream quality change

2. **Detection Events**
   - Motion detected
   - Object detected
   - Face detected

3. **System Events**
   - System started
   - System stopped
   - Configuration changed
   - Storage warning

### Notification Format

Notifications are sent in JSON format via webhooks, MQTT, or push notifications.

#### JSON Notification Schema

```json
{
  "id": "unique-notification-id",
  "timestamp": "2026-01-29T08:00:00.000Z",
  "type": "event-type",
  "severity": "info|warning|error|critical",
  "source": {
    "deviceId": "device-identifier",
    "streamUrl": "rtsp://192.168.1.100:554/stream",
    "deviceName": "AMB82 Mini Camera"
  },
  "event": {
    "name": "event-name",
    "description": "Event description",
    "data": {
      "key": "value"
    }
  },
  "metadata": {
    "location": "optional-location",
    "tags": ["tag1", "tag2"]
  }
}
```

#### Notification Examples

**Stream Connected Notification:**
```json
{
  "id": "notif-001",
  "timestamp": "2026-01-29T08:00:00.000Z",
  "type": "stream.connected",
  "severity": "info",
  "source": {
    "deviceId": "amb82-001",
    "streamUrl": "rtsp://192.168.1.100:554/stream",
    "deviceName": "AMB82 Mini Camera 1"
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

**Motion Detection Notification:**
```json
{
  "id": "notif-002",
  "timestamp": "2026-01-29T08:05:00.000Z",
  "type": "detection.motion",
  "severity": "warning",
  "source": {
    "deviceId": "amb82-001",
    "streamUrl": "rtsp://192.168.1.100:554/stream",
    "deviceName": "AMB82 Mini Camera 1"
  },
  "event": {
    "name": "Motion Detected",
    "description": "Motion detected in monitored area",
    "data": {
      "confidence": 0.95,
      "region": {
        "x": 100,
        "y": 150,
        "width": 200,
        "height": 300
      },
      "snapshotUrl": "https://example.com/snapshots/snapshot-001.jpg"
    }
  },
  "metadata": {
    "location": "Front Door",
    "tags": ["motion", "security"]
  }
}
```

**Stream Error Notification:**
```json
{
  "id": "notif-003",
  "timestamp": "2026-01-29T08:10:00.000Z",
  "type": "stream.error",
  "severity": "error",
  "source": {
    "deviceId": "amb82-001",
    "streamUrl": "rtsp://192.168.1.100:554/stream",
    "deviceName": "AMB82 Mini Camera 1"
  },
  "event": {
    "name": "Stream Connection Failed",
    "description": "Failed to connect to RTSP stream",
    "data": {
      "errorCode": "ETIMEDOUT",
      "errorMessage": "Connection timeout after 5000ms",
      "retryAttempt": 3
    }
  }
}
```

### Notification Configuration

Configure notification delivery methods:

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
      },
      "mqtt": {
        "enabled": true,
        "broker": "mqtt://broker.example.com:1883",
        "topic": "mediavision/notifications",
        "qos": 1
      },
      "email": {
        "enabled": false,
        "smtp": {
          "host": "smtp.example.com",
          "port": 587,
          "secure": true,
          "auth": {
            "user": "notifications@example.com",
            "pass": "password"
          }
        },
        "from": "Mediavision <notifications@example.com>",
        "to": ["admin@example.com"]
      }
    },
    "filters": {
      "minSeverity": "info",
      "types": ["stream.*", "detection.*"],
      "devices": ["amb82-001"]
    }
  }
}
```

### Notification Delivery Methods

| Method | Description | Use Case |
|--------|-------------|----------|
| **Webhook** | HTTP POST to custom endpoint | Integration with custom systems |
| **MQTT** | Publish to MQTT broker | IoT systems, real-time dashboards |
| **Email** | Send email notifications | Alerts, daily summaries |
| **Push** | Mobile push notifications | Mobile app alerts |
| **WebSocket** | Real-time browser notifications | Web dashboards |

## Quick Start

### Basic Setup for AMB82 Mini

1. **Configure your AMB82 Mini** to send RTSP stream:
   ```cpp
   // AMB82 Mini Arduino code example
   rtsp_server.begin("stream", 1920, 1080, 30);
   ```

2. **Configure Mediavision** to receive the stream:
   ```json
   {
     "rtsp": {
       "url": "rtsp://192.168.1.100:554/stream"
     }
   }
   ```

3. **Enable notifications** for stream events:
   ```json
   {
     "notifications": {
       "enabled": true,
       "methods": {
         "webhook": {
           "enabled": true,
           "url": "https://your-server.com/webhook"
         }
       }
     }
   }
   ```

## Troubleshooting

### AMB82 Mini Connection Issues

**Problem**: Cannot connect to AMB82 Mini RTSP stream

**Solutions**:
- Verify the AMB82 Mini is on the same network
- Check the RTSP URL and port (default: 554)
- Ensure firewall allows RTSP traffic
- Try using TCP transport instead of UDP
- Verify the AMB82 Mini RTSP server is running

**Problem**: Stream is choppy or disconnects frequently

**Solutions**:
- Check network bandwidth
- Reduce video resolution or frame rate on AMB82 Mini
- Use TCP transport for more reliable connection
- Enable reconnect option in configuration

### Notification Issues

**Problem**: Notifications not being received

**Solutions**:
- Verify notification method is enabled in configuration
- Check webhook URL is accessible
- Verify MQTT broker connection
- Check notification filters are not blocking events
- Review logs for delivery errors

## Technical Requirements

### Minimum Requirements
- Network connectivity to RTSP source
- Support for H.264 video decoding
- 2GB RAM minimum
- 1GB free disk space

### Recommended Requirements
- Gigabit Ethernet or 802.11ac WiFi
- 4GB RAM or more
- SSD storage for recording
- Multi-core processor

## Support

For issues, questions, or contributions, please visit the [GitHub repository](https://github.com/sameer07Es/Mediavision).

## License

See LICENSE file for details.
