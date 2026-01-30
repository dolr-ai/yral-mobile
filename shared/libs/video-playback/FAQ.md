# video-playback — FAQ

## Why only current + next players?
It keeps memory and CPU predictable while still enabling smooth swipes. Most feeds only need the active item and the next item prepared.

## Why no built‑in controls?
This module focuses on feed playback. Controls are intentionally left to the app layer to avoid opinionated UI and keep the library reusable.

## How do I show a thumbnail before playback?
Use the `shutter` slot in `VideoSurfaceSlot`. Android’s `ContentFrame` automatically decides when to show it. On iOS, shutter hides once the player is ready and starts progressing.

## How do I add analytics?
Implement `PlaybackEventReporter` and pass it to `rememberPlaybackCoordinatorWithLifecycle`. You’ll receive progress and end events.

## Can I use this outside a vertical pager?
Yes. The UI layer is decoupled—use `VideoFeed` with a different pager or swipe deck, or compose your own UI and only use the coordinator + surface.

## How is caching handled?
Android uses a shared Media3 `SimpleCache`. The default size is 250 MB. iOS does not use a custom cache by default.

## Why is the resize mode “fit”?
It avoids cropping in full‑screen reels and matches expected aspect‑fit behavior. You can customize it in the platform surface if needed.

## Is instrumentation required?
Not required, but easy to add later. The event reporter hook is intentionally small and can be extended without breaking the UI API.

