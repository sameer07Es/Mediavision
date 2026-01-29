# Notification System Documentation

## Overview

The Mediavision notification system provides real-time alerts for stream events, detection events, and system status changes. Notifications can be delivered through multiple channels including webhooks, MQTT, email, and push notifications.

## Notification Architecture

```
[Event Source] → [Event Processor] → [Notification Engine] → [Delivery Methods]
     ↓                    ↓                      ↓                    ↓
  Streams           Filter & Route          Format               Webhook
  Detectors         Priority                Serialize            MQTT
  System            Throttle                Validate             Email
                                                                Push
```

## Notification Format Specification

### Base Notification Schema

All notifications follow this JSON schema:

```json
{
  "id": "string (required) - Unique notification identifier",
  "timestamp": "string (required) - ISO 8601 timestamp",
  "type": "string (required) - Event type in dot notation",
  "severity": "string (required) - info|warning|error|critical",
  "source": {
    "deviceId": "string (required) - Device identifier",
    "streamUrl": "string (optional) - RTSP stream URL",
    "deviceName": "string (optional) - Human-readable device name"
  },
  "event": {
    "name": "string (required) - Human-readable event name",
    "description": "string (required) - Event description",
    "data": "object (optional) - Event-specific data"
  },
  "metadata": {
    "location": "string (optional) - Device location",
    "tags": "array (optional) - Event tags",
    "custom": "object (optional) - Custom metadata"
  }
}
```

### Field Descriptions

#### Top-Level Fields
- **id**: Unique identifier for the notification (UUID recommended)
- **timestamp**: When the event occurred in ISO 8601 format (e.g., "2026-01-29T08:00:00.000Z")
- **type**: Event type using dot notation for categorization
- **severity**: Event severity level

#### Source Object
- **deviceId**: Unique identifier for the device that generated the event
- **streamUrl**: RTSP or other stream URL (optional)
- **deviceName**: Human-readable device name

#### Event Object
- **name**: Short, human-readable event name
- **description**: Longer description of what happened
- **data**: Event-specific payload with additional details

#### Metadata Object
- **location**: Physical location of the device
- **tags**: Array of tags for filtering and categorization
- **custom**: Additional custom fields as needed

## Event Types

### Stream Events

#### stream.connected
Triggered when a stream successfully connects.

```json
{
  "type": "stream.connected",
  "severity": "info",
  "event": {
    "name": "Stream Connected",
    "data": {
      "codec": "H.264",
      "resolution": "1920x1080",
      "fps": 30,
      "bitrate": 4000000
    }
  }
}
```

#### stream.disconnected
Triggered when a stream disconnects.

```json
{
  "type": "stream.disconnected",
  "severity": "warning",
  "event": {
    "name": "Stream Disconnected",
    "data": {
      "reason": "client_closed",
      "duration": 3600,
      "lastSeen": "2026-01-29T08:00:00.000Z"
    }
  }
}
```

#### stream.error
Triggered when a stream error occurs.

```json
{
  "type": "stream.error",
  "severity": "error",
  "event": {
    "name": "Stream Error",
    "data": {
      "errorCode": "ETIMEDOUT",
      "errorMessage": "Connection timeout",
      "retryAttempt": 3,
      "nextRetry": "2026-01-29T08:05:00.000Z"
    }
  }
}
```

#### stream.quality.changed
Triggered when stream quality changes significantly.

```json
{
  "type": "stream.quality.changed",
  "severity": "info",
  "event": {
    "name": "Stream Quality Changed",
    "data": {
      "previousFps": 30,
      "currentFps": 15,
      "previousBitrate": 4000000,
      "currentBitrate": 2000000,
      "reason": "network_congestion"
    }
  }
}
```

### Detection Events

#### detection.motion
Triggered when motion is detected in the video stream.

```json
{
  "type": "detection.motion",
  "severity": "warning",
  "event": {
    "name": "Motion Detected",
    "data": {
      "confidence": 0.95,
      "region": {
        "x": 100,
        "y": 150,
        "width": 200,
        "height": 300
      },
      "intensity": 0.75,
      "snapshotUrl": "https://storage.example.com/snapshots/snapshot-001.jpg",
      "videoClipUrl": "https://storage.example.com/clips/clip-001.mp4"
    }
  }
}
```

#### detection.object
Triggered when a specific object is detected.

```json
{
  "type": "detection.object",
  "severity": "info",
  "event": {
    "name": "Object Detected",
    "data": {
      "objectType": "person",
      "confidence": 0.92,
      "boundingBox": {
        "x": 150,
        "y": 200,
        "width": 100,
        "height": 250
      },
      "attributes": {
        "color": "blue",
        "size": "medium"
      }
    }
  }
}
```

#### detection.face
Triggered when a face is detected.

```json
{
  "type": "detection.face",
  "severity": "info",
  "event": {
    "name": "Face Detected",
    "data": {
      "confidence": 0.88,
      "boundingBox": {
        "x": 300,
        "y": 150,
        "width": 80,
        "height": 100
      },
      "recognized": false,
      "attributes": {
        "age": "adult",
        "expression": "neutral"
      }
    }
  }
}
```

### System Events

#### system.started
Triggered when the system starts.

```json
{
  "type": "system.started",
  "severity": "info",
  "event": {
    "name": "System Started",
    "data": {
      "version": "1.0.0",
      "hostname": "mediavision-server",
      "startTime": "2026-01-29T08:00:00.000Z"
    }
  }
}
```

#### system.stopped
Triggered when the system stops.

```json
{
  "type": "system.stopped",
  "severity": "warning",
  "event": {
    "name": "System Stopped",
    "data": {
      "reason": "user_initiated",
      "uptime": 86400,
      "stopTime": "2026-01-29T08:00:00.000Z"
    }
  }
}
```

#### system.storage.warning
Triggered when storage space is low.

```json
{
  "type": "system.storage.warning",
  "severity": "warning",
  "event": {
    "name": "Storage Warning",
    "data": {
      "totalSpace": 1000000000000,
      "usedSpace": 900000000000,
      "freeSpace": 100000000000,
      "percentUsed": 90,
      "threshold": 85
    }
  }
}
```

## Delivery Methods

### Webhook

HTTP POST notifications to a custom endpoint.

#### Configuration
```json
{
  "notifications": {
    "methods": {
      "webhook": {
        "enabled": true,
        "url": "https://your-server.com/webhook",
        "method": "POST",
        "headers": {
          "Authorization": "Bearer your-token",
          "Content-Type": "application/json"
        },
        "timeout": 5000,
        "retries": 3,
        "retryDelay": 1000
      }
    }
  }
}
```

#### Expected Response
Your webhook endpoint should return:
- **Success**: HTTP 200-299 status code
- **Retry**: HTTP 429, 500-599 status codes (will retry)
- **Failure**: HTTP 400-499 status codes (except 429)

#### Example Webhook Handler (Node.js)
```javascript
app.post('/webhook', (req, res) => {
  const notification = req.body;
  
  // Validate notification
  if (!notification.id || !notification.type) {
    return res.status(400).json({ error: 'Invalid notification' });
  }
  
  // Process notification
  console.log(`Received ${notification.type} from ${notification.source.deviceName}`);
  
  // Handle based on type
  switch (notification.type) {
    case 'detection.motion':
      handleMotionDetection(notification);
      break;
    case 'stream.error':
      handleStreamError(notification);
      break;
    default:
      console.log('Unhandled notification type:', notification.type);
  }
  
  // Acknowledge receipt
  res.status(200).json({ 
    received: true,
    id: notification.id,
    timestamp: new Date().toISOString()
  });
});
```

### MQTT

Publish notifications to an MQTT broker.

#### Configuration
```json
{
  "notifications": {
    "methods": {
      "mqtt": {
        "enabled": true,
        "broker": "mqtt://broker.example.com:1883",
        "clientId": "mediavision",
        "topic": "mediavision/notifications",
        "qos": 1,
        "retain": false,
        "authentication": {
          "username": "mqtt-user",
          "password": "mqtt-password"
        },
        "tls": {
          "enabled": false,
          "ca": "/path/to/ca.crt",
          "cert": "/path/to/client.crt",
          "key": "/path/to/client.key"
        }
      }
    }
  }
}
```

#### Topic Structure
```
mediavision/notifications                    # All notifications
mediavision/notifications/stream            # Stream events
mediavision/notifications/detection         # Detection events
mediavision/notifications/system            # System events
mediavision/notifications/devices/{id}      # Device-specific
```

#### Example MQTT Subscriber (Python)
```python
import paho.mqtt.client as mqtt
import json

def on_connect(client, userdata, flags, rc):
    print(f"Connected with result code {rc}")
    client.subscribe("mediavision/notifications/#")

def on_message(client, userdata, msg):
    notification = json.loads(msg.payload)
    print(f"Received {notification['type']} on topic {msg.topic}")
    
    if notification['type'] == 'detection.motion':
        print(f"Motion detected at {notification['timestamp']}")
        print(f"Confidence: {notification['event']['data']['confidence']}")

client = mqtt.Client()
client.on_connect = on_connect
client.on_message = on_message

client.connect("broker.example.com", 1883, 60)
client.loop_forever()
```

### Email

Send notifications via email.

#### Configuration
```json
{
  "notifications": {
    "methods": {
      "email": {
        "enabled": true,
        "smtp": {
          "host": "smtp.gmail.com",
          "port": 587,
          "secure": false,
          "auth": {
            "user": "notifications@example.com",
            "pass": "app-specific-password"
          }
        },
        "from": "Mediavision <notifications@example.com>",
        "to": ["admin@example.com", "security@example.com"],
        "subject": "[Mediavision] {severity}: {type}",
        "template": "html"
      }
    }
  }
}
```

#### Email Template Variables
- `{severity}` - Event severity
- `{type}` - Event type
- `{name}` - Event name
- `{deviceName}` - Source device name
- `{timestamp}` - Event timestamp

### Push Notifications

Send push notifications to mobile devices.

#### Configuration
```json
{
  "notifications": {
    "methods": {
      "push": {
        "enabled": true,
        "provider": "fcm",
        "credentials": {
          "serverKey": "your-fcm-server-key"
        },
        "tokens": [
          "device-token-1",
          "device-token-2"
        ]
      }
    }
  }
}
```

## Filtering and Routing

### Severity Filtering
```json
{
  "notifications": {
    "filters": {
      "minSeverity": "warning",
      "severityRouting": {
        "info": ["webhook"],
        "warning": ["webhook", "mqtt"],
        "error": ["webhook", "mqtt", "email"],
        "critical": ["webhook", "mqtt", "email", "push"]
      }
    }
  }
}
```

### Type Filtering
```json
{
  "notifications": {
    "filters": {
      "types": [
        "stream.*",
        "detection.motion",
        "detection.object",
        "system.storage.*"
      ],
      "exclude": [
        "stream.quality.changed"
      ]
    }
  }
}
```

### Device Filtering
```json
{
  "notifications": {
    "filters": {
      "devices": ["amb82-001", "amb82-002"],
      "locations": ["Front Door", "Back Yard"],
      "tags": ["security", "critical"]
    }
  }
}
```

### Time-Based Filtering
```json
{
  "notifications": {
    "filters": {
      "schedule": {
        "enabled": true,
        "timeZone": "America/New_York",
        "rules": [
          {
            "days": ["monday", "tuesday", "wednesday", "thursday", "friday"],
            "startTime": "09:00",
            "endTime": "17:00",
            "types": ["detection.motion"]
          },
          {
            "days": ["saturday", "sunday"],
            "allDay": true,
            "types": ["*"]
          }
        ]
      }
    }
  }
}
```

## Rate Limiting and Throttling

### Configuration
```json
{
  "notifications": {
    "rateLimit": {
      "enabled": true,
      "maxPerMinute": 60,
      "maxPerHour": 1000,
      "throttle": {
        "enabled": true,
        "duplicateWindow": 300,
        "burstWindow": 60,
        "burstMax": 10
      }
    }
  }
}
```

### Behavior
- **maxPerMinute**: Maximum notifications per minute across all types
- **maxPerHour**: Maximum notifications per hour across all types
- **duplicateWindow**: Suppress duplicate notifications within N seconds
- **burstWindow**: Time window (seconds) for burst detection
- **burstMax**: Maximum notifications within burst window

## Testing Notifications

### Test Notification Endpoint

Send a test notification to verify configuration:

```bash
curl -X POST http://localhost:8080/api/notifications/test \
  -H "Content-Type: application/json" \
  -d '{
    "type": "system.test",
    "severity": "info",
    "methods": ["webhook", "mqtt"]
  }'
```

### Sample Test Notification
```json
{
  "id": "test-12345",
  "timestamp": "2026-01-29T08:00:00.000Z",
  "type": "system.test",
  "severity": "info",
  "source": {
    "deviceId": "test-device",
    "deviceName": "Test Device"
  },
  "event": {
    "name": "Test Notification",
    "description": "This is a test notification to verify configuration",
    "data": {
      "test": true
    }
  }
}
```

## Best Practices

### For Webhook Handlers
1. **Respond quickly**: Return 200 status within 5 seconds
2. **Process asynchronously**: Queue notifications for processing
3. **Validate signatures**: Verify notification authenticity
4. **Handle duplicates**: Use notification ID for deduplication
5. **Log failures**: Track and investigate failed notifications

### For MQTT Subscribers
1. **Use QoS 1 or 2**: Ensure reliable delivery
2. **Subscribe to specific topics**: Reduce unnecessary traffic
3. **Handle reconnections**: Implement reconnection logic
4. **Persistent sessions**: Use clean session = false for reliability
5. **Monitor broker health**: Track connection status

### For Email Notifications
1. **Rate limit**: Avoid overwhelming recipients
2. **Batch notifications**: Combine similar events
3. **Use digests**: Send summary emails instead of individual alerts
4. **Unsubscribe option**: Include opt-out mechanism
5. **Mobile-friendly**: Use responsive email templates

### Security
1. **Use HTTPS**: Always use encrypted connections
2. **Authenticate**: Require authentication on notification endpoints
3. **Validate input**: Sanitize all notification data
4. **Rate limit**: Prevent abuse and DoS attacks
5. **Audit logs**: Keep logs of all notification deliveries

## Troubleshooting

### Notifications Not Being Received

1. **Check configuration**:
   ```bash
   # Verify notification config is loaded
   curl http://localhost:8080/api/notifications/config
   ```

2. **Test connectivity**:
   ```bash
   # Test webhook endpoint
   curl -X POST https://your-server.com/webhook \
     -H "Content-Type: application/json" \
     -d '{"test": true}'
   
   # Test MQTT broker
   mosquitto_pub -h broker.example.com -t test -m "test"
   ```

3. **Check logs**:
   ```bash
   # View notification logs
   tail -f /var/log/mediavision/notifications.log
   ```

4. **Verify filters**: Ensure filters aren't blocking notifications

5. **Check rate limits**: Verify not hitting rate limits

### High Notification Volume

1. **Adjust filters**: Increase minSeverity threshold
2. **Enable throttling**: Use duplicate suppression
3. **Tune detection sensitivity**: Reduce false positives
4. **Use batching**: Combine notifications into digests
5. **Review event sources**: Disable noisy event types

## API Reference

### POST /api/notifications/test
Test notification delivery.

### GET /api/notifications/config
Get current notification configuration.

### PUT /api/notifications/config
Update notification configuration.

### GET /api/notifications/history
Get notification history.

### GET /api/notifications/stats
Get notification statistics.

For more information, see the main README.md file.
