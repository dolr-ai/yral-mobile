# File I/O Library

A cross-platform file I/O library for Kotlin Multiplatform that provides file download functionality with platform-specific implementations for Android and iOS.

## Overview

This library provides a unified interface for downloading files across Android and iOS platforms. It handles platform-specific storage APIs, permissions, and file type detection automatically.

## Features

- **Cross-platform API**: Single interface for both Android and iOS
- **Gallery/Photo Library support**: Automatically saves media to device galleries
- **Smart file type detection**: Handles videos, images, and documents differently
- **MediaStore integration** (Android): Uses modern MediaStore API for Android Q+
- **Photo Library integration** (iOS): Uses PHPhotoLibrary for iOS
- **Automatic cleanup**: Manages temporary files automatically
- **Error handling**: Returns typed Result for proper error handling

## Architecture

```
file-io/
├── commonMain/
│   └── FileDownloader.kt          # Interface and common types
├── androidMain/
│   └── AndroidFileDownloader.kt   # Android implementation
└── iosMain/
    └── IosFileDownloader.kt       # iOS implementation
```

## API Reference

### FileDownloader Interface

```kotlin
interface FileDownloader {
    suspend fun downloadFile(
        url: String,              // URL to download from
        fileName: String,         // Target filename with extension
        saveToGallery: Boolean    // true: device gallery, false: app storage
    ): Result<String, YralException>  // Returns local path or error
}
```

### FileType Enum

Automatically determined from file extension:
- `VIDEO`: mp4, mov, avi, mkv, webm, m4v
- `IMAGE`: jpg, jpeg, png, gif, webp, bmp, heic
- `DOCUMENT`: pdf, doc, docx, txt, csv, xls, xlsx
- `OTHER`: All other extensions

## Usage Examples

### Basic Download

```kotlin
class MyViewModel(
    private val fileDownloader: FileDownloader,
) : ViewModel() {
    
    fun downloadVideo(url: String, videoId: String) {
        viewModelScope.launch {
            fileDownloader.downloadFile(
                url = url,
                fileName = "YRAL_$videoId.mp4",
                saveToGallery = true
            ).onSuccess { path ->
                showToast("Video saved to gallery")
            }.onFailure { error ->
                showToast("Download failed: ${error.message}")
            }
        }
    }
}
```

### Download with UI Feedback

```kotlin
fun downloadWithProgress(videoUrl: String, videoId: String) {
    viewModelScope.launch {
        ToastManager.showInfo(ToastType.Small("Downloading..."))
        
        fileDownloader.downloadFile(
            url = videoUrl,
            fileName = "video_$videoId.mp4",
            saveToGallery = true
        ).onSuccess {
            ToastManager.showSuccess(ToastType.Small("Download complete!"))
        }.onFailure { error ->
            ToastManager.showError(ToastType.Small("Download failed"))
            crashlytics.recordException(error)
        }
    }
}
```

## Platform-Specific Behavior

### Android Implementation

**Download Process:**
1. Downloads file to app's cache directory
2. Determines file type from extension
3. Saves to appropriate MediaStore collection
4. Cleans up temporary cache file

**Storage Locations (saveToGallery = true):**
- **API 29+** (Scoped Storage):
  - Videos → `Movies/YRAL/filename.mp4`
  - Images → `Pictures/YRAL/filename.jpg`
  - Documents → `Downloads/YRAL/filename.pdf`
- **API 28-** (Legacy Storage):
  - Uses external storage directories
  - Registers files with MediaStore

**Storage Locations (saveToGallery = false):**
- App internal storage: `<app>/files/downloads/filename`

**Technical Details:**
- Uses `ContentResolver` and `MediaStore` APIs
- Handles scoped storage properly
- Returns MediaStore URI for API 29+
- Returns file path for API 28-

### iOS Implementation

**Download Process:**
1. Downloads file using `NSData.dataWithContentsOfURL`
2. Saves to temporary directory
3. Checks photo library permissions
4. Saves to Photo Library if authorized

**Storage Locations:**
- **saveToGallery = true**: iOS Photo Library
- **saveToGallery = false**: NSTemporaryDirectory

**Technical Details:**
- Uses `PHPhotoLibrary` and `PHAssetCreationRequest`
- Checks authorization status before saving
- Supports VIDEO and IMAGE types in Photo Library
- Uses Kotlin/Native iOS interop (no Swift bridge needed)

## Setup & Integration

### 1. Add Dependency

In your module's `build.gradle.kts`:

```kotlin
commonMain.dependencies {
    implementation(projects.shared.libs.fileIo)
}
```

### 2. Register DI Module

In your app initialization:

```kotlin
import com.yral.shared.libs.fileio.di.fileIoModule

fun initKoin() {
    startKoin {
        modules(
            // ... other modules
            fileIoModule,
        )
    }
}
```

### 3. Inject in ViewModel

```kotlin
class ProfileViewModel(
    private val fileDownloader: FileDownloader,
    // ... other dependencies
) : ViewModel() {
    // Use fileDownloader
}
```

## Permissions

### Android Manifest

```xml
<!-- Required for network downloads -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- Only needed for API 28 and below -->
<uses-permission 
    android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```

**Note:** Android API 29+ (Q) uses scoped storage and doesn't require WRITE_EXTERNAL_STORAGE permission.

### iOS Info.plist

```xml
<!-- Required for saving to Photo Library -->
<key>NSPhotoLibraryAddUsageDescription</key>
<string>Save downloaded videos and photos to your library</string>

<!-- Optional: If you also need to read from library -->
<key>NSPhotoLibraryUsageDescription</key>
<string>Access your photo library</string>
```

## Error Handling

All errors are wrapped in `YralException`:

```kotlin
fileDownloader.downloadFile(url, filename, true)
    .onSuccess { path ->
        // path: String - local file path or URI
    }
    .onFailure { error: YralException ->
        // Handle various error cases:
        // - Network errors (invalid URL, connection failed)
        // - Permission errors (gallery access denied)
        // - Storage errors (insufficient space, write failed)
    }
```

## Testing

Mock the `FileDownloader` interface in tests:

```kotlin
class FakeFileDownloader : FileDownloader {
    override suspend fun downloadFile(
        url: String,
        fileName: String,
        saveToGallery: Boolean
    ) = Ok("/fake/path/$fileName")
}
```

## Implementation Notes

### Why No Swift Bridge?

The iOS implementation uses Kotlin/Native's excellent iOS interop directly:
- Direct access to `PHPhotoLibrary`, `NSData`, `NSURL` APIs
- No need for Swift wrapper code
- Simpler architecture and maintenance
- All code stays in Kotlin

### Temporary File Management

Both platforms:
1. Download to temporary location first
2. Move to final destination
3. Clean up temporary file automatically

This ensures:
- Atomic operations (file appears complete)
- No partial downloads in gallery
- Proper error recovery

### File Type Detection

Uses file extension to determine type. Extension function:

```kotlin
fun String.getFileType(): FileType {
    val extension = this.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "mp4", "mov", ... -> FileType.VIDEO
        "jpg", "jpeg", ... -> FileType.IMAGE
        "pdf", "doc", ... -> FileType.DOCUMENT
        else -> FileType.OTHER
    }
}
```

## Future Enhancements

Potential features for future versions:

- **Progress tracking**: Stream download progress
- **Resumable downloads**: Support pause/resume
- **File uploads**: Upload files to servers
- **File reading**: Read file contents
- **Batch operations**: Download multiple files
- **Compression**: Zip/unzip files
- **File sharing**: Share files with other apps
- **Custom storage**: Configurable storage locations
- **Validation**: Verify file integrity (checksums)

## Troubleshooting

**Android: Files not appearing in gallery**
- Ensure `saveToGallery = true`
- Check MediaStore permissions
- Use MediaScanner on older Android versions

**iOS: Permission denied errors**
- Add `NSPhotoLibraryAddUsageDescription` to Info.plist
- Request permission before first download
- Check Settings > Privacy > Photos

**Both: Download fails silently**
- Check network connectivity
- Verify URL is valid and accessible
- Check device storage space
- Review logs for detailed error messages

