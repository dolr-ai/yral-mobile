# Firebase Console Guide for Video Performance Traces

## How to View Video Performance Traces

### 1. Access Firebase Console
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project (Yral Mobile)
3. In the left sidebar, click on **Performance**

### 2. Navigate to Custom Traces
1. In the Performance dashboard, look for the **Custom traces** section
2. You should see these video performance traces:
   - `video_download_time` - Download and buffering performance
   - `video_load_time` - Decoder initialization performance
   - `first_frame_render_time` - Time to first frame ready
   - `video_download_time_prefetch` - Prefetch download performance
   - `video_load_time_prefetch` - Prefetch load performance

### 3. Analyze Trace Data
Click on any trace to see detailed metrics:

#### Key Metrics to Monitor:
- **Duration**: Average time for each trace
- **Success Rate**: Percentage of successful vs failed traces
- **90th Percentile**: Performance for slower devices/connections
- **Device Type**: Performance differences across devices
- **Network Type**: Performance on WiFi vs mobile data

#### Filtering Options:
- **video_format**: Filter by "hls" vs "progressive" videos
- **result**: Filter by "success" vs "error" traces
- **video_url**: Analyze specific video URLs
- **module**: Filter by "video_player" module
- **App Version**: Compare performance across app versions
- **Country**: Regional performance differences

### 4. Performance Insights

#### What Good Performance Looks Like:
- **Video Download Time**: 
  - **Excellent**: < 1 second (fast network, small videos)
  - **Good**: 1-3 seconds (typical performance)
  - **Poor**: > 5 seconds (slow network or large files)
  
- **Video Load Time**: 
  - **Excellent**: < 500ms (decoder initialization)
  - **Good**: 500ms-1 second
  - **Poor**: > 2 seconds (device performance issues)
  
- **First Frame Render Time**: 
  - **Excellent**: < 1 second (from buffering start to ready)
  - **Good**: 1-2 seconds
  - **Poor**: > 3 seconds (combined performance issues)

#### Understanding the Trace Relationships:
- **Download Time** measures network and buffering performance
- **Load Time** measures device/decoder performance 
- **First Frame Time** measures rendering preparation time
- All three traces end simultaneously at `STATE_READY`

#### Red Flags to Watch For:
- High error rates (> 5%)
- Increasing average durations over time
- Significant performance differences between HLS and Progressive
- Poor performance on specific device types or regions
- Download time much higher than load time (network issues)
- Load time much higher than download time (device issues)

### 5. Setting Up Alerts
1. In Firebase Performance, go to **Alerts**
2. Create custom alerts for:
   - Duration thresholds (e.g., if video_download_time > 10 seconds)
   - Error rate increases (e.g., if error rate > 10%)
   - Performance degradation over time

### 6. Troubleshooting Common Issues

#### High Download Times:
- Check CDN performance and availability
- Verify video file sizes and encoding
- Analyze network conditions by region
- Consider adaptive bitrate streaming

#### High Load Times:
- Device performance issues (older devices)
- Decoder initialization problems
- Memory constraints
- Too many concurrent players

#### High First Frame Render Times:
- Usually indicates combined download + load issues
- Buffer size optimization needed
- Video format optimization
- Check if progressive vs HLS makes a difference

#### Performance Analysis Tips:
1. **Compare Progressive vs HLS**: Use `video_format` filter
2. **Network Impact**: Compare WiFi vs mobile performance
3. **Device Impact**: Filter by device model/type
4. **Regional Issues**: Check performance by country
5. **Time-based Analysis**: Look at performance trends over time

### 7. Comparison with iOS
Your iOS implementation uses similar trace names:
- **iOS**: `VideoStartup`, `FirstFrame`, `VideoPlayback`
- **Android**: `video_download_time`, `video_load_time`, `first_frame_render_time`

Both implementations track similar metrics, allowing cross-platform performance comparison:
- iOS `VideoStartup` ≈ Android `video_download_time`
- iOS `FirstFrame` ≈ Android `first_frame_render_time`

### Sample Firebase Performance Query
```
// Example: Get all failed video downloads in last 24 hours
Filter: 
- Trace: video_download_time
- Attribute: result = "error"
- Time range: Last 24 hours

// Example: Compare HLS vs Progressive performance
Filter:
- Trace: video_download_time
- Attribute: video_format = "hls" OR "progressive"
- Time range: Last 7 days
```

### 8. Performance Optimization Recommendations

Based on trace data, consider:

1. **For High Download Times**:
   - Implement adaptive bitrate streaming
   - Optimize CDN configuration
   - Consider video compression improvements

2. **For High Load Times**:
   - Profile decoder performance on target devices
   - Consider hardware acceleration settings
   - Optimize buffer sizes

3. **For High First Frame Times**:
   - Reduce initial buffer requirements
   - Optimize video encoding for faster start
   - Consider preloading strategies 