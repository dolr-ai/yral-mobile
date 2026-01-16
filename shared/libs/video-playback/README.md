# video-playback

A Kotlin Multiplatform library that provides a lightweight, reusable video playback stack for Compose Multiplatform (Android + iOS). It is designed for vertically paged video feeds, but the core pieces are generic and can be reused for other swipe or paging UIs.

## What it provides

- **Cross‑platform playback coordinator** for Android (Media3) and iOS (AVPlayer).
- **Composable building blocks** for building a paged video UI:
  - `VideoFeed`, `VideoFeedSync`, `VideoPagerEffects`, `VideoSurfaceSlot`.
- **Lifecycle integration** via `rememberPlaybackCoordinatorWithLifecycle`.
- **Shutter/thumbnail slot** support (Android uses `ContentFrame` shutter).
- **Keep screen on** effect for demo or app screens.

## Module usage

Add dependency from a shared module:

```kotlin
dependencies {
    implementation(projects.shared.libs.videoPlayback)
}
```

## Basic usage (Compose)

```kotlin
@Composable
fun ReelFeed(
    items: List<VideoItem>,
    pagerState: PagerState,
    eventReporter: PlaybackEventReporter,
) {
    val coordinator = rememberPlaybackCoordinatorWithLifecycle(
        eventReporter = eventReporter,
    )

    VideoFeed(
        items = items,
        pagerState = pagerState,
        coordinator = coordinator,
        key = { it.id },
    ) { item, isActive ->
        VideoSurfaceSlot(
            coordinator = coordinator,
            item = item,
            isActive = isActive,
            shutter = { /* optional thumbnail */ },
        )
    }

    VideoPagerEffects(
        coordinator = coordinator,
        pagerState = pagerState,
        items = items,
        key = { it.id },
    )
}
```

## Playback events

Implement `PlaybackEventReporter` to receive playback progress and end events. This is intentionally lightweight and can be wired to analytics later.

```kotlin
class MyReporter : PlaybackEventReporter {
    override fun playbackProgress(id: String, index: Int, positionMs: Long, durationMs: Long) {
        // analytics hook
    }

    override fun playbackEnded(id: String, index: Int) {
        // analytics hook
    }
}
```

## Android details

- Uses **Media3** and `ContentFrame` for shutter presentation.
- Cache is provided via a shared `SimpleCache` instance. Default cache size is 250 MB.
- Resize mode defaults to **fit**.

## iOS details

- Uses **AVPlayer** with `AVPlayerLayer`.
- Shutter is driven by player state (time/ready) to avoid showing black frames.
- Resize mode defaults to **aspect fit**.

## Keep screen on

Use this in a screen that should prevent display sleep during playback:

```kotlin
KeepScreenOnEffect(enabled = true)
```

## Notes

- This module intentionally does **not** include UI controls.
- Prefetch/pooling are handled internally for the active + next items only.
- The API is designed to be generic and reuseable for non‑shortform experiences.

