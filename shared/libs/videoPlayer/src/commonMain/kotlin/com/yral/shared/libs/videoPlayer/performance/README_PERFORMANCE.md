# Firebase Performance Monitoring for ExoPlayer

This implementation adds three key Firebase Performance traces to monitor video playback performance:

## 1. Video Download Time
**Trace Name:** `video_download_time`
- **What it measures:** Time taken to download/fetch the video content and buffer enough data
- **Start:** When media source loading begins (`LaunchedEffect(url)`)
- **End:** When player reaches `STATE_READY` (buffering complete, sufficient data downloaded)
- **Attributes:**
  - `video_url`: The URL of the video
  - `video_format`: Either "hls" or "progressive"
  - `result`: "success" or "error"

## 2. Video Load Time
**Trace Name:** `video_load_time`
- **What it measures:** Time taken for decoders to initialize and prepare video
- **Start:** When `ExoPlayer.prepare()` is called (decoder initialization begins)
- **End:** When player reaches `STATE_READY` (decoder ready)
- **Attributes:**
  - `video_url`: The URL of the video
  - `video_format`: Either "hls" or "progressive"
  - `result`: "success" or "error"

## 3. First Frame Render Time
**Trace Name:** `first_frame_render_time`
- **What it measures:** Time taken for first frame to be ready for display
- **Start:** When player enters `STATE_BUFFERING` (actively preparing to render)
- **End:** When player reaches `STATE_READY` (ready to display first frame)
- **Attributes:**
  - `video_url`: The URL of the video
  - `video_format`: Either "hls" or "progressive"
  - `result`: "success" or "error"

## Implementation Details

### ExoPlayer States and Trace Lifecycle:
1. **LaunchedEffect starts** → Download trace starts, Load trace starts
2. **ExoPlayer.prepare() called** → Decoder initialization begins
3. **STATE_BUFFERING** → First frame trace starts (preparing to render)
4. **STATE_READY** → All traces end (download complete, decoder ready, first frame ready)
5. **Errors** → All active traces stop with error result

### Key Timing Changes:
- **Download trace** now properly measures until buffering is complete (`STATE_READY`)
- **Load trace** measures decoder preparation time
- **First frame trace** measures from buffering start to ready state
- All traces stop simultaneously at `STATE_READY` for accurate measurements

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