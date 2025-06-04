# Performance Monitoring Module

A cross-platform performance monitoring module that provides Firebase Performance Monitoring capabilities for the Yral mobile app.

## Overview

This module provides a unified interface for performance monitoring across different platforms:
- **Android**: Uses Firebase Performance Monitoring
- **iOS**: Can be extended to use iOS performance monitoring tools

## Architecture

```
PerformanceMonitor (Interface)
    ↓
FirebasePerformanceMonitor (Android Implementation)
    ↓
OperationTrace (Base class with common functionality)
    ↓
Your custom trace classes
```

## Basic Usage

### 1. Create Custom Trace Classes

```kotlin
class MyFeatureLoadTrace(url: String) : OperationTrace("my_feature_load_time") {
    init {
        setModule("my_feature")
        setOperation("load")
        putAttribute("url", url)
    }
}
```

### 2. Use in Your Code

```kotlin
// Start trace
val trace = MyFeatureLoadTrace(url).apply { start() }

// ... perform your operation ...

// Mark as successful
trace.success()

// Or mark as failed
trace.error()
```

## Key Features

### OperationTrace Base Class

The `OperationTrace` class provides common functionality:
- `success()` - Mark trace as successful and stop
- `error()` - Mark trace as failed and stop
- `setModule(module: String)` - Set module identifier
- `setOperation(operation: String)` - Set operation identifier

### Common Constants

Use `PerformanceConstants` for standard attribute keys and values:
- `RESULT_KEY`, `SUCCESS_VALUE`, `ERROR_VALUE`
- `URL_KEY`, `MODULE_KEY`, `OPERATION_KEY`
- `DURATION_MS`, `COUNT_METRIC`

## Example: Video Player Module

See the `VideoTrace` class in the video player module for an example of how to extend `OperationTrace` for domain-specific functionality:

```kotlin
abstract class VideoTrace(
    traceName: String,
    url: String,
) : OperationTrace(traceName) {
    
    init {
        // Set video-specific attributes
        putAttribute("video_url", url)
        putAttribute("video_format", if (isHlsUrl(url)) "hls" else "progressive")
        setModule("video_player")
    }
}

class DownloadTrace(url: String): VideoTrace("video_download_time", url)
class LoadTimeTrace(url: String): VideoTrace("video_load_time", url)
```

## Integration

### 1. Add Dependency

In your module's `build.gradle.kts`:

```kotlin
sourceSets {
    commonMain.dependencies {
        implementation(projects.shared.libs.performance)
    }
}
```

### 2. Platform-Specific Setup

**Android**: Firebase Performance is automatically included via the performance module.

**iOS**: Can be extended with iOS-specific performance monitoring tools.

## Best Practices

1. **Use descriptive trace names**: e.g., `"feature_name_operation_type"`
2. **Set module identifiers**: Use `setModule()` to group related traces
3. **Add relevant attributes**: Include context like URLs, user IDs, etc.
4. **Handle errors**: Always call `error()` in exception handling
5. **Clean up**: Ensure traces are stopped to avoid memory leaks

## Performance Monitoring in Firebase Console

Traces will appear in the Firebase Console under:
- Performance → Dashboard
- Performance → Traces

You can filter by:
- Module (using the `module` attribute)
- Operation (using the `operation` attribute)
- Result (success/error)

## Example Trace Structure

```
Trace Name: video_download_time
Attributes:
  - module: video_player
  - operation: download
  - video_url: https://example.com/video.mp4
  - video_format: progressive
  - result: success
Metrics:
  - duration_ms: 1500
``` 