# video-playback — Architecture

## Overview

This module provides a small, reusable playback layer for Compose Multiplatform. It separates **UI paging**, **playback coordination**, and **platform player** so it can be reused across different swipe UIs.

```
UI (Pager / Swipe)
      │
      ▼
VideoFeed / VideoPagerEffects
      │
      ▼
PlaybackCoordinator (common API)
      │
      ├── Android: Media3 ExoPlayer + ContentFrame
      └── iOS: AVPlayer + AVPlayerLayer
```

## Design principles (summary)

- **UI is dumb; playback is coordinated**: UI owns surfaces, coordinator owns players.
- **Small player pool**: only active + next are kept warm (no player‑per‑page).
- **Sliding window preload**: adjacent item prepared; disk cache used on Android.
- **Format‑agnostic descriptor**: container hints allow MP4 today, HLS/DASH later.
- **Tunable policy**: cache size and preload tick cadence are configurable.
- **Observability**: playback progress/end events exposed via `PlaybackEventReporter`.

## Core concepts

- **Coordinator**: Owns players, binds them to items, and exposes a stable API to the UI.
- **Active/Next slots**: Only the current and next items are kept warm to keep memory/CPU predictable.
- **Shutter**: Thumbnail placeholder shown until a player is ready/rendering. Android uses `ContentFrame` shutter; iOS uses player state to hide the shutter.

## Data flow (simplified)

1. `VideoFeed` renders a pager with content.
2. `VideoPagerEffects` observes pager state and tells the coordinator which item is active/next.
3. The coordinator binds a player to the active item and prepares the next item.
4. `VideoSurfaceSlot` attaches the platform surface (Android View / iOS UIView) to the bound player.
5. Shutter hides when player renders the first frame (Android) or when iOS reaches a ready/playing state.

## Sequence diagram (page change)

```
User               Pager/UI              Coordinator              Platform Player
 |                   |                        |                           |
 | swipe to next     |                        |                           |
 |------------------>|                        |                           |
 |                   | onPageChanged          |                           |
 |                   |----------------------->|                           |
 |                   |                        | bind active + prepare next|
 |                   |                        |-------------------------->|
 |                   |                        |                           | prepare()
 |                   |                        |                           |
 |                   | render VideoSurface    |                           |
 |                   |----------------------->| get surface binding       |
 |                   |                        |-------------------------->|
 |                   | shutter shown          |                           |
 |                   |                        |                           | start playback
 |                   |                        |                           | render first frame
 |                   | shutter hides          |                           |
 |                   |<-----------------------|                           |
```

## Internal mechanics (concise)

### Slot lifecycle
- **Active slot**: bound player for the visible item.
- **Prepared slot**: exactly one next item preloaded and ready to attach.
- Non‑active, non‑prepared items are released immediately.

### Preload decisions
- Only the next item is prepared.
- Android uses Media3 cache; iOS relies on prepared items by default.
- Prefetch concurrency is intentionally small to avoid playback contention.

### Shutter behavior
- Android: `ContentFrame` decides when to show/hide shutter based on `PresentationState`.
- iOS: shutter hides when the player is ready/playing and time advances past a tiny threshold.

### Lifecycle handling
- Foreground: resume playback and keep surfaces attached.
- Background: pause playback and release surfaces.
- Dispose: release players and clear slots.

### Event reporting
- `PlaybackEventReporter` emits progress ticks and playback ended events.

## Lifecycle

- `rememberPlaybackCoordinatorWithLifecycle` ties player creation/release to lifecycle events.
- On background/foreground, playback is paused/resumed and surfaces are released or reattached.

## Platform notes

### Android
- Media3 ExoPlayer is used.
- `ContentFrame` handles shutter presentation based on `PresentationState`.
- Cache is a shared `SimpleCache` (default 250 MB).

### iOS
- AVPlayer and AVPlayerLayer are used.
- Shutter state is driven by `timeControlStatus` and current time.
- Audio session is set to allow playback with device volume controls.

## Extensibility

- Replace pager with a swipe deck or horizontal pager by reusing `VideoFeed` and `VideoPagerEffects`.
- Add analytics by implementing `PlaybackEventReporter`.
- Custom shutter visuals can be supplied from UI.
