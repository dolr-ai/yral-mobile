# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

YRAL Mobile is a Kotlin Multiplatform (KMM) video social platform with native iOS and Android apps. The codebase is ~90% shared code using Compose Multiplatform for UI, with unique Rust FFI integration for blockchain operations (Internet Computer Protocol).

## Common Commands

### Building

```bash
# Build Android app
./gradlew :androidApp:assembleDebug

# Build Android release
./gradlew :androidApp:assembleRelease

# Build all modules
./gradlew build

# Clean build
./gradlew clean
```

### Testing

```bash
# Run all tests across all modules
./gradlew allTests

# Run tests for specific module
./gradlew :shared:features:feed:allTests
./gradlew :shared:core:allTests

# Run Android instrumented tests
./gradlew :androidApp:connectedAndroidTest
```

### Code Quality

```bash
# Run ktlint check
./gradlew ktlintCheck

# Auto-format with ktlint
./gradlew ktlintFormat

# Run detekt static analysis
./gradlew detekt

# Run detekt with auto-fix
./gradlew detekt -PdetektAutoCorrect=true

# Run all verification tasks
./gradlew check
```

### iOS Development

```bash
# Open iOS project in Xcode
open iosApp/iosApp.xcworkspace

# Install iOS dependencies (CocoaPods)
cd iosApp && pod install

# Build iOS framework
./gradlew :iosSharedUmbrella:assembleXCFramework
```

### Rust Integration

The project uses Rust for blockchain/canister operations. By default, it uses pre-built Rust binaries. To work with local Rust:

```bash
# Enable local Rust builds
# Set in gradle.properties: isLocalRust=true

# Build Rust agent
cd rust-agent && cargo build
```

### Pre-commit Hooks

Pre-commit hooks run ktlint, detekt, and SwiftLint. Install with:

```bash
pre-commit install
```

## Architecture Overview

### Module Structure

```
/androidApp/        # Android app wrapper (thin layer)
/iosApp/            # iOS app wrapper (SwiftUI + Compose)
/shared/            # Shared KMM code (~90% of app)
  /app/             # Main app composition, DI setup
  /core/            # Session, logging, exceptions, config
  /data/            # Repositories, data sources, models
  /features/        # Feature modules
    /auth/          # Social login (Google, Apple)
    /feed/          # Video feed with pagination, voting
    /game/          # Smiley game
    /wallet/        # Token balance, transactions
    /profile/       # User profiles
    /uploadvideo/   # Video upload, AI generation
    /leaderboard/   # Rankings, tournaments
    /account/       # Account management
    /root/          # Root navigation coordinator
    /reportVideo/   # Video reporting
  /libs/            # Reusable libraries
    /arch/          # Base ViewModel, UseCase classes
    /koin/          # DI configuration
    /routing/       # Custom deeplink engine
    /http/          # Network layer
    /preferences/   # Settings storage
    /designsystem/  # Shared UI components
    /videoPlayer/   # Video playback
    /analytics/     # Analytics wrapper
    /crashlytics/   # Crash reporting
    /firebaseAuth/  # Firebase Auth integration
    /firebaseStore/ # Firestore integration
  /rust/            # Rust FFI wrappers
    /service/       # Kotlin wrappers for Rust services
/rust-agent/        # Rust backend (blockchain/crypto)
```

### Key Architectural Patterns

**Navigation: Decompose (not Android Navigation)**
- Component-based architecture (not Fragment/Activity)
- Each screen is a Component interface + DefaultComponent implementation
- `ChildStack` for hierarchical navigation, `ChildSlot` for overlays
- Serializable configs for type-safe navigation and state restoration

Example:
```kotlin
interface FeedComponent {
    val state: StateFlow<FeedState>
}

class DefaultFeedComponent(
    componentContext: ComponentContext,
    // injected dependencies
) : FeedComponent, ComponentContext by componentContext
```

**State Management: MVI-inspired**
- ViewModels extend `androidx.lifecycle.ViewModel` (shared across platforms)
- State in `StateFlow<State>` (immutable data classes)
- One-time events via `Channel<Event>.receiveAsFlow()`
- Updates via `.update { }` on MutableStateFlow

**Dependency Injection: Koin**
- Each feature has its own DI module in `di/` folder
- Platform-specific modules use `expect`/`actual`
- Central initialization in `/shared/app/di/AppDI.kt`
- Strict mode: no overrides allowed

**Feature Organization (Clean Architecture)**
```
features/yourfeature/
├── di/              # Koin module
├── data/            # Repository impl, data sources
├── domain/          # Repository interface, use cases
├── viewmodel/       # ViewModel with state
├── nav/             # Component interface + impl
└── analytics/       # Feature-specific telemetry
```

**Use Cases**
- Base classes: `SuspendUseCase<Params, Result>`, `FlowUseCase`
- Return `Result<T, E>` from kotlin-result library
- Auto-logging via `UseCaseFailureListener`

**Session Management**
- `SessionManager` singleton manages user session
- Reactive properties: `observeSessionProperty { }`
- Properties: `userPrincipal`, `canisterID`, `identity`, `coinBalance`, etc.

### Routing & Deeplinks

Custom type-safe routing system in `/shared/libs/routing/`:

**Route Definition**
```kotlin
@Serializable
data class PostDetailsRoute(
    val canisterId: String,
    val postId: String,
    val publisherUserId: String
) : AppRoute, ExternallyExposedRoute {
    companion object {
        const val PATH = "post/details/{canisterId}/{postId}/{publisherUserId}"
    }
}
```

**Navigation Flow**
1. Deep link → `RoutingService.parseUrl()` → `AppRoute`
2. `AppRoute` → `RootComponent.onNavigationRequest()`
3. Decompose handles actual navigation
4. `PendingAppRouteStore` queues routes before login

### Rust Integration (Unique to This Project)

**Why Rust?**
- Blockchain canister interactions (Internet Computer Protocol)
- Performance-critical crypto operations
- Shared identity/security logic

**FFI via UniFFI**
- Rust code in `/rust-agent/rust-agent-uniffi/`
- UniFFI generates Kotlin bindings
- Service factories in `/shared/rust/service/`:
  - `IndividualUserServiceFactory`
  - `UserPostServiceFactory`
  - `SnsLedgerServiceFactory` (blockchain ledger)
  - `ICPLedgerServiceFactory` (ICP blockchain)

**Usage Pattern**
```kotlin
class IndividualUserDataSourceImpl(
    private val serviceFactory: IndividualUserServiceFactory
) : IndividualUserDataSource {
    // Calls Rust FFI
}
```

### Platform Integration

**Android**
- `MainActivity` sets up Decompose + Compose
- Platform services: install referrer, in-app updates, Firebase

**iOS**
- SwiftUI wrapper around shared Compose UI
- `RootViewController.kt` provides Compose integration
- `ContentView.swift` embeds shared UI
- CocoaPods for dependencies

## Development Workflow

### Adding a New Feature

1. Create module: `/shared/features/yourfeature/`
2. Structure: `data/`, `domain/`, `viewmodel/`, `nav/`, `di/`
3. Create `YourFeatureModule.kt` in `di/`
4. Register module in `/shared/app/di/AppDI.kt`
5. Define routes in `/shared/libs/routing/routes-api/`
6. Create Component interface + Default implementation
7. Wire up navigation in `RootComponent`

### Working with Navigation

1. Define route in `/shared/libs/routing/routes-api/`
2. Mark as `ExternallyExposedRoute` if deeplink-accessible
3. Add route parsing logic to `RoutingService`
4. Handle in appropriate Component's navigation logic

### Debugging Tips

- **Session state**: Check `SessionManager` for auth state
- **Logging**: Use `YralLogger` (don't use println)
- **Crashes**: `CrashlyticsManager.recordException(exception, ExceptionType.XXX)`
- **Analytics**: Feature-specific `Telemetry` classes (e.g., `FeedTelemetry`)
- **Network**: Check `/shared/libs/http/` for API client setup

### Code Style

The project enforces code quality via pre-commit hooks:

- **ktlint**: Kotlin code formatting (runs automatically)
- **detekt**: Static analysis (custom config in `detekt-config.yml`)
- **SwiftLint**: iOS Swift code formatting (strict mode)

Key detekt customizations:
- LongParameterList: 15 params for functions, 10 for constructors
- TooManyFunctions: 20 threshold
- FunctionNaming: Composables exempt from naming rules

### Common Patterns

**ViewModel**
```kotlin
class YourViewModel(...) : ViewModel() {
    private val _state = MutableStateFlow(YourState())
    val state: StateFlow<YourState> = _state.asStateFlow()

    private val eventsChannel = Channel<YourEvent>()
    val events = eventsChannel.receiveAsFlow()

    fun doSomething() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            // ...
        }
    }
}
```

**Use Case**
```kotlin
class YourUseCase(
    private val repository: IYourRepository
) : SuspendUseCase<Params, Result>() {
    override suspend fun execute(parameter: Params): Result {
        return repository.getData()
    }
}
```

**Repository**
```kotlin
interface IYourRepository {
    suspend fun getData(): Result<Data, Error>
}

class YourRepository(
    private val dataSource: YourDataSource
) : IYourRepository {
    override suspend fun getData(): Result<Data, Error> {
        // Implementation
    }
}
```

**Koin Module**
```kotlin
val yourModule = module {
    // ViewModels
    viewModelOf(::YourViewModel)

    // Use Cases
    factoryOf(::YourUseCase)

    // Repositories
    singleOf(::YourRepository) { bind<IYourRepository>() }

    // Data Sources
    factoryOf(::YourDataSource)
}
```

## Environment Setup

### Requirements

- **JDK 17+** (verified at build time)
- **Kotlin**: Managed by Gradle
- **Android Studio**: Latest stable
- **Xcode**: Latest stable (for iOS development)
- **Rust** (optional): Only if `isLocalRust=true`
- **CocoaPods**: For iOS dependencies

### GitHub Packages Authentication

The project uses GitHub Packages for some dependencies. Set environment variables:

```bash
export GITHUB_USERNAME=your-username
export GITHUB_TOKEN=your-personal-access-token
```

### Firebase Setup

Firebase is configured for multiple flavors (debug, staging, prod). Configuration files are in `/config/`.

## Build Configuration

### Gradle Properties

Important flags in `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx8192M  # High memory for KMM builds
org.gradle.caching=true        # Build cache enabled
org.gradle.configuration-cache=true  # Configuration cache
isLocalRust=false             # Use local Rust vs. pre-built
```

### Version Catalog

All dependencies managed via `libs.versions.toml` (Gradle Version Catalog). When adding dependencies, use the catalog instead of hardcoding versions.

### Multiplatform Targets

- `androidTarget()` - Android
- `iosArm64()` - iOS devices
- `iosSimulatorArm64()` - iOS simulators (Apple Silicon)

## Troubleshooting

### Detekt Failures

If detekt fails with false positives, consider:
1. Check `detekt-config.yml` for rule configuration
2. Add `@Suppress("RuleName")` for valid exceptions
3. Run with `-PdetektAutoCorrect=true` for auto-fixes

### ktlint Format Issues

```bash
# Auto-fix most issues
./gradlew ktlintFormat

# Check specific files
./gradlew ktlintCheck -PktlintFiles="path/to/file.kt"
```

### Rust Build Issues

If Rust FFI issues occur:
1. Ensure `isLocalRust=false` unless actively developing Rust code
2. Pre-built binaries are in GitHub Packages
3. Check Rust agent is on correct version

### iOS Build Issues

```bash
# Clean CocoaPods
cd iosApp && pod deintegrate && pod install

# Clean derived data
rm -rf ~/Library/Developer/Xcode/DerivedData

# Rebuild framework
./gradlew :iosSharedUmbrella:clean :iosSharedUmbrella:assembleXCFramework
```

### Configuration Cache Issues

If Gradle configuration cache causes issues:

```bash
# Disable temporarily
./gradlew build --no-configuration-cache
```
