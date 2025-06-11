# Firebase Performance Monitoring for ExoPlayer

This implementation adds four key Firebase Performance traces to monitor different phases of video playback performance:

## 1. Video Download Time
**Trace Name:** `VideoDownload`
- **What it measures:** Time taken to download/fetch video content from network
- **Start:** When performance tracing is set up (before `ExoPlayer.prepare()` is called)
- **End:** When player reaches `STATE_READY` (network data received and ready to play)
- **Attributes:**
  - `video_id`: The ID extracted from the video URL
  - `video_format`: Either "hls" or "progressive"
  - `result`: "success" or "error"

## 2. Video Load Time
**Trace Name:** `VideoStartup`
- **What it measures:** Time taken for decoder initialization and preparation
- **Start:** When performance tracing is set up (before `ExoPlayer.prepare()` is called)
- **End:** When player reaches `STATE_BUFFERING` (decoder initialized and buffering starts)
- **Attributes:**
  - `video_id`: The ID extracted from the video URL
  - `video_format`: Either "hls" or "progressive"
  - `result`: "success" or "error"

## 3. First Frame Render Time
**Trace Name:** `FirstFrame`
- **What it measures:** Time taken from when video becomes visible to first frame render
- **Start:** When video becomes visible (`startFirstFrameTraceForUrl` is called)
- **End:** When `onRenderedFirstFrame()` callback is triggered
- **Attributes:**
  - `video_id`: The ID extracted from the video URL
  - `video_format`: Either "hls" or "progressive"
  - `result`: "success" or "error"

## 4. Video Playback Time
**Trace Name:** `VideoPlayback`
- **What it measures:** Time taken for complete video playback from start to end
- **Start:** When video actually starts playing (`onIsPlayingChanged(true)`)
- **End:** When player reaches `STATE_ENDED` (video completed)
- **Attributes:**
  - `video_id`: The ID extracted from the video URL
  - `video_format`: Either "hls" or "progressive"
  - `buffering_count`: Number of buffering events during playback
  - `seek_during_playback`: "true" if user seeked backwards during playback
  - `result`: "success" or "error"

## Implementation Details

### Trace Classes:
- `DownloadTrace`, `LoadTimeTrace`, `FirstFrameTrace`, `PrefetchDownloadTrace`, `PrefetchLoadTimeTrace`, and `PlaybackTimeTrace` are defined to track different aspects of video performance.
- Each trace class initializes with attributes like `video_id` and `video_format`, and sets the module to "video_player".

### Factory Interface:
- `VideoPerformanceFactory` interface defines methods to create instances of each trace class.
- `VideoPerformanceFactoryProvider` implements this interface, providing concrete methods to create each trace.

### Constants:
- `VideoPerformanceConstants` object holds constants for trace names, attribute keys, and metric names.

### Attributes:
- Attributes include `video_id`, `video_format`, and `player_state`.
- Video formats are identified as either "hls" or "progressive".

### Trace Names:
- Trace names include `VideoDownload`, `VideoStartup`, `FirstFrame`, and `VideoPlayback`.

### ExoPlayer States and Trace Lifecycle:
1. **Performance tracing setup** → Download trace and Load trace start
2. **ExoPlayer.prepare() called** → Media source setup and decoder initialization
3. **STATE_BUFFERING** → Load trace ends (decoder initialized and buffering starts)
4. **STATE_READY** → Download trace ends (network data received and ready to play)
5. **Video becomes visible** → First frame trace starts
6. **onRenderedFirstFrame()** → First frame trace ends
7. **onIsPlayingChanged(true)** → Playback time trace starts
8. **STATE_ENDED** → Playback time trace ends (video completed)
9. **Errors** → All active traces stop with error result

### Distinct Phase Measurements:
- **Download Time**: Network performance and data fetching (setup → ready)
- **Load Time**: Device decoder performance and initialization (setup → buffering)
- **First Frame Time**: Time from visibility to first frame render (visible → rendered)
- **Playback Time**: Complete video playback duration (playing → ended)
- Each trace measures a different aspect with **no duplicate creation**

### Key Timing Benefits:
- **Download trace** measures pure network/CDN performance
- **Load trace** measures device-specific decoder performance
- **First frame trace** measures rendering performance from visibility to display
- **Playback time trace** measures complete playback experience
- **Single trace per video**: Each video URL creates exactly one trace of each type

### Prefetch Support:
- Prefetch players have simplified performance monitoring
- Only `VideoDownload_prefetch` and `VideoStartup_prefetch` are tracked for background video loading
- Focuses on network/caching performance since videos aren't immediately played
- Helps monitor preloading effectiveness and CDN performance
- No first frame or playback traces for prefetch since videos aren't displayed until played

### Usage:
The traces are automatically initialized and managed within the `rememberExoPlayerWithLifecycle` and `rememberPrefetchExoPlayerWithLifecycle` composables. No additional setup is required by the calling code.

### Firebase Console:
View the traces in Firebase Console → Performance → Custom traces:
- Look for traces named `VideoDownload`, `VideoStartup`, `FirstFrame`, `VideoPlayback`
- Filter by attributes like `video_format` to analyze HLS vs Progressive performance
- Monitor success/error rates and performance trends over time 