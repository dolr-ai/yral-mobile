# Firebase Performance Monitoring for ExoPlayer

This implementation adds three key Firebase Performance traces to monitor different phases of video playback performance:

## 1. Video Download Time
**Trace Name:** `video_download_time`
- **What it measures:** Time taken to download/fetch video content from network
- **Start:** When media source loading begins (`LaunchedEffect(url)`)
- **End:** When player reaches `STATE_BUFFERING` (network data received, buffering starts)
- **Attributes:**
  - `video_url`: The URL of the video
  - `video_format`: Either "hls" or "progressive"
  - `result`: "success" or "error"

## 2. Video Load Time
**Trace Name:** `video_load_time`
- **What it measures:** Time taken for decoder initialization and preparation
- **Start:** When `ExoPlayer.prepare()` is called (decoder initialization begins)
- **End:** When player reaches `STATE_READY` (decoder ready to play)
- **Attributes:**
  - `video_url`: The URL of the video
  - `video_format`: Either "hls" or "progressive"
  - `result`: "success" or "error"

## 3. First Frame Render Time
**Trace Name:** `first_frame_render_time`
- **What it measures:** Time taken for complete video initialization to first frame readiness
- **Start:** When video loading begins (`LaunchedEffect(url)`) - after prepare() is called
- **End:** When player reaches `STATE_READY` (ready to display first frame)
- **Attributes:**
  - `video_url`: The URL of the video
  - `video_format`: Either "hls" or "progressive"
  - `result`: "success" or "error"

## Implementation Details

### ExoPlayer States and Trace Lifecycle:
1. **LaunchedEffect starts** → Download trace starts
2. **ExoPlayer.prepare() called** → Load trace starts (decoder initialization)
3. **After prepare()** → First frame trace starts (complete initialization to ready)
4. **STATE_BUFFERING** → Download trace ends (network data received)
5. **STATE_READY** → Load trace and First frame trace end (everything ready)
6. **Errors** → All active traces stop with error result

### Distinct Phase Measurements:
- **Download Time**: Network performance and data fetching (start → buffering)
- **Load Time**: Device decoder performance and initialization (prepare → ready)
- **First Frame Time**: Complete initialization to ready (prepare → ready)
- Each trace measures a different aspect with **no duplicate creation**

### Key Timing Benefits:
- **Download trace** measures pure network/CDN performance
- **Load trace** measures device-specific decoder performance
- **First frame trace** measures total time from decoder start to display ready
- **Single trace per video**: Each video URL creates exactly one trace of each type

### Prefetch Support:
- Prefetch players also have performance monitoring
- Prefetch traces are suffixed with "_prefetch"
- Same timing logic applies for background video loading performance
- Helps monitor preloading effectiveness

### Usage:
The traces are automatically initialized and managed within the `rememberExoPlayerWithLifecycle` and `rememberPrefetchExoPlayerWithLifecycle` composables. No additional setup is required by the calling code.

### Firebase Console:
View the traces in Firebase Console → Performance → Custom traces:
- Look for traces named `video_download_time`, `video_load_time`, `first_frame_render_time`
- Filter by attributes like `video_format` to analyze HLS vs Progressive performance
- Monitor success/error rates and performance trends over time 