# Yral iOS App

A Swift-based iOS application integrating native Swift/SwiftUI with a Kotlin Multiplatform Mobile (KMM) shared module (`iosSharedUmbrella`). The project uses CocoaPods for dependency management and includes custom notification handling, deep links, and foreground notification toasts.

## Contents
- Overview
- Architecture
- Notification Handling
- Build & Run
- Testing
- Dependencies
- Troubleshooting

## Overview
This app blends native iOS code with a shared Kotlin module. Core app navigation is expressed with `AppRoute` types and a routing service provided by dependency injection (`AppDIHelper`). Foreground notifications are surfaced via a bridge from the shared module to native UI.

## Architecture
- Swift/SwiftUI front-end with platform-specific code
- Kotlin Multiplatform Mobile (KMM) shared module: `iosSharedUmbrella`
- Dependency Injection helper: `AppDIHelper`
- Routing: A routing service parsing internal URLs into `AppRoute` values
- CocoaPods integrates the shared framework and resources

### Key Files
- NotificationHandler.swift: Centralizes parsing of push payloads and Branch parameters into `AppRoute` values, and coordinates foreground notification UX.

## Notification Handling
`NotificationHandler` converts incoming push payloads and Branch links into routes and foreground UI.

### Supported Keys
- payload: Stringified JSON payload (optional). If present, it is parsed and used as the source of truth.
- type: String notification type. Known values include:
  - VideoUploadSuccessful
  - VideoUploadedToDraft
  - RewardEarned
- internalUrl: String internal URL (deeplink) to navigate within the app.
- $deeplink_path (Branch): Path for deep linking via Branch.
- +clicked_branch_link (Branch): Indicates whether a Branch link was clicked.

### Behavior Summary
- notificationRoute(from:):
  - Parses `payload` JSON if present, otherwise falls back to flattened `userInfo`.
  - Triggers draft polling when `type == VideoUploadedToDraft`.
  - Returns a route in this order of precedence:
    1. `internalUrl` parsed via the routing service
    2. A type-specific fallback route (see Config)

- foregroundRoute(from:):
  - Returns a route only when the corresponding type config opts into direct navigation while app is in foreground.

- handleForegroundNotification(...):
  - If `foregroundRoute` returns a route, navigates immediately.
  - If `type == VideoUploadedToDraft`, triggers draft polling and shows a toast with a CTA to navigate.
  - Otherwise, shows a standard success toast.

- branchRoute(from:):
  - If `+clicked_branch_link` indicates a Branch click, maps known types or uses `$deeplink_path` to produce a route.

### Type Configuration
`NotificationHandler.Config` allows per-type behavior:
- fallbackInternalUrl: Closure returning a default internal URL when none is present in payload.
- fallbackRoute: Closure returning a default `AppRoute` when no URL is provided.
- navigateDirectlyInForeground: Enables immediate navigation for foreground notifications.

Current mapping:
- VideoUploadSuccessful: Fallback route to `VideoUploadSuccessful(videoID: nil)`
- VideoUploadedToDraft: Fallback internal URL to profile; fallback route to profile path
- RewardEarned: Navigate directly in foreground

## Build & Run
1. Install Ruby and CocoaPods if not already installed.
2. Install pods:
   ```sh
   bundle exec pod install
