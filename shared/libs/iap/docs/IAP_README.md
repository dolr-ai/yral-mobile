# IAP (In-App Purchase) Module

A Kotlin Multiplatform module for handling in-app purchases on Android and iOS. This module provides a unified API for managing product fetching, purchases, and purchase restoration across both platforms.

## Overview

The IAP module abstracts platform-specific implementations (Google Play Billing Library for Android and StoreKit for iOS) behind a common interface, making it easy to implement in-app purchases in a multiplatform codebase.

### Key Features

- ✅ **Unified API**: Single API for both Android and iOS
- ✅ **Type-Safe Product IDs**: Enum-based product identification
- ✅ **Event Listeners**: Reactive event handling via `IAPListener`
- ✅ **Compose Support**: Built-in Compose helpers for easy integration
- ✅ **Subscription Status Tracking**: Comprehensive handling of active, paused, cancelled, and expired subscriptions
- ✅ **Timeout Handling**: Automatic timeout and cleanup for purchase operations
- ✅ **Error Matching**: Intelligent error matching to specific product IDs
- ✅ **Thread-Safe**: All operations are thread-safe with proper synchronization
- ✅ **Backend-Driven**: No local account identifier management - backend handles account matching during verification

## Architecture

The module is split into two nested modules:

### Core Module (`shared/libs/iap/core`)

Pure IAP functionality without business logic:
- **No dependencies** on `Preferences`, `SessionManager`, or business logic
- Pure platform IAP operations (Billing Library/StoreKit)
- Package: `com.yral.shared.iap.core.*`
- Use this if you need vanilla IAP without account validation

### Main Module (`shared/libs/iap/main`)

Simplified wrapper around core:
- **Depends on** `iap/core`, `preferences`, `sessionManager`
- Adds userId-based account validation through BE
- Adds Compose helpers
- Package: `com.yral.shared.iap.*` (public API)
- **This is what you should use** in your app

```
┌─────────────────────────────────────────────────┐
│              Main Module (Public API)          │
│  ┌──────────────────────────────────────────┐  │
│  │   IAPManager (with userId support)       │  │
│  │   IAPProvider (with account validation)   │  │
│  │   IAPListener (with onWarning)            │  │
│  │   IAPProviderImpl (common, KMP)          │  │
│  └──────────────┬───────────────────────────┘  │
│                 │ Wraps                        │
│                 ▼                               │
│  ┌──────────────────────────────────────────┐  │
│  │         Core Module (Pure IAP)           │  │
│  │  ┌────────────────────────────────────┐  │  │
│  │  │   IAPManager (no userId)           │  │  │
│  │  │   IAPProvider (no userId)          │  │  │
│  │  │   IAPListener (no onWarning)        │  │  │
│  │  └──────────────┬─────────────────────┘  │  │
│  │                 │                          │  │
│  │    ┌────────────┴────────────┐            │  │
│  │    ▼                          ▼            │  │
│  │  ┌────────┐              ┌────────┐       │  │
│  │  │Android │              │  iOS   │       │  │
│  │  │Provider│              │Provider│       │  │
│  │  └────────┘              └────────┘       │  │
│  └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

### Components

**Main Module (Public API):**
- **IAPManager**: Main entry point
- **IAPProvider**: Interface - returns all purchases from store with BE verfication
- **IAPProviderImpl**: Common implementation that wraps core provider (works for both Android and iOS)
- **IAPListener**: Extends core listener with `onWarning` method

**Core Module (Internal):**
- **IAPManager**: Pure IAP manager without business logic
- **IAPProvider**: Interface without userId parameters
- **AndroidIAPProvider**: Pure Android IAP using Google Play Billing Library 8.1.0
- **IOSIAPProvider**: Pure iOS IAP using StoreKit
- **IAPListener**: Basic listener without `onWarning`

## Setup

### 1. Add Dependency

The module is already included in the project. Register both `iapCoreModule` and `iapModule` in your Koin configuration:

```kotlin
startKoin {
    modules(
        // ... other modules
        iapCoreModule,  // Core module (pure IAP)
        iapModule,      // Main module (with account validation)
    )
}
```

**Note**: The main module depends on the core module, so both must be registered. The core module provides pure IAP functionality, while the main module adds business logic (account validation, userId support).

### 2. Configure Product IDs

Edit `ProductId.kt` to add your product IDs:

```kotlin
enum class ProductId(val productId: String) {
    // Subscription products
    PREMIUM_MONTHLY("premium_monthly"),
    PREMIUM_YEARLY("premium_yearly"),
    
    // One-time purchase products
    REMOVE_ADS("remove_ads"),
    
    // Add your products here
}
```

**Important**: 
- For Android: Product IDs must match those in Google Play Console
- For iOS: Product IDs must match those in App Store Connect

### 3. Platform-Specific Setup

#### Android

No additional setup required. The module uses Google Play Billing Library 8.0.

#### iOS

No additional setup required. The module uses StoreKit.

## Usage

### Basic Usage

#### 1. Inject IAPManager

```kotlin
class MyViewModel(
    private val iapManager: IAPManager,
) : ViewModel() {
    // Use iapManager
}
```

#### 2. Fetch Products

```kotlin
viewModelScope.launch {
    val result = iapManager.fetchProducts(
        listOf(ProductId.PREMIUM_MONTHLY, ProductId.PREMIUM_YEARLY)
    )
    
    result.onSuccess { products ->
        // Display products to user
        products.forEach { product ->
            println("${product.title}: ${product.price}")
        }
    }.onFailure { error ->
        // Handle error
        when (error) {
            is IAPError.NetworkError -> { /* Network issue */ }
            is IAPError.ProductNotFound -> { /* Product not found */ }
            else -> { /* Other error */ }
        }
    }
}
```

#### 3. Make a Purchase (Compose)

```kotlin
@Composable
fun PurchaseScreen(iapManager: IAPManager) {
    val purchaseProduct = iapManager.rememberPurchase(
        onSuccess = { purchase ->
            // Handle successful purchase
            println("Purchase successful: ${purchase.productId}")
        },
        onError = { error ->
            // Handle purchase error
            when (error) {
                is IAPError.PurchaseCancelled -> {
                    // User cancelled
                }
                is IAPError.PurchaseFailed -> {
                    // Purchase failed
                }
                else -> {
                    // Other error
                }
            }
        }
    )
    
    Button(onClick = { purchaseProduct(ProductId.PREMIUM_MONTHLY) }) {
        Text("Buy Premium")
    }
}
```

#### 4. Make a Purchase (Non-Compose)

```kotlin
viewModelScope.launch {
    // On Android, you need to pass Activity context
    val activity = context as Activity
    
    val result = iapManager.purchaseProduct(
        productId = ProductId.PREMIUM_MONTHLY,
        context = activity
    )
    
    result.onSuccess { purchase ->
        // Handle success
    }.onFailure { error ->
        // Handle error
    }
}
```

#### 5. Restore Purchases

```kotlin
viewModelScope.launch {
    val userId = sessionManager.userPrincipal
    val result = iapManager.restorePurchases(userId)
    
    result.onSuccess { purchases ->
        // Process restored purchases
        purchases.forEach { purchase ->
            println("Restored: ${purchase.productId}")
        }
    }.onFailure { error ->
        // Handle error
    }
}
```

**Note**: Always pass `userId` from `SessionManager.userPrincipal` to ensure proper account validation.

#### 6. Check Purchase Status

```kotlin
viewModelScope.launch {
    val userId = sessionManager.userPrincipal
    val result = iapManager.isProductPurchased(ProductId.PREMIUM_MONTHLY, userId)
    result.onSuccess { isPurchased ->
        if (isPurchased) {
            // User has purchased this product
        }
    }.onFailure { error ->
        // Handle error checking purchase status
    }
}
```

**Note**: Always pass `userId` from `SessionManager.userPrincipal` to ensure proper account validation.

### Using IAPListener

For more advanced scenarios, implement `IAPListener`:

```kotlin
class MyIAPListener : IAPListener {
    override fun onProductsFetched(products: List<Product>) {
        // Handle products fetched
    }
    
    override fun onPurchaseSuccess(purchase: Purchase) {
        // Handle purchase success
    }
    
    override fun onPurchaseError(error: IAPError) {
        // Handle purchase error
    }
    
    override fun onPurchasesRestored(purchases: List<Purchase>) {
        // Handle purchases restored
    }
    
    override fun onRestoreError(error: IAPError) {
        // Handle restore error
    }
}

// Register listener
viewModelScope.launch {
    iapManager.addListener(myIAPListener)
}

// Don't forget to unregister
viewModelScope.launch {
    iapManager.removeListener(myIAPListener)
}
```

## Key Concepts

### ProductId Enum

All product IDs are defined as an enum for type safety:

```kotlin
enum class ProductId(val productId: String) {
    PREMIUM_MONTHLY("premium_monthly"),
    // ...
}
```

Benefits:
- Compile-time safety
- IDE autocomplete
- Easy refactoring
- No typos

### Product Model

Represents a product from the store:

```kotlin
data class Product(
    val id: String,
    val price: String,              // Formatted price (e.g., "$9.99")
    val priceAmountMicros: Long,    // Price in micros (for calculations)
    val currencyCode: String,
    val title: String,
    val description: String,
    val type: ProductType,          // SUBS or ONE_TIME
)
```

### Purchase Model

Represents a completed purchase with subscription status information:

```kotlin
data class Purchase(
    val productId: String,
    val purchaseToken: String?,         // Token for server validation (Android)
    val receipt: String? = null,         // Base64 receipt (iOS)
    val purchaseTime: Long,              // Timestamp in milliseconds
    val state: PurchaseState,            // PURCHASED, PENDING, or FAILED
    val expirationDate: Long? = null,    // Expiration date (subscriptions only)
    val isAutoRenewing: Boolean? = null, // Auto-renewal status (subscriptions only)
    val subscriptionStatus: SubscriptionStatus? = null, // Current subscription status
) {
    fun isActiveSubscription(): Boolean // Checks if subscription is currently active
}
```

#### Field Details

**Platform-Specific Fields:**
- `purchaseToken`: Android-specific token for server-side validation. Always null on iOS.
- `receipt`: iOS-specific base64-encoded receipt for server-side validation. Always null on Android.

**Subscription Fields:**
All subscription-related fields (`expirationDate`, `isAutoRenewing`, `subscriptionStatus`) are null for non-subscription products (one-time purchases).

**expirationDate:**
- For subscriptions, represents when the subscription period ends (milliseconds since epoch)
- **Android**: Not directly available from Purchase object. The Android Purchase API does not expose `getExpirationDate()`. Expiration dates must be:
  - Calculated from `purchaseTime` + subscription period (from ProductDetails)
  - Retrieved via server-side validation using `purchaseToken`
- **iOS**: Not available from StoreKit 1 transaction objects. Must be extracted from the receipt via server-side validation using the `receipt` field
- **Best Practice**: Always validate expiration dates server-side for production apps

**isAutoRenewing:**
- Indicates if the subscription will automatically renew at the end of the current billing period
- **true**: Subscription will automatically renew. User has active subscription
  - Note: During grace period or account hold (payment failures), this remains `true` even though payment failed. The subscription is still active during these periods
- **false**: Subscription is cancelled but remains active until expiration. User retains access until the end of the current billing period
- **null**: For non-subscription products or when status is unknown
- **Platform Availability:**
  - **Android**: Available directly from `Purchase.isAutoRenewing` property. According to [Android subscription lifecycle](https://developer.android.com/google/play/billing/lifecycle/subscriptions), `isAutoRenewing` remains `true` during grace periods and account holds
  - **iOS**: Not available from StoreKit 1. Requires server-side receipt validation
- **Important Notes:**
  - A cancelled subscription (auto-renewal disabled) is still considered active until expiration
  - Grace period and account hold states cannot be detected from `isAutoRenewing` alone (both remain `true`). Server-side validation is required to detect these states
  - Use `isActiveSubscription()` to check current access, which handles these cases

**subscriptionStatus:**
- Current subscription lifecycle state:
  - **ACTIVE**: Subscription is active, auto-renewing, and user has access. Includes subscriptions in grace period or account hold (payment failures), as these remain active and `isAutoRenewing` stays `true`
  - **PAUSED**: Subscription is paused (Android only). User retains access until pause period begins, then loses access. Can be resumed
  - **CANCELLED**: Subscription is cancelled but still active until expiration. User retains access until the end of the current billing period
  - **EXPIRED**: Subscription has expired. User no longer has access
  - **UNKNOWN**: Status cannot be determined (non-subscriptions or unavailable data)
- **Platform Availability:**
  - **Android**: Determined from `isAutoRenewing` and `isSuspended` (available in Billing Library 8.1.0+). According to [Android subscription lifecycle](https://developer.android.com/google/play/billing/lifecycle/subscriptions):
    - Grace period and account hold cannot be detected client-side (both show as ACTIVE)
    - Expiration checking requires server-side validation or calculation
    - Paused subscriptions may not appear in query results during pause period
  - **iOS**: Cannot be determined client-side. Requires server-side receipt validation

**isActiveSubscription():**
- Determines if this purchase represents an active subscription with current access
- A subscription is considered active if:
  1. It has a valid subscription status (not null or UNKNOWN)
  2. The status is ACTIVE or CANCELLED (cancelled subscriptions remain active until expiry)
  3. The expiration date (if available) is in the future
- **Important Notes:**
  - **ACTIVE** subscriptions include those in grace period or account hold (payment failures). According to [Android subscription lifecycle](https://developer.android.com/google/play/billing/lifecycle/subscriptions), subscriptions remain active during grace periods and account holds, with `isAutoRenewing` staying `true`. These states cannot be distinguished client-side
  - **CANCELLED** subscriptions are considered active until expiration
  - **PAUSED** subscriptions return false (user loses access during pause)
  - **EXPIRED** subscriptions return false
  - If expiration date is unavailable, status alone determines active state
- **Best Practice**: For production apps, use server-side validation to detect grace periods, account holds, and accurate expiration dates, as these cannot be determined from the Purchase object alone

#### Android Subscription Lifecycle

According to [Android subscription lifecycle](https://developer.android.com/google/play/billing/lifecycle/subscriptions):

- **Grace Period**: Payment failed but subscription remains active. `isAutoRenewing` stays `true`. Cannot be detected client-side - requires server-side validation
- **Account Hold**: Multiple payment failures. Subscription remains active, `isAutoRenewing` stays `true`. Cannot be detected client-side - requires server-side validation
- **Paused**: User-initiated pause. May not appear in query results during pause period
- **Cancelled**: Auto-renewal disabled but subscription active until expiration
- **Expired**: Subscription period ended. User loses access

**Platform Limitations:**
- **Android**: Expiration dates are not directly available from the Purchase object. They must be calculated from purchase time + subscription period or retrieved via server-side validation using the purchase token. Grace periods and account holds cannot be detected client-side
- **iOS**: Subscription status and expiration dates require server-side receipt validation. The receipt is included as base64-encoded data for this purpose

### Error Handling

All errors are represented by `IAPError` sealed class:

```kotlin
sealed class IAPError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    data class ProductNotFound(
        val productId: String,
    ) : IAPError("Product not found: $productId")
    
    data class PurchaseFailed(
        val productId: String,
        override val cause: Throwable? = null,
    ) : IAPError("Purchase failed for product: $productId", cause)
    
    data class PurchaseCancelled(
        val productId: String,
    ) : IAPError("Purchase cancelled for product: $productId")
    
    data class BillingUnavailable(
        override val cause: Throwable? = null,
    ) : IAPError("Billing service unavailable", cause)
    
    data class NetworkError(
        override val cause: Throwable? = null,
    ) : IAPError("Network error during IAP operation", cause)
    
    data class VerificationFailed(
        val productId: String,
        override val cause: Throwable? = null,
    ) : IAPError("Purchase verification failed for product: $productId", cause)
    
    data class UnknownError(
        override val cause: Throwable? = null,
    ) : IAPError("Unknown IAP error", cause)
}
```

#### Common Error Handling Utilities

The module provides reusable error handling functions to reduce code duplication:

**Location:** `shared/libs/iap/core/src/commonMain/kotlin/com/yral/shared/iap/core/util/IAPOperations.kt`

```kotlin
/**
 * Executes an IAP operation with standard error handling.
 * Handles CancellationException, IAPError, and generic Exception types.
 */
suspend inline fun <T> handleIAPOperation(
    crossinline operation: suspend () -> T,
): Result<T> {
    return try {
        Result.success(operation())
    } catch (e: CancellationException) {
        throw e
    } catch (e: IAPError) {
        Result.failure(e)
    } catch (
        @Suppress("TooGenericExceptionCaught")
        e: Exception,
    ) {
        Result.failure(IAPError.UnknownError(e))
    }
}

/**
 * Executes an IAP operation that already returns Result<T>.
 * This is useful for chaining operations that return Result.
 */
suspend inline fun <T> handleIAPResultOperation(
    crossinline operation: suspend () -> Result<T>,
): Result<T> {
    return try {
        operation()
    } catch (e: CancellationException) {
        throw e
    } catch (e: IAPError) {
        Result.failure(e)
    } catch (
        @Suppress("TooGenericExceptionCaught")
        e: Exception,
    ) {
        Result.failure(IAPError.UnknownError(e))
    }
}
```

**Usage Example:**

```kotlin
// In IAPProviderImpl.kt
override suspend fun purchaseProduct(
    productId: ProductId,
    context: Any?,
): Result<CorePurchase> =
    handleIAPResultOperation {
        sessionManager.userPrincipal?.let { userId ->
            coreProvider
                .purchaseProduct(productId, context, userId)
                .map { purchase ->
                    verificationService.verifyPurchase(purchase, userId).fold(
                        onSuccess = { purchase },
                        onFailure = { error -> throw error },
                    )
                }
        } ?: throw IAPError.UnknownError(Exception("User principal is null"))
    }
```

**Benefits:**
- ✅ Consistent error handling across all IAP operations
- ✅ Reduced code duplication
- ✅ Proper cancellation handling
- ✅ Automatic wrapping of exceptions as `IAPError.UnknownError`

## Subscription Status Handling

The module tracks subscription lifecycle states to handle paused, cancelled, and expired subscriptions.

### Status States

- **ACTIVE**: Subscription is active and auto-renewing
- **PAUSED**: Subscription is paused (Android only)
- **CANCELLED**: Subscription is cancelled but still active until expiration
- **EXPIRED**: Subscription has expired
- **UNKNOWN**: Status cannot be determined

### Implementation

**Location:** `shared/libs/iap/core/src/commonMain/kotlin/com/yral/shared/iap/core/model/Purchase.kt`

```kotlin
data class Purchase(
    val productId: String,
    val purchaseToken: String? = null,
    val receipt: String? = null,
    val purchaseTime: Long,
    val state: PurchaseState,
    val expirationDate: Long? = null,
    val isAutoRenewing: Boolean? = null,
    val subscriptionStatus: SubscriptionStatus? = null,
    val accountIdentifier: String? = null,
) {
    @OptIn(ExperimentalTime::class)
    fun isActiveSubscription(): Boolean {
        val status = subscriptionStatus ?: return false
        if (status == SubscriptionStatus.UNKNOWN) return false

        expirationDate?.let { expiry ->
            val currentTime = Clock.System.now().toEpochMilliseconds()
            if (expiry <= currentTime) {
                return false
            }
        }

        return status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.CANCELLED
    }
}
```

### Usage

```kotlin
// Check if subscription is active
val userId = sessionManager.userPrincipal
val result = iapManager.isProductPurchased(ProductId.PREMIUM_MONTHLY, userId)
result.onSuccess { isActive ->
    if (isActive) {
        // Subscription is active
    }
}.onFailure { error ->
    // Handle error checking subscription status
}

// Or use Purchase helper
if (purchase.isActiveSubscription()) {
    // Subscription is active
}
```

**Note**: See `SubscriptionStatus.kt`, `Purchase.kt`, and platform provider files for detailed documentation on status determination, platform limitations, and best practices.

**Important**: For production apps, use the **Subscription Module** (`shared/features/subscription/`) to query subscription status from the backend, as it provides more accurate status including credits, entitlements, and handles child account sharing.

## Platform-Specific Details

### Android

- **Billing Library**: Google Play Billing Library 8.1.0
- **Context Requirement**: `Activity` context required for `purchaseProduct()`
- **Automatic Acknowledgment**: Purchases are automatically acknowledged
- **Subscription Support**: Full support for subscription products with all pricing phases
- **Purchase Restoration**: Returns all purchases from store and verifies with BE using Google Play account identifiers

See `core/src/androidMain/.../AndroidIAPProvider.kt` and `PurchaseManager.kt` for detailed implementation notes.

### iOS

- **StoreKit**: Native StoreKit framework (StoreKit 1)
- **No Context Required**: `purchaseProduct()` accepts `Unit` on iOS
- **Receipt Extraction**: Receipts are automatically extracted and included in `Purchase` model
- **Transaction Observer**: Automatically handles transaction state updates
- **Purchase Restoration**: Returns all purchases from store (no filtering)

See `core/src/iosMain/.../IOSIAPProvider.kt` and `PurchaseManager.kt` for detailed implementation notes and limitations.

## Timeout and Error Handling

The module includes robust timeout and error handling:

- **Timeout**: Purchase operations timeout after 5 minutes
- **Cleanup**: Automatic cleanup of pending operations on timeout/cancellation
- **Error Matching**: Errors are matched to specific product IDs when possible
- **Thread Safety**: All operations are thread-safe with proper synchronization

## Architecture Overview

### Simplified Design

The IAP module follows a simplified, backend-driven architecture:

1. **Core Module**: Provides purchases from store (no business logic)
2. **Main Module**: Simple wrapper that returns all purchases (no filtering) and handles purchase verification
3. **Subscription Module**: Handles subscription status management, credit consumption, and subscription lifecycle

**Key Principle**: Backend is the source of truth. All purchases are returned from store, and backend handles account matching during verification.

#### Module Responsibilities

**IAP Module (Core + Main):**
- Fetch products from store
- Purchase products from store
- Restore purchases from store
- **Automatically verify ALL purchases with backend** (`POST /google/verify`)
- Return only verified purchases

**Subscription Module:**
- Query subscription status (`GET /subscription/status`)
- Manage subscription lifecycle (active, paused, cancelled, expired)
- Handle credit consumption (`POST /subscription/credits/consume`)
- Manage entitlements and benefits
- Handle child account sharing (transparent to client)

### Purchase Flow

1. **Store Operations** (IAP Module):
   - Fetch products from store
   - Purchase product (returns purchase object)
   - Restore purchases (returns all purchases from store)

2. **Backend Verification** (Subscription Module):
   - Verify each purchase with backend (`POST /google/verify`)
   - Backend handles account matching internally
   - Backend grants subscription access and credits

3. **Subscription Status** (Subscription Module):
   - Query subscription status (`GET /subscription/status`)
   - Backend returns subscription details (credits, entitlements)
   - Backend automatically handles child account sharing

### Benefits

- ✅ **No local account identifier management** - Works on reinstall automatically
- ✅ **Backend is source of truth** - More reliable and secure
- ✅ **Simpler code** - Less complexity, fewer edge cases
- ✅ **Automatic restore** - App launch automatically restores and verifies purchases

**Edge Cases:** See `IAP_EDGE_CASES.md` for detailed information about edge cases, potential issues, and how the module handles them.

## Purchase Verification

**The IAP module automatically verifies ALL purchases with the backend** before returning them.

### Implementation

**Location:** `shared/libs/iap/main/src/commonMain/kotlin/com/yral/shared/iap/verification/PurchaseVerificationService.kt`

```kotlin
internal class PurchaseVerificationService(
    private val httpClient: HttpClient,
    private val preferences: Preferences,
) {
    suspend fun verifyPurchase(
        purchase: Purchase,
        userId: String,
    ): Result<Boolean> =
        handleIAPOperation {
            val purchaseToken = purchase.purchaseToken
            val idToken = preferences.getString(PrefKeys.ID_TOKEN.name)

            if (purchaseToken == null || idToken == null) {
                throw IAPError.UnknownError(
                    Exception("Missing required tokens for verification")
                )
            }

            val response = try {
                httpClient.post {
                    url {
                        host = AppConfigurations.BILLING_BASE_URL
                        path("google/verify")
                    }
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $idToken")
                    }
                    setBody(
                        VerifyPurchaseRequest(
                            userId = userId,
                            packageName = PackageNameProvider.getPackageName(),
                            productId = purchase.productId,
                            purchaseToken = purchaseToken,
                        ),
                    )
                }
            } catch (e: Exception) {
                throw IAPError.NetworkError(
                    Exception("Network error during purchase verification", e),
                )
            }

            if (!response.status.isSuccess()) {
                throw IAPError.VerificationFailed(
                    purchase.productId,
                    Exception("Backend verification failed with HTTP status: ${response.status.value}"),
                )
            }

            true
        }
}
```

**Integration in IAPProviderImpl:**

```kotlin
override suspend fun purchaseProduct(
    productId: ProductId,
    context: Any?,
): Result<CorePurchase> =
    handleIAPResultOperation {
        sessionManager.userPrincipal?.let { userId ->
            coreProvider
                .purchaseProduct(productId, context, userId)
                .map { purchase ->
                    verificationService.verifyPurchase(purchase, userId).fold(
                        onSuccess = { purchase },
                        onFailure = { error -> throw error },
                    )
                }
        } ?: throw IAPError.UnknownError(Exception("User principal is null"))
    }

override suspend fun restorePurchases(userId: String?): Result<List<CorePurchase>> =
    handleIAPResultOperation {
        userId?.let {
            coreProvider
                .restorePurchases()
                .map { purchases ->
                    purchases.filter { purchase ->
                        verificationService.verifyPurchase(purchase, userId).fold(
                            onSuccess = { true },
                            onFailure = { error ->
                                Logger.w("IAPProviderImpl", error) {
                                    "Purchase verification error for product ${purchase.productId} during restore"
                                }
                                false
                            },
                        )
                    }
                }
        } ?: throw IAPError.UnknownError(Exception("User principal is null"))
    }
```

### IAP Module Responsibilities

The IAP module handles:
- Fetch products from store
- Purchase product (verifies with backend before returning)
- Restore purchases (verifies all purchases with backend before returning)
- Backend handles account matching internally during verification

### Verification Flow

1. **IAP Module**: Purchases from store or restores purchases
2. **IAP Module**: Automatically verifies each purchase with backend (`POST /google/verify`)
   - Backend validates receipt with App Store/Play Store
   - Backend handles account matching internally
   - Backend grants subscription access and credits (for subscription products)
3. **IAP Module**: Returns only verified purchases
   - For `purchaseProduct`: Returns purchase after verification (throws `IAPError.VerificationFailed` if verification fails)
   - For `restorePurchases`: Returns only purchases that were successfully verified (filters out failed verifications)

**Important:** The IAP module verifies ALL purchases (subscriptions, consumables, one-time purchases) with the backend automatically. Only verified purchases are returned.

## Subscription Module

**The Subscription Module handles subscription status management, credit consumption, and subscription lifecycle.**

### Overview

The Subscription Module is a separate feature module (`shared/features/subscription/`) that works alongside the IAP module:

- **IAP Module**: Handles store operations (purchase, restore) and automatic purchase verification
- **Subscription Module**: Handles subscription status, credits, entitlements, and subscription lifecycle

### Key Responsibilities

1. **Subscription Status Management**
   - Query subscription status from backend (`GET /subscription/status`)
   - Track subscription lifecycle (active, paused, cancelled, expired)
   - Handle child account sharing (transparent to client)

2. **Credit Management**
   - Consume credits for premium features (`POST /subscription/credits/consume`)
   - Track remaining credits and credit reset dates
   - Atomic credit consumption (prevents race conditions)

3. **Entitlements Management**
   - Manage subscription benefits and entitlements
   - Provide dynamic UI rendering based on subscription status

### Module Structure

```
shared/features/subscription/
├── data/
│   ├── SubscriptionRemoteDataSource.kt
│   ├── SubscriptionRepositoryImpl.kt
│   └── models/
│       ├── SubscriptionStatusResponse.kt
│       └── ConsumeCreditRequest.kt
├── domain/
│   ├── repository/
│   │   └── SubscriptionRepository.kt
│   ├── model/
│   │   ├── SubscriptionStatus.kt
│   │   ├── Entitlement.kt
│   │   └── CreditType.kt
│   └── usecases/
│       ├── GetSubscriptionStatusUseCase.kt
│       └── ConsumeCreditUseCase.kt
├── viewmodel/
│   └── SubscriptionViewModel.kt
└── di/
    └── SubscriptionModule.kt
```

### Integration with IAP Module

The Subscription Module integrates with the IAP module as follows:

1. **After Purchase**: IAP module automatically verifies purchase with backend
2. **Status Refresh**: Subscription module queries `/subscription/status` to get credits and entitlements
3. **Credit Consumption**: Subscription module consumes credits when using premium features

**Example Flow:**

```kotlin
// 1. IAP module purchases and verifies (automatic)
val purchase = iapManager.purchaseProduct(ProductId.YRAL_PRO, context)

purchase.onSuccess { purchase ->
    // 2. Subscription module refreshes status to get credits
    if (productId.getProductType() == ProductType.SUBS) {
        val status = subscriptionRepository.getSubscriptionStatus(userId)
        // Update UI with credits and entitlements
    }
}

// 3. When using premium feature
val result = subscriptionRepository.consumeCredit(
    userId = userId,
    creditType = CreditType.AI_VIDEO,
    videoId = videoId,
)
```

## Subscription APIs Integration

For subscription management (credits, entitlements, status), use the **Subscription APIs** from the YRAL Billing Service.

### Overview

After a successful in-app purchase (for subscription products):
1. **IAP module purchases from store** and **automatically verifies with backend** (`/google/verify`)
2. **IAP module returns verified purchase**
3. **Subscription module refreshes subscription status** (`/subscription/status`) to get credits and entitlements
4. Consume credits when using premium features (`/subscription/credits/consume`)

### Key APIs

**Base URL:** `https://billing.yral.com`  
**Authentication:** `Authorization: Bearer {yral_id_token}` (same as chat.yral.com)

1. **`POST /google/verify`** - Verify purchase for ALL product types (existing, enhanced)
   - **Called automatically by IAP Module** after store purchase/restore
   - Backend handles account matching internally
   - For subscription products: Backend grants credits and sets up entitlements
   - Returns success confirmation

2. **`GET /subscription/status?user_id={userId}`** - Get subscription status, credits, entitlements
   - Called first when user opens app/subscription screen
   - Called after purchase to get full details
   - Automatically handles child account sharing (transparent to client)
   - If no subscription found, client should fetch products and show purchase flow

3. **`POST /subscription/credits/consume`** - Consume credits for AI video/tournament
   - Called before AI video creation or tournament entry
   - Atomically deducts credits (transaction-safe)
   - Returns remaining credits

### Complete Purchase Flow

```kotlin
// 1. Check subscription status first (on app launch or subscription screen)
val subscriptionStatus = subscriptionApi.getSubscriptionStatus(userId)

if (subscriptionStatus.isActive) {
    // User has subscription (direct or shared) - show benefits
    showSubscriptionBenefits(subscriptionStatus)
} else {
    // 2. No subscription - fetch products from store
    val products = iapManager.fetchProducts(listOf(ProductId.YRAL_PRO))
    showPurchaseFlow(products)
    
    // 3. User purchases via IAP module (automatically verifies with backend)
    val purchase = iapManager.purchaseProduct(ProductId.YRAL_PRO, context)
    
    purchase.onSuccess { purchase ->
        // 4. Purchase is already verified by IAP module
        // For subscription products, refresh subscription status to get credits/entitlements
        if (productId.getProductType() == ProductType.SUBS) {
            val updatedStatus = subscriptionApi.getSubscriptionStatus(userId)
            // Update UI with credits and entitlements
            showSubscriptionBenefits(updatedStatus)
        }
    }
}
```

### Important Notes

- **Purchase verification is automatic**: The IAP module automatically verifies ALL purchases with backend (`/google/verify`) before returning them. Only verified purchases are returned.
- **Restore purchases**: `restorePurchases()` verifies all purchases with backend before returning. Only successfully verified purchases are included in the result.
- **Backend is source of truth**: Always query `/subscription/status` to get subscription state. Backend handles account matching and child account sharing automatically.
- **Child account sharing**: Handled automatically in the backend. The `/subscription/status` API automatically checks for shared subscriptions from parent accounts. Client simply queries subscription status for any user_id (parent or child).
- **Purchase flow**: Always check subscription status first. If no subscription, then fetch products from Play Store/App Store and show purchase flow.
- **Token source**: YRAL ID token is obtained from app preferences (same token used for chat service) via `PrefKeys.ID_TOKEN.name`.

For detailed end-to-end flow and API documentation, see [SUBSCRIPTION_END_TO_END.md](./SUBSCRIPTION_END_TO_END.md).

For client implementation details and code examples, see [SUBSCRIPTION_CLIENT_IMPLEMENTATION.md](./SUBSCRIPTION_CLIENT_IMPLEMENTATION.md).

## Best Practices

1. **Always use `rememberPurchase` in Compose**: It handles context automatically
2. **Register listeners early**: Register `IAPListener` when your screen/view model is created
3. **Unregister listeners**: Always unregister listeners to prevent memory leaks
4. **Handle all error cases**: Check for `PurchaseCancelled`, `PurchaseFailed`, etc.
5. **Purchase verification is automatic**: The IAP module automatically verifies ALL purchases with backend before returning them. No manual verification needed.
6. **Restore purchases**: `restorePurchases()` automatically verifies all purchases with backend and returns only verified purchases
7. **Backend is source of truth**: Always query `/subscription/status` to get subscription state - don't rely on local purchase state
8. **Restore purchases**: Always provide a "Restore Purchases" option for users (IAP module handles verification automatically)
9. **Check subscription status first**: Before showing purchase flow, check `/subscription/status` to see if user already has subscription (direct or shared)
10. **For subscription products: Auto-refresh status**: After successful purchase (IAP module auto-verifies), automatically call `/subscription/status` to get credits and entitlements

## Example: Complete Purchase Flow

```kotlin
@Composable
fun PremiumScreen(iapManager: IAPManager) {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    val purchaseProduct = iapManager.rememberPurchase(
        onSuccess = { purchase ->
            // Navigate to premium content
            navigateToPremium()
        },
        onError = { error ->
            when (error) {
                is IAPError.PurchaseCancelled -> {
                    // User cancelled - no action needed
                }
                is IAPError.PurchaseFailed -> {
                    // Show error message
                    showError("Purchase failed: ${error.cause?.message}")
                }
                else -> {
                    showError("An error occurred")
                }
            }
        }
    )
    
    LaunchedEffect(Unit) {
        isLoading = true
        val userId = sessionManager.userPrincipal
        
        // Restore account identifier from backend if needed
        // (This should be done during login/authentication)
        // No account identifier management needed - backend handles account matching during verification
        
        iapManager.fetchProducts(
            listOf(ProductId.PREMIUM_MONTHLY, ProductId.PREMIUM_YEARLY)
        ).onSuccess {
            products = it
        }.onFailure {
            showError("Failed to load products")
        }
        isLoading = false
    }
    
    if (isLoading) {
        CircularProgressIndicator()
    } else {
        products.forEach { product ->
            ProductCard(
                product = product,
                onPurchase = { purchaseProduct(ProductId.fromString(product.id)!!) }
            )
        }
    }
}
```

## Troubleshooting

### Android

- **"Activity context is required"**: Make sure you're passing `Activity` from `LocalContext.current` in Compose
- **Products not found**: Verify product IDs match Google Play Console exactly
- **Purchase not completing**: Check that billing is enabled in Google Play Console

### iOS

- **Products not found**: Verify product IDs match App Store Connect exactly
- **Purchase not completing**: Ensure you're testing on a real device (not simulator) or using sandbox account
- **Receipt issues**: Receipt extraction requires proper App Store configuration

## Module Structure

```
shared/libs/iap/
├── core/                                    # Core module (pure IAP)
│   └── src/
│       ├── commonMain/
│       │   └── kotlin/com/yral/shared/iap/core/
│       │       ├── IAPManager.kt           # Core IAP manager
│       │       ├── IAPListener.kt          # Core listener
│       │       ├── IAPError.kt              # Error types
│       │       ├── model/                   # Data models
│       │       │   ├── Product.kt
│       │       │   ├── ProductId.kt
│       │       │   ├── Purchase.kt
│       │       │   ├── ProductType.kt
│       │       │   ├── PurchaseState.kt
│       │       │   └── SubscriptionStatus.kt
│       │       ├── providers/
│       │       │   └── IAPProvider.kt       # Core provider interface
│       │       └── di/
│       │           └── IAPModule.kt        # Core DI module
│       ├── androidMain/
│       │   └── kotlin/com/yral/shared/iap/core/providers/
│       │       ├── AndroidIAPProvider.kt
│       │       ├── BillingClientConnectionManager.kt
│       │       ├── ProductFetcher.kt
│       │       └── PurchaseManager.kt
│       └── iosMain/
│           └── kotlin/com/yral/shared/iap/core/providers/
│               ├── IOSIAPProvider.kt
│               ├── ProductFetcher.kt
│               ├── PurchaseManager.kt
│               ├── TransactionObserver.kt
│               └── util/
│                   └── IOSIAPUtils.kt
└── main/                                    # Main module (business logic)
    └── src/
        ├── commonMain/
        │   └── kotlin/com/yral/shared/iap/
        │       ├── IAPManager.kt            # Main IAP manager (with userId)
        │       ├── IAPListener.kt            # Main listener (with onWarning)
        │       ├── IAPComposeHelpers.kt      # Compose helpers
        │       ├── providers/
        │       │   ├── IAPProvider.kt        # Main provider interface
        │       │   └── IAPProviderImpl.kt     # Common implementation (KMP)
        │       └── di/
        │           └── IAPModule.kt          # Main DI module
        ├── androidMain/
        │   └── kotlin/com/yral/shared/iap/
        │       └── IAPComposeHelpers.android.kt
        └── iosMain/
            └── kotlin/com/yral/shared/iap/
                └── IAPComposeHelpers.ios.kt
```

## Related Documentation

- [IAP Edge Cases](./IAP_EDGE_CASES.md) - Edge cases and handling strategies
- [Subscription Module](./SUBSCRIPTION_MODULE.md) - Subscription Module API reference and integration guide

## License

This module is part of the Yral mobile application.

