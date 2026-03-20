# AGENTS.md

This is a **living document**. It reflects the current conventions and confirmed patterns in this repository — not a changelog. Update it whenever you make changes that introduce new overarching patterns, retire old ones, or discover something that future agents should know. Do not add entries for one-off decisions; only add facts that apply broadly. Keep it concise.

## Resuming from a Handoff

If a `HANDOFF.md` file exists at the root of this directory, read it first. It describes the exact state of an active task and the precise next steps. After absorbing its context, delete the file and proceed. If no `HANDOFF.md` exists, no prior handoff is pending.

---

## Project Snapshot

YRAL Mobile is a Kotlin Multiplatform (KMM) video social platform. ~90% of the product code lives in `/shared` and is shared across Android and iOS via Compose Multiplatform UI. Internet Computer Protocol (ICP) blockchain operations are handled by a Rust FFI layer.

**Key directories:**

| Path | Purpose |
|---|---|
| `/androidApp` | Thin Android wrapper (MainActivity, platform DI) |
| `/iosApp` | Thin iOS wrapper (SwiftUI host, CocoaPods) |
| `/shared/app` | Root composition, Koin initialisation, root nav component |
| `/shared/core` | Session, logging, exceptions, app config, utilities |
| `/shared/data` | Cross-feature repositories and data sources |
| `/shared/features` | All product feature modules |
| `/shared/libs` | Reusable library modules (arch, routing, designsystem, …) |
| `/shared/rust` | Kotlin wrappers over Rust FFI services |
| `/rust-agent` | Rust source (blockchain / crypto via UniFFI) |
| `/functions` | Firebase Cloud Functions (Python) |

**All features under `/shared/features`:**
`account`, `aiInfluencer`, `auth`, `chat`, `feed`, `game`, `leaderboard`, `profile`, `reportVideo`, `root`, `subscriptions`, `tournament`, `uploadvideo`, `wallet`

---

## Non-Negotiable Rules

- Every code change must include or update tests. A task is not done until tests pass.
- `./gradlew detekt` must pass before a task is considered complete.
- Use `YralLogger` for all logging. Never use `println`.
- Add dependencies via `gradle/libs.versions.toml`. Never hardcode versions.
- Prefer existing architecture and module boundaries. Do not reach across layers.
- Use typed project accessors (`projects.shared.features.feed`) never string paths.
- Interfaces bind to implementations via Koin `.bind<Interface>()`. Callers depend on interfaces only.
- `allowOverride(false)` in Koin — strict mode is enforced project-wide.

---

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
./gradlew detekt -PdetektAutoCorrect=true   # auto-fix

# iOS
open iosApp/iosApp.xcworkspace
cd iosApp && pod install
./gradlew :iosSharedUmbrella:assembleXCFramework

# Rust (only when isLocalRust=true is set)
cd rust-agent && cargo build
```

---

## Architecture

### Module / Package Conventions

- Feature package root: `com.yral.shared.features.<name>.<layer>`
- Lib package root: `com.yral.shared.libs.<name>`
- Core package root: `com.yral.shared.core`

Each feature module declares convention plugins in its `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.yral.shared.feature)
    alias(libs.plugins.yral.android.feature)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.yral.shared.library.compose)  // only if UI is included
}
```

Inter-module dependencies use typed accessors:

```kotlin
implementation(projects.shared.core)
implementation(projects.shared.libs.arch)
implementation(projects.shared.libs.routing.routesApi)
```

### Feature Layout

Every feature follows this exact structure. Do not deviate:

```
features/yourfeature/
  analytics/     YourFeatureTelemetry.kt
  data/
    models/      DTOs
    IYourDataSource.kt
    YourDataSourceImpl.kt
    YourRepositoryImpl.kt
  di/
    YourModule.kt
  domain/
    models/      Domain models
    repository/  IYourRepository.kt
    useCases/    GetSomethingUseCase.kt, …
  nav/
    YourComponent.kt         (interface + factory companion)
    DefaultYourComponent.kt  (impl, extends ComponentContext)
  ui/
    YourScreen.kt            (+ components/ subpackage)
    YourScreen.android.kt    (platform override if needed)
    YourScreen.ios.kt
  viewmodel/
    YourViewModel.kt
```

Tests go in `src/commonTest/kotlin/com/yral/shared/features/<name>/`.

### Navigation (Decompose)

- Never use Android Navigation.
- Each screen is a `Component` interface + `Default...Component` implementation.
- `ChildStack` for hierarchical navigation, `ChildSlot` for overlays.
- Configs are `@Serializable` for type-safe state restoration.

```kotlin
interface FeedComponent {
    val state: StateFlow<FeedState>
}

class DefaultFeedComponent(
    componentContext: ComponentContext,
    // injected deps
) : FeedComponent, ComponentContext by componentContext
```

### State Management

- ViewModels that work with Koin's `viewModelOf` extend `androidx.lifecycle.ViewModel`.
- ViewModels bound to Decompose `InstanceKeeper` extend the arch base `ViewModel<E>`.
- State in `MutableStateFlow`, exposed as `StateFlow`.
- One-off events via `Channel<Event>` and `.receiveAsFlow()`.
- State updates through `.update { it.copy(…) }`.

```kotlin
class YourViewModel(…) : ViewModel() {
    private val _state = MutableStateFlow(YourState())
    val state: StateFlow<YourState> = _state.asStateFlow()

    private val eventsChannel = Channel<YourEvent>()
    val events = eventsChannel.receiveAsFlow()
}
```

### Dependency Injection (Koin)

Feature module declares a top-level `val`:

```kotlin
val yourModule = module {
    factoryOf(::YourDataSourceImpl).bind<IYourDataSource>()
    factoryOf(::YourRepositoryImpl).bind<IYourRepository>()
    factoryOf(::YourTelemetry)
    factoryOf(::YourUseCase)
    viewModelOf(::YourViewModel)
}
```

Register in `/shared/app/di/AppDI.kt` inside `initKoin { modules(…, yourModule) }`.

Platform-specific additions: `AppDI.android.kt` / `AppDI.ios.kt` via `expect`/`actual`.

### Use Cases (Arch Base Classes)

| Class | When to use |
|---|---|
| `SuspendUseCase<P, R>` | Single suspend operation with params |
| `UnitSuspendUseCase<R>` | No-param variant |
| `ResultSuspendUseCase<P, R, E>` | Returns typed `Result<R, E>` (kotlin-result) |
| `FlowUseCase` | Ongoing stream |

Use cases return `Result<T, E>` from the `kotlin-result` library. Errors bubble through `UseCaseFailureListener`.

### Routing & Deeplinks

Routes are `@Serializable` implementations of `AppRoute` in `shared/libs/routing/routes-api/`:

```kotlin
// No-param route
@Serializable
object WalletRoute : AppRoute, ExternallyExposedRoute {
    const val PATH = "/wallet"
}

// Parameterised route
@Serializable
data class PostDetailsRoute(
    val canisterId: String,
    val postId: String,
    val publisherUserId: String,
) : AppRoute, ExternallyExposedRoute

// Optional params
@Serializable
data class UserProfileRoute(
    val canisterId: String,
    val userPrincipalId: String,
    val profilePic: String? = null,
) : AppRoute, ExternallyExposedRoute
```

- Mark deeplink-accessible routes with `ExternallyExposedRoute`.
- Add URL parsing in `RoutingService`.
- Handle navigation in `DefaultRootComponent`.
- `PendingAppRouteStore` queues routes that arrive before login.

### Rust / FFI Integration

- All Rust-backed services expose Kotlin wrappers in `/shared/rust/service/`.
- FFI bindings are generated via UniFFI from `/rust-agent/rust-agent-uniffi/`.
- Available service factories: `IndividualUserServiceFactory`, `UserPostServiceFactory`, `SnsLedgerServiceFactory`, `ICPLedgerServiceFactory`.
- Default to pre-built Rust binaries (`isLocalRust=false`). Only set `isLocalRust=true` when actively changing Rust code.

---

## Testing

### Requirements

All code changes must have tests. A task is only done when:
1. Tests for new/modified code are written.
2. `./gradlew :shared:features:<module>:allTests` (or `./gradlew test` at root) passes.
3. `./gradlew detekt` passes.

### Patterns

- Tests in `src/commonTest/kotlin/…`.
- Use `kotlin.test` for multiplatform assertions.
- Use `kotlinx.coroutines.test` for StateFlow and Channel.
- Use `turbine` for Flow assertions.
- Use `mockk` for mocking.
- Use `truth` for fluent assertions.
- Use `robolectric` for Android-specific unit tests.

```kotlin
class YourViewModelTest {
    @Test
    fun `loading state is set while fetching`() = runTest {
        // arrange
        // act
        // assert via awaitItem() with turbine or assertEquals
    }
}
```

### Coverage Expectations

| Layer | What to test |
|---|---|
| ViewModel | State transitions and event emissions |
| Use Cases | Success and error paths |
| Repositories | Data mapping and failure handling |
| Components | Navigation behaviour when relevant |
| Analytics | Event name and payload accuracy |

---

## Code Quality

- **ktlint**: enforced via pre-commit hook and CI. Run `./gradlew ktlintFormat` to auto-fix.
- **detekt**: custom config at `detekt-config.yml`. Key overrides:
  - `LongParameterList`: 15 (functions) / 10 (constructors)
  - `TooManyFunctions`: raised to 20
  - `FunctionNaming`: Composable functions exempted
  - `MagicNumber`: use named constants
- **SwiftLint**: strict mode for iOS Swift (enforced in pre-commit).

---

## Cross-Cutting Concerns

### Logging

Always `YralLogger`. Never `println`. Example:

```kotlin
YralLogger.d("FeedViewModel") { "Feed loaded: ${posts.size} posts" }
```

### Crash Reporting

```kotlin
CrashlyticsManager.recordException(exception, ExceptionType.NETWORK)
```

### Analytics

Each feature has a `*Telemetry` class (e.g. `FeedTelemetry`, `WalletTelemetry`). Use the feature-scoped telemetry class. Do not log events ad hoc.

### Session

`SessionManager` is the single source of truth for auth state and user properties (`userPrincipal`, `canisterID`, `identity`, `coinBalance`, …). Observe reactively via `observeSessionProperty { }`.

---

## Environment

- **JDK**: 17+ required (enforced at build time)
- **Kotlin**: managed by Gradle version catalog
- **Android Studio**: latest stable
- **Xcode + CocoaPods**: required for iOS
- **GitHub Packages**: set `GITHUB_USERNAME` and `GITHUB_TOKEN` env vars if dependency resolution fails
- **Gradle flags** (`gradle.properties`):
  - `isLocalRust=false` — keep false unless working on Rust
  - `org.gradle.jvmargs=-Xmx8192M`
  - `org.gradle.caching=true`
  - `org.gradle.configuration-cache=true`

---

## Tournament Creation (Cloud Functions)

When creating tournaments, always call the Firebase Cloud Functions. Do not write to Firestore manually.

**Defaults** (unless stated otherwise): entry cost = 5 YRAL, duration ≈ 10 min, status flow: `scheduled → live → ended → settled`.

Use **staging** by default unless the user explicitly requests production.

```bash
ACCESS_TOKEN=$(gcloud auth print-access-token)

# Smiley tournament (staging)
curl -X POST "https://us-central1-yral-staging.cloudfunctions.net/create_tournaments" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"…","entry_cost":5,"total_prize_pool":5,"prize_map":{"1":3,"2":2},"start_time":"HH:MM","end_time":"HH:MM"}'

# Hot or Not tournament (staging)
curl -X POST "https://us-central1-yral-staging.cloudfunctions.net/create_hot_or_not_tournament" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"…","entry_cost":5,"total_prize_pool":5,"prize_map":{"1":3,"2":2},"start_time":"HH:MM","end_time":"HH:MM","video_count":10}'
```

Replace `yral-staging` with `yral-mobile` for production.

Cloud Tasks on the `tournament-status-updates` queue (us-central1) drive automatic status transitions.

---

## Updating This File

Update `.github/copilot-instructions.md` when:
- A new architectural pattern is introduced or an old one retired.
- A new cross-cutting library or service is adopted (logging, analytics, crash reporting, etc.).
- Module structure conventions change.
- A new required command or environment prerequisite is confirmed.
- A previously undocumented but consistently followed pattern is discovered.

Do **not** add one-off decisions, task-specific notes, or changelog entries. This file describes the current state of conventions, not history.
