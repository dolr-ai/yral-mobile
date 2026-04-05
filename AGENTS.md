# AGENTS.md

## Project Snapshot

YRAL Mobile is a Kotlin Multiplatform app with thin Android and iOS wrappers and most product code in `/shared`. UI is Compose Multiplatform. Blockchain and canister work goes through Rust FFI.

Key directories:
- `/androidApp` - Android wrapper
- `/iosApp` - iOS wrapper
- `/shared` - shared app, features, libs, routing, Rust wrappers
- `/rust-agent` - Rust backend and FFI sources

## Non-Negotiables

- A task is not complete until relevant tests pass and `./gradlew detekt` passes.
- Use `YralLogger` for logging. Do not add `println`.
- Add new dependencies through `libs.versions.toml`, not hardcoded versions.
- Prefer existing architecture and module boundaries over convenience edits.

## Commands

```bash
# Build
./gradlew :androidApp:assembleDebug
./gradlew build

# Tests
./gradlew test                                        # all unit tests (root)
./gradlew :shared:features:<module>:allTests             # per-module (KMP, includes commonTest)

# Code quality
./gradlew ktlintCheck
./gradlew ktlintFormat
./gradlew detekt

# iOS
open iosApp/iosApp.xcworkspace
cd iosApp && pod install
./gradlew :iosSharedUmbrella:assembleXCFramework

# Rust (only when explicitly working on local Rust)
# Set isLocalRust=true in gradle.properties first
cd rust-agent && cargo build
```

## Architecture Rules

### Navigation

- Use Decompose, not Android Navigation.
- Screens are typically `Component` interfaces with `Default...Component` implementations.
- Use `ChildStack` for hierarchical navigation and `ChildSlot` for overlays.
- Routes are serialized and type-safe.

### State

- Shared ViewModels extend `androidx.lifecycle.ViewModel`.
- UI state lives in `StateFlow<...>` with immutable state classes.
- One-off events use `Channel` plus `receiveAsFlow()`.

### Dependency Injection

- DI is Koin-based.
- Each feature owns its own module under `di/`.
- Register feature modules centrally in `/shared/app/di/AppDI.kt`.
- Keep strict DI behavior; do not rely on overrides.

### Feature Layout

Prefer this structure for new shared features:

```text
features/yourfeature/
  di/
  data/
  domain/
  viewmodel/
  nav/
  analytics/
```

### Routing

- Define new routes in `/shared/libs/routing/routes-api/`.
- Mark deeplinkable routes as `ExternallyExposedRoute`.
- Add parsing in `RoutingService`.
- Wire handling into the relevant component, usually from the root flow.

### Rust Integration

- Rust-backed services are exposed through wrappers in `/shared/rust/service/`.
- Default to prebuilt Rust binaries.
- Only switch to local Rust builds when the task actually requires Rust changes.

### CocoaPods and KMM Modules

When a KMM module needs a native iOS pod for Kotlin interop, declare it via `pod("PodName")` in that module's `build.gradle.kts` using `configureCocoapods { }`. Do **not** also add it to `iosApp/Podfile` — that causes a linker error (`symbol multiply defined`) because the framework ends up linked from two paths.

The exception: if a pod is also imported directly in Swift code inside `iosApp/`, it **must** remain in the Podfile. KMM `pod()` only generates Kotlin cinterop bindings and does not add the framework to Xcode's build path for Swift.

A second exception: even if a pod is **not** imported in Swift, it must still be in the Podfile if DYLD needs to resolve it as a dynamic framework at runtime (e.g. `FirebasePerformance`, `FirebaseStorage`). KMM `pod()` declarations do not place the built `.framework` into the simulator products directory used at test time.

## Working Pattern

When adding or changing a feature:

1. Make the smallest change consistent with the existing module structure.
2. Keep business logic in use cases, repositories, and data sources rather than pushing it into UI code.
3. Update DI registration if new implementations are introduced.
4. Add or update tests in the touched module.
5. Run the relevant module tests.
6. Run `./gradlew detekt` before considering the task done.

Test expectations:
- ViewModels: state transitions and event emission
- Use cases: success and error paths
- Repositories: mapping and failure handling
- Components: navigation behavior when relevant

## Environment Notes

- JDK 17+ is required.
- Xcode and CocoaPods are required for iOS work.
- Some dependencies come from GitHub Packages. If resolution fails, check `GITHUB_USERNAME` and `GITHUB_TOKEN`.
- Important Gradle flag: `isLocalRust=false` unless actively developing Rust.

## Repo-Specific Notes

### Session and App Services

- Session state is managed through `SessionManager`.
- For crash reporting, use `CrashlyticsManager.recordException(...)`.
- For analytics, prefer the feature telemetry classes instead of ad hoc logging.

### Tournament Creation

When asked to create tournaments, use the Firebase Cloud Functions rather than writing Firestore documents manually. Those functions handle registration, video fetching, scheduling status changes, and settlement.

Defaults unless specified otherwise:
- Entry cost: 5 YRAL
- Duration: about 10 minutes
- Status flow: scheduled -> live -> ended -> settled

Use staging endpoints by default unless the user explicitly asks for production.
