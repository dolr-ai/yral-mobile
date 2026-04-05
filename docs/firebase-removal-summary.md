# Firebase Removal Summary

## Scope

This cleanup removes Firebase usage tied to:

- Firebase Auth
- Firestore
- Firebase Cloud Functions request wiring from the mobile codebase

The following Firebase services remain in the project:

- Analytics
- Crashlytics
- Remote Config
- Messaging
- In-App Messaging
- Performance
- App Check
- Storage

Storage remains only for live product paths that still need it, such as support icon URL resolution in the account flow.

## Removed

### Shared modules

Removed from the build graph:

- `:shared:libs:firebaseAuth`
- `:shared:libs:firebaseStore`

Their code, DI wiring, and module registrations were removed from:

- `settings.gradle.kts`
- `shared/app/src/commonMain/kotlin/com/yral/shared/app/di/AppDI.kt`

### Backend / tooling

Removed:

- `firebase.json`
- `firestore.indexes.json`

The mobile code no longer uses Firebase Cloud Functions request wrappers.
This branch does not remove the repo's `functions/` directory itself.

### Firebase-backed app behavior

Removed:

- Firebase-authenticated session/bootstrap paths
- Firestore-backed read/write paths
- Cloud Functions request wrappers such as `FirebaseFunctionRequest`
- Balance mirroring/writeback through Firebase after auth/login

`GetBalanceUseCase` remains in the codebase, but the auth flow no longer invokes it as a Firebase sync side effect.

## Retained replacements / moved code

Some code previously living under removed Firebase modules was kept and relocated because the underlying behavior is still needed:

- App Check token helpers now live under `shared/data/`
- `GetIdTokenUseCase` now lives under `shared/data/`
- Storage download URL helper now lives in `shared/data/src/commonMain/kotlin/com/yral/shared/firebaseStore/StorageUtils.kt`

## Dependency changes

### Android / KMP

App Check is BOM-managed on Android again. In `shared/data/build.gradle.kts`, the Android source set uses:

- `platform(libs.firebase.bom)`
- versionless App Check artifacts from the version catalog

This replaced the temporary explicit App Check version pin.

### iOS / CocoaPods

The synthetic CocoaPods setup under `shared:data` still includes:

- `FirebaseAppCheck`
- `FirebaseStorage`

because App Check and Storage are still retained services.

## Known behavioral changes

- `DefaultAuthClient` no longer updates balance as part of post-login side effects.
- Account support icons still resolve through Firebase Storage and were intentionally preserved.
- Subscriptions no longer depend on the removed Firebase Auth / Firestore modules; those dependencies were stale and not used directly in subscriptions source.

## Verification status

Passing:

- `./gradlew detekt`
- `./gradlew :shared:features:auth:allTests`

Added / updated tests:

- `shared/data/src/commonTest/kotlin/com/yral/shared/data/RemovedFirebaseCloudFunctionsTest.kt`
- `shared/data/src/commonTest/kotlin/com/yral/shared/firebaseAuth/usecase/GetIdTokenUseCaseTest.kt`
- `shared/data/src/androidUnitTest/kotlin/com/yral/shared/firebaseStore/StorageUtilsTest.kt`
- `shared/features/feed/src/commonTest/kotlin/com/yral/shared/features/feed/domain/useCases/CheckVideoVoteUseCaseTest.kt`

Open issue:

- A broader targeted run involving feed/data iOS tests previously failed at iOS test link time with `framework 'FirebaseAppCheck' not found` in the iOS simulator test graph. That linker issue is separate from the Auth / Firestore / Cloud Functions removal itself and is still to be fully closed out.
