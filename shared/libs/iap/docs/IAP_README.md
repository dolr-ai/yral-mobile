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
- ✅ **Account Validation**: Automatic filtering of purchases by Google Play account to prevent cross-account access
- ✅ **Timeout Handling**: Automatic timeout and cleanup for purchase operations
- ✅ **Error Matching**: Intelligent error matching to specific product IDs
- ✅ **Thread-Safe**: All operations are thread-safe with proper synchronization

## Architecture

The module follows a provider pattern architecture:

```
┌─────────────────┐
│   IAPManager    │  ← Public API (consumers use this)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   IAPProvider   │  ← Interface (platform abstraction)
└────────┬────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌────────┐ ┌────────┐
│Android │ │  iOS   │  ← Platform-specific implementations
│Provider│ │Provider│
└────────┘ └────────┘
```

### Components

- **IAPManager**: Main entry point for IAP operations. Manages listeners and delegates to platform providers.
- **IAPProvider**: Interface defining the contract for platform-specific implementations.
- **AndroidIAPProvider**: Android implementation using Google Play Billing Library 8.0.
- **IOSIAPProvider**: iOS implementation using StoreKit.
- **IAPListener**: Interface for receiving IAP events (products fetched, purchase success/failure, etc.).

## Setup

### 1. Add Dependency

The module is already included in the project. Ensure `iapModule` is registered in your Koin configuration:

```kotlin
startKoin {
    modules(
        // ... other modules
        iapModule,
    )
}
```

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
    val isPurchased = iapManager.isProductPurchased(ProductId.PREMIUM_MONTHLY, userId)
    if (isPurchased) {
        // User has purchased this product
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
    val type: ProductType,          // SUBSCRIPTION or NON_CONSUMABLE
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
sealed class IAPError : Exception {
    data class ProductNotFound(val productId: String)
    data class PurchaseFailed(val productId: String, override val cause: Throwable?)
    data class PurchaseCancelled(val productId: String)
    data class BillingUnavailable(override val cause: Throwable?)
    data class NetworkError(override val cause: Throwable?)
    data class UnknownError(override val cause: Throwable?)
}
```

## Subscription Status Handling

The module tracks subscription lifecycle states to handle paused, cancelled, and expired subscriptions.

### Status States

- **ACTIVE**: Subscription is active and auto-renewing
- **PAUSED**: Subscription is paused (Android only)
- **CANCELLED**: Subscription is cancelled but still active until expiration
- **EXPIRED**: Subscription has expired
- **UNKNOWN**: Status cannot be determined

### Usage

```kotlin
// Check if subscription is active
val userId = sessionManager.userPrincipal
val isActive = iapManager.isProductPurchased(ProductId.PREMIUM_MONTHLY, userId)

// Or use Purchase helper
if (purchase.isActiveSubscription()) {
    // Subscription is active
}
```

**Note**: See `SubscriptionStatus.kt`, `Purchase.kt`, and platform provider files for detailed documentation on status determination, platform limitations, and best practices.

## Platform-Specific Details

### Android

- **Billing Library**: Google Play Billing Library 8.1.0
- **Context Requirement**: `Activity` context required for `purchaseProduct()`
- **Automatic Acknowledgment**: Purchases are automatically acknowledged
- **Subscription Support**: Full support for subscription products with all pricing phases

See `AndroidIAPProvider.kt` and `PurchaseManager.kt` for detailed implementation notes.

### iOS

- **StoreKit**: Native StoreKit framework (StoreKit 1)
- **No Context Required**: `purchaseProduct()` accepts `Unit` on iOS
- **Receipt Extraction**: Receipts are automatically extracted and included in `Purchase` model
- **Transaction Observer**: Automatically handles transaction state updates

See `IOSIAPProvider.kt` and `PurchaseManager.kt` for detailed implementation notes and limitations.

## Timeout and Error Handling

The module includes robust timeout and error handling:

- **Timeout**: Purchase operations timeout after 5 minutes
- **Cleanup**: Automatic cleanup of pending operations on timeout/cancellation
- **Error Matching**: Errors are matched to specific product IDs when possible
- **Thread Safety**: All operations are thread-safe with proper synchronization

## Account Validation

### Multiple Google Play Accounts

When users have multiple Google Play accounts on their device, subscriptions are tied to the specific Google Play account that made the purchase. The IAP module handles account validation to ensure users can only access subscriptions from their own Google Play account.

#### How It Works

1. **Account Identifier Storage**: When a purchase is made, the module:
   - Extracts the Google Play account identifier from the `Purchase` object using `accountIdentifiers.obfuscatedAccountId`
   - Gets the current app user ID from `SessionManager.userPrincipal`
   - Stores the mapping: `userId → accountIdentifier` in persistent storage (Preferences)

2. **Purchase Validation**: When checking if a product is purchased (`isProductPurchased`) or restoring purchases (`restorePurchases`):
   - Retrieves the stored account identifier for the given `userId`
   - If account identifier is **not found**, returns `false` (not purchased) or empty list
   - If account identifier is found, filters purchases to only include those matching the stored identifier

3. **Security**: The module uses a strict validation approach:
   - **No fallback behavior**: If account identifier mapping is missing, access is denied
   - This prevents cross-account access even after app reinstall
   - Backend can rehydrate the mapping using `setAccountIdentifier()`

#### Usage Example

```kotlin
// Get current user ID
val userId = sessionManager.userPrincipal

// Check if product is purchased (automatically filters by user's Google Play account)
val isPurchased = iapManager.isProductPurchased(ProductId.PREMIUM_MONTHLY, userId)

// Restore purchases (automatically filters by user's Google Play account)
val purchases = iapManager.restorePurchases(userId)
```

#### Backend Rehydration

If the account identifier mapping is lost (e.g., after app reinstall), the backend can restore it:

```kotlin
// After fetching user data from backend
val userId = sessionManager.userPrincipal
val accountIdentifier = backendResponse.googlePlayAccountId

// Rehydrate the mapping
iapManager.setAccountIdentifier(userId, accountIdentifier)

// Now purchases will work correctly
val isPurchased = iapManager.isProductPurchased(ProductId.PREMIUM_MONTHLY, userId)
```

**Backend Integration:**
- Store the Google Play account identifier (`obfuscatedAccountId`) on your backend when a purchase is made
- When user logs in, include the account identifier in the user profile/response
- Call `setAccountIdentifier()` to restore the mapping before checking purchases

#### Important Notes

- **Android**: Account validation is fully supported using Google Play Billing Library's account identifiers.
- **iOS**: Account validation is not applicable as iOS uses a single App Store account per device.
- **Security**: If account identifier mapping is missing, `isProductPurchased()` returns `false` and `restorePurchases()` returns empty list (no fallback to all purchases).
- **Persistence**: Account identifier mappings are stored in Preferences and persist across app restarts.

#### User Experience

When a user switches Google Play accounts:
- Subscriptions from the previous account are automatically filtered out
- Only subscriptions from the current account are considered
- Users must switch back to the original account to access their previous subscriptions

When a user reinstalls the app:
- Account identifier mapping is lost locally
- Backend must provide the mapping via `setAccountIdentifier()`
- Until mapping is restored, purchases will return `false`/empty list

This ensures that:
- User A's subscription cannot be accessed by User B
- Each account maintains its own subscription state
- No cross-account subscription access occurs
- Secure validation even after app reinstall

**Edge Cases:** See `IAP_EDGE_CASES.md` for detailed information about edge cases, potential issues, and how the module handles them.

## Best Practices

1. **Always use `rememberPurchase` in Compose**: It handles context automatically
2. **Register listeners early**: Register `IAPListener` when your screen/view model is created
3. **Unregister listeners**: Always unregister listeners to prevent memory leaks
4. **Handle all error cases**: Check for `PurchaseCancelled`, `PurchaseFailed`, etc.
5. **Validate purchases server-side**: Use `purchaseToken` (Android) or `receipt` (iOS) for server validation
6. **Always pass userId**: Use `SessionManager.userPrincipal` when calling `isProductPurchased()` and `restorePurchases()`
7. **Backend rehydration**: Store account identifier on backend and restore using `setAccountIdentifier()` after login
8. **Restore purchases**: Always provide a "Restore Purchases" option for users

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
        // iapManager.setAccountIdentifier(userId, backendAccountIdentifier)
        
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
├── src/
│   ├── commonMain/
│   │   └── kotlin/com/yral/shared/iap/
│   │       ├── IAPManager.kt          # Main API
│   │       ├── IAPListener.kt          # Event listener interface
│   │       ├── IAPError.kt             # Error types
│   │       ├── IAPComposeHelpers.kt   # Compose helpers
│   │       ├── di/IAPModule.kt         # Koin DI module
│   │       ├── model/                  # Data models
│   │       │   ├── Product.kt
│   │       │   ├── ProductId.kt
│   │       │   ├── Purchase.kt
│   │       │   └── ProductType.kt
│   │       └── providers/
│   │           └── IAPProvider.kt      # Provider interface
│   ├── androidMain/
│   │   └── kotlin/com/yral/shared/iap/providers/
│   │       ├── AndroidIAPProvider.kt
│   │       ├── BillingClientConnectionManager.kt
│   │       ├── ProductFetcher.kt
│   │       └── PurchaseManager.kt
│   └── iosMain/
│       └── kotlin/com/yral/shared/iap/providers/
│           ├── IOSIAPProvider.kt
│           ├── ProductFetcher.kt
│           ├── PurchaseManager.kt
│           ├── TransactionObserver.kt
│           └── util/IOSIAPUtils.kt
└── build.gradle.kts
```

## License

This module is part of the Yral mobile application.

