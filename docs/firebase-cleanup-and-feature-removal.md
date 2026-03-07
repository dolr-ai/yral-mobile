# Firebase Cleanup & Feature Removal

**Branch:** `964-firebase-cleanup`
**Date:** 2026-03-07

---

## Overview

This change removes the Game, Leaderboard, and Tournament features from the app and cleans up the Firebase infrastructure that was introduced exclusively to support them. The work was structured as 6 sequential commits to keep changes reviewable and bisectable.

---

## Commits

| Commit | Description |
|--------|-------------|
| `39ec1424a` | feat: remove tournament feature |
| `3db2c7ff7` | feat: remove leaderboard feature |
| `1662052c0` | feat: remove game feature |
| `e7219406e` | feat: remove Firestore document writes from auth |
| `f12b91de0` | refactor: remove unused session param from signInWithToken |
| `f9122d577` | feat: remove Firebase tracking from session and surface APIs |

---

## What Was Removed

### 1. Tournament Feature (`shared/features/tournament/`)

The entire tournament module was deleted, including:

- All UI screens: `TournamentScreen`, `TournamentGameScreen`, `TournamentLeaderboardScreen`, `TournamentWinnerScreen`, `TournamentFailScreen`, `TournamentHowToPlayScreen`, `TournamentIntroBottomSheet`, `TournamentCountdownBottomSheet`, `TournamentCtaButton`, `DailyTournamentCard`, `DailyTournamentResultScreen`, `LeaveTournamentBottomSheet`, `OutOfDiamondsBottomSheet`, `PrizeBreakdownBottomSheet`, `CircularTimerProgress`
- All ViewModels: `TournamentViewModel`, `TournamentGameViewModel`, `TournamentLeaderboardViewModel`
- All navigation components: `TournamentComponent`, `TournamentGameComponent`
- All domain models, use cases, repository, and data source
- Analytics: `TournamentTelemetry`
- All assets: banners, icons, fonts (`dela_gothic_one_regular.ttf`), sound files
- `TournamentResumeCacheStore` (Firestore-backed resume cache)
- `TournamentModule` (Koin DI)
- `AppRoute.Tournaments` deep link route
- `TournamentLeaderboard` and `TournamentGame` nav configs
- Navigation wiring in `RootComponent`, `DefaultRootComponent`, `ComponentFactory`, `HomeComponent`, `DefaultHomeComponent`
- `TournamentGameScaffoldScreen` from the app module

### 2. Leaderboard Feature (`shared/features/leaderboard/` + `shared/libs/leaderboard/`)

Both the feature module and shared UI library were deleted, including:

- All UI screens: `LeaderboardScreen`, `LeaderboardMainScreen`, `LeaderboardHistoryScreen`
- All ViewModels: `LeaderBoardViewModel`, `LeaderboardHistoryViewModel`
- All navigation components: `LeaderboardComponent`, `LeaderboardMainComponent`, `LeaderboardDetailsComponent`
- All domain models, use cases, repository, and data source
- Analytics: `LeaderBoardTelemetry`
- Shared UI library: `LeaderBoardUIComponents`, `TrophyGallery`, `LeaderboardConfetti`, `LeaderboardModes`
- `AppRoute.Leaderboard` deep link route
- `Leaderboard` nav config
- Navigation wiring in `RootComponent`, `DefaultRootComponent`, `ComponentFactory`, `HomeComponent`, `DefaultHomeComponent`

### 3. Game Feature (`shared/features/game/`)

The entire game module was deleted, including:

- All UI: `SmileyGame`, `GameToggle`, `GameIcon`, `GameIconStrip`, `GameResultSheet`, `HotOrNotOnboardingOverlay`, `HotOrNotResultOverlay`, `HowToPlay`, `CoinBalance`, `CoinDeltaAnimation`, `EmojiBubblesAnimation`, `AboutGameSheet`, `RefreshBalanceAnimation`
- `GameViewModel` (637 lines)
- All domain models, use cases, repository, and data source
- Analytics: `GameTelemetry`
- All assets: emoji PNGs, coin audio files, icon drawables
- `GameModule` (Koin DI)

**Migration:** `GetBalanceUseCase` was moved from the game module to `shared/features/wallet/domain/GetCoinBalanceUseCase` so `DefaultAuthClient` can continue fetching the YRAL coin balance after login without depending on the game module.

### 4. Firestore Document Writes from Auth

`DefaultAuthClient` previously wrote user data (username, canister ID, coin balance) to Firestore after each login via `postFirebaseLogin()` and `updateBalanceAndProceed()`. These writes served tournament/leaderboard features and are no longer needed. Removed:

- `UpdateDocumentUseCase` injection and usage from `DefaultAuthClient`
- `postFirebaseLogin()` method
- `updateBalanceAndProceed()` method
- `firebaseStore` dependency from `shared/features/auth/build.gradle.kts` (then re-added — see note below)

The Firebase anonymous sign-in + token exchange flow is **kept** because it is still used by `GetBtcConversionUseCase` (wallet), which calls a Firebase Cloud Function that requires an authenticated ID token. This flow is now internalized as a private `doFirebaseAuth()` called via `scope.launch` inside `handleExtractIdentityResponse`, removing it from the public `AuthClient` interface.

> **Note:** `shared/features/auth/build.gradle.kts` retains the `firebaseStore` dependency because `AuthDataSourceImpl.exchangePrincipalId()` uses `cloudFunctionUrl()` and `firebaseAppCheckToken()` from that module.

### 5. Session Properties Cleanup

Removed from `SessionProperties` (in `shared/core/session/Session.kt`):

| Property | Purpose |
|----------|---------|
| `isForcedGamePlayUser` | Hot-or-Not onboarding state |
| `isFirebaseLoggedIn` | Gated BTC conversion fetch on Firebase auth readiness |
| `dailyRank` | Tournament daily rank |
| `honDailyRank` | Hot-or-Not tournament daily rank |
| `pendingTournamentRegistrationId` | Queued tournament registration |

Removed `SessionManager` methods: `updateIsForcedGamePlayUser()`, `updateFirebaseLoginState()`, `updateDailyRank()`, `updateHonDailyRank()`, `setPendingTournamentRegistrationId()`, `consumePendingTournamentRegistrationId()`, `isFirebaseLoggedIn()`.

### 6. Root ViewModel Cleanup

Removed from `RootViewModel`:

- `initializeFirebase()` — previously triggered Firebase auth manually on app start for already-signed-in users
- `firebaseJob: Job?` — tracked the Firebase auth coroutine
- `sessionManager.updateIsForcedGamePlayUser(...)` call
- `YralFBAuthException` import

Account-switch logic simplified: bot accounts no longer need special Firebase handling; non-bot accounts just call `authClient.initialize()`.

### 7. `AuthClient` Interface Cleanup

Removed from the public `AuthClient` interface:

- `suspend fun authorizeFirebase(session: Session)` — Firebase auth is now an internal implementation detail of `DefaultAuthClient`
- `suspend fun fetchBalance(session: Session)` — balance fetch is now private and triggered automatically post-login

Removed `YralFBAuthException` class entirely (was only used inside `DefaultAuthClientFactory`'s `CoroutineExceptionHandler`).

### 8. Wallet Screen / ViewModel Cleanup

- `WalletViewModel`: removed `isFirebaseLoggedIn` observer; BTC conversion is now always fetched on `refresh()` instead of being gated on Firebase auth state
- `WalletScreen`: merged two `LaunchedEffect` blocks into one; removed `isFirebaseLoggedIn` as a key

---

## What Was NOT Removed

The following Firebase infrastructure remains because it is still actively used by other features:

| Module | Used By | Reason Kept |
|--------|---------|-------------|
| `shared/libs/firebaseAuth` | `DefaultAuthClient` | Anonymous sign-in + token exchange for BTC conversion Cloud Function auth |
| `shared/libs/firebaseStore` | `AuthDataSourceImpl`, `WalletDataSourceImpl`, `FeedRepositoryImpl`, `AccountsViewModel` | `cloudFunctionUrl()`, `firebaseAppCheckToken()`, Firestore vote checking, Firebase Storage downloads |

---

## Key Files Changed

| File | Change |
|------|--------|
| `settings.gradle.kts` | Removed 4 module includes: game, leaderboard, tournament, libs/leaderboard |
| `shared/app/build.gradle.kts` | Removed game, leaderboard, tournament dependencies |
| `androidApp/build.gradle.kts` | Removed game dependency |
| `shared/app/di/AppDI.kt` | Removed `gameModule`, `leaderboardModule`, `tournamentModule` |
| `shared/app/nav/Config.kt` | Removed `TournamentLeaderboard`, `TournamentGame`, `Leaderboard` configs |
| `shared/app/nav/RootComponent.kt` | Removed tournament/leaderboard nav methods and Child classes |
| `shared/app/nav/DefaultRootComponent.kt` | Removed tournament/leaderboard navigation implementations |
| `shared/app/nav/factories/ComponentFactory.kt` | Removed tournament/leaderboard factory methods |
| `shared/app/ui/screens/home/HomeScreen.kt` | Removed tournament/leaderboard/game UI |
| `shared/app/ui/screens/home/nav/HomeComponent.kt` | Removed tournament/leaderboard tabs and Child types |
| `shared/libs/routing/routes/api/AppRoute.kt` | Removed `Leaderboard`, `Tournaments` routes |
| `shared/features/auth/DefaultAuthClient.kt` | Removed Firestore writes; internalized Firebase auth as private `doFirebaseAuth()` |
| `shared/features/auth/DefaultAuthClientFactory.kt` | Removed `FirebaseAuth` constructor param and `YralFBAuthException` handler |
| `shared/features/auth/AuthClient.kt` | Removed `authorizeFirebase()` and `fetchBalance()` from public interface |
| `shared/features/auth/YralAuthException.kt` | Removed `YralFBAuthException` class |
| `shared/core/session/Session.kt` | Removed 5 game/tournament-specific `SessionProperties` fields |
| `shared/core/session/SessionManager.kt` | Removed 7 game/tournament-specific update methods |
| `shared/features/root/viewmodels/RootViewModel.kt` | Removed `initializeFirebase()`, `firebaseJob`, Firebase session updates |
| `shared/features/wallet/viewmodel/WalletViewModel.kt` | Removed `isFirebaseLoggedIn` gating |
| `shared/features/wallet/ui/WalletScreen.kt` | Removed `isFirebaseLoggedIn` key from `LaunchedEffect` |
| `shared/features/wallet/domain/GetCoinBalanceUseCase.kt` | **New** — migrated from deleted game module |

### 9. Tournament Feed Context Removed from FeedViewModel

Removed residual tournament feed support that remained in `FeedViewModel` after the tournament feature module was deleted:

**Deleted files:**
- `shared/features/feed/viewmodel/FeedContext.kt` — removed `FeedContext.Tournament` subclass; `FeedContext` sealed class retained with only `Default` for future extensibility
- `shared/features/feed/domain/useCases/GetTournamentFeedUseCase.kt`
- `shared/features/feed/data/models/TournamentPostResponseDTO.kt`

**Updated files:**

| File | Change |
|------|--------|
| `shared/features/feed/viewmodel/FeedViewModel.kt` | Removed `feedContext: FeedContext` constructor param; removed `initialTournamentFeedData()`, `stableTournamentOrderKey()`; removed tournament guards in `initializeFeed()`, `loadMoreFeed()`, `onCurrentPageChange()`; removed `checkAndShowTournamentIntroSheet()`, `dismissTournamentIntroSheet()`; removed `showTournamentIntroSheet`/`tournamentIntroCheckedThisSession` from `FeedState`; removed `TOURNAMENT_INTRO_PAGE` constant |
| `shared/features/feed/di/FeedModule.kt` | Removed `GetTournamentFeedUseCase` registration; `viewModel` block passes `FeedContext` parameter (retained for future extensibility) |
| `shared/features/feed/domain/IFeedRepository.kt` | Removed `getTournamentFeeds()` |
| `shared/features/feed/data/IFeedDataSource.kt` | Removed `getTournamentFeeds()` |
| `shared/features/feed/data/FeedRepository.kt` | Removed `getTournamentFeeds()` implementation |
| `shared/features/feed/data/FeedRemoteDataSource.kt` | Removed `getTournamentFeeds()` implementation and `TOURNAMENT_FEED_PATH`/`TOURNAMENT_FEED_VIDEOS_PATH` constants |
| `shared/core/AppConfigurations.kt` | Removed `TOURNAMENT_FEED_BASE_URL` |
| `shared/libs/preferences/PrefKeys.kt` | Removed `TOURNAMENT_INTRO_SHOWN`, `TOURNAMENT_LEADERBOARD_SUBSCRIPTION_NUDGE_LAST_SHOWN_DATE` |

### 10. Game Remnants Removed from Feed

Removed leftover game-specific state and UI that remained in the feed module after the game feature was deleted:

| File | Change |
|------|--------|
| `shared/features/feed/viewmodel/FeedViewModel.kt` | Removed `OnboardingStep.INTRO_GAME/INTRO_RANK/INTRO_GAME_END`; simplified `dismissOnboardingStep()` and `trackOnboardingShown()`; removed `showHotOrNotOnboarding` from `FeedState`; removed `OverlayType` enum entirely; removed `init` call to `setHonExperimentStatus()` |
| `shared/features/feed/ui/FeedActionsRight.kt` | Removed `OverlayType.GAME_TOGGLE` guard; profile/follow UI now always rendered (was always-true default) |
| `shared/features/feed/ui/FeedScreen.kt` | Removed `OnboardingStep.INTRO_RANK` from onboarding condition |
| `shared/features/feed/analytics/FeedTelemetry.kt` | Removed `INTRO_GAME/INTRO_RANK/INTRO_GAME_END` from `onboardingStepShown()` mapping; set `isGameEnabled = false` in all video tracking events; removed `setHonExperimentStatus()` wrapper |
| `shared/libs/analytics/events/YralEvents.kt` | Removed `INTRO_GAME/INTRO_RANK/INTRO_GAME_END` from `AnalyticsOnboardingStep` |
| `shared/libs/analytics/AnalyticsManager.kt` | Removed `setHonExperimentStatus()` method |
| `shared/libs/analytics/User.kt` | Removed `isHonExperiment` user property |
| `shared/features/root/viewmodels/RootViewModel.kt` | Removed `isHonExperiment = null` from `User` construction |

---

## Scale of Change

- **~21,850 lines deleted**, ~233 lines added (net removal of ~21,600 lines)
- **286 files changed**
- 4 Gradle modules removed
- 3 feature modules deleted entirely (`game`, `leaderboard`, `tournament`)
- 1 shared library deleted (`libs/leaderboard`)

---

## Build Verification

```
./gradlew :androidApp:assembleDebug
BUILD SUCCESSFUL
918 actionable tasks
```
