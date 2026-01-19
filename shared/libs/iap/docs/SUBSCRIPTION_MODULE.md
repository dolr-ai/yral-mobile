# Subscription Module - API Reference & Integration Guide

**Related Document:** [IAP Module README](./IAP_README.md)

This document provides API reference and integration guidance for the Subscription Module, which handles subscription status management, credit consumption, and subscription lifecycle.

## Overview

The Subscription Module (`shared/features/subscription/`) works alongside the IAP Module:

- **IAP Module**: Handles store operations (purchase, restore) and **automatically verifies ALL purchases** with backend
- **Subscription Module**: Handles subscription status management, credit consumption, and subscription lifecycle

### Key Principles

- **IAP module automatically verifies purchases** - No manual verification needed
- **Backend is source of truth** - Always query `/subscription/status` for subscription state
- **Entitlements always provided** - Even when inactive, for dynamic UI rendering
- **Child account sharing** - Handled automatically by backend (transparent to client)
- **Atomic credit consumption** - Backend prevents race conditions

## Subscription Management APIs

**Base URL:** `https://billing.yral.com`  
**Authentication:** `Authorization: Bearer {yral_id_token}` (same token as chat service)

### 1. Get Subscription Status

**Endpoint:** `GET /subscription/status?user_id={userId}`

**Purpose:** Get current subscription status, credits, and entitlements. **Always returns subscription object** (even when inactive) to enable dynamic UI rendering.

**When to Call:**
- On app launch to check subscription status
- After successful subscription purchase (to get credits/entitlements)
- Before accessing premium features
- Periodically to sync subscription state

**Request:**
```
GET /subscription/status?user_id=principal_id_here
Headers:
  Authorization: Bearer {yral_id_token}
```

**Response (Active Subscription):**
```json
{
  "status": "success",
  "subscription": {
    "isActive": true,
    "planType": "yral_pro_49",
    "renewalDate": 1234567890000,
    "expirationDate": 1234567890000,
    "creditsRemaining": 45,
    "creditsTotal": 60,
    "nextCreditResetDate": 1234567890000,
    "entitlements": [
      {
        "creditType": "ai_video",
        "enabled": true,
        "creditCost": 1,
        "benefitTitle": "Free AI Video Creation",
        "benefitDescription": "1 credit per AI video (resets monthly)",
        "benefitIcon": "smart_toy"
      },
      {
        "creditType": "tournament",
        "enabled": true,
        "creditCost": 1,
        "benefitTitle": "Tournament Access",
        "benefitDescription": "Join tournaments using credits",
        "benefitIcon": "emoji_events"
      },
      {
        "creditType": "global_feed",
        "enabled": true,
        "creditCost": 0,
        "benefitTitle": "Global Feed Access",
        "benefitDescription": "Access trending creators worldwide",
        "benefitIcon": "public"
      },
      {
        "creditType": "ai_chatbots",
        "enabled": true,
        "creditCost": 0,
        "benefitTitle": "Exclusive AI Chatbots",
        "benefitDescription": "Health & Lifestyle assistants",
        "benefitIcon": "chat"
      }
    ],
    "isSharedSubscription": false,
    "sharedFromAccountId": null
  }
}
```

**Response (Inactive/No Subscription):**
```json
{
  "status": "success",
  "subscription": {
    "isActive": false,
    "planType": null,
    "renewalDate": null,
    "expirationDate": null,
    "creditsRemaining": 0,
    "creditsTotal": 60,
    "nextCreditResetDate": null,
    "entitlements": [
      {
        "creditType": "ai_video",
        "enabled": false,
        "creditCost": 1,
        "benefitTitle": "Free AI Video Creation",
        "benefitDescription": "1 credit per AI video (resets monthly)",
        "benefitIcon": "smart_toy"
      }
      // ... other entitlements with enabled: false
    ],
    "isSharedSubscription": false,
    "sharedFromAccountId": null
  }
}
```

**Key Points:**
- **Always returns subscription object** (never 404) - even when user has no subscription
- When inactive: `isActive: false`, `enabled: false` for all entitlements, but benefit metadata is still provided
- When active: `isActive: true`, `enabled: true` for entitlements
- Backend automatically checks for direct subscription or shared subscription from parent account
- Use `entitlements` array to dynamically render purchase UI benefits

**Error Responses:**
- `401 UNAUTHORIZED`: Invalid or missing YRAL auth token
- `500 INTERNAL_ERROR`: Server error

### 2. Consume Credit

**Endpoint:** `POST /subscription/credits/consume`

**Purpose:** Consume credits for AI video creation or tournament entry. Backend handles atomic consumption (prevents race conditions).

**Request:**
```json
{
  "user_id": "principal_id_here",
  "credit_type": "ai_video" | "tournament",
  "video_id": "video_id_here",
  "tournament_id": "tournament_id"
}
```

**Response:**
```json
{
  "status": "success",
  "creditsRemaining": 44,
  "transactionId": "txn_123",
  "creditCost": 1
}
```

**Error Responses:**
- `400 BAD_REQUEST`: Invalid credit_type or missing required fields
- `401 UNAUTHORIZED`: Invalid or missing YRAL auth token
- `402 INSUFFICIENT_CREDITS`: Not enough credits available
- `403 FORBIDDEN`: Subscription expired or inactive
- `404 NOT_FOUND`: No active subscription found
- `500 INTERNAL_ERROR`: Server error

### 3. Purchase Verification (Handled by IAP Module)

**Note:** Purchase verification is **automatically handled by the IAP Module**. The Subscription Module does NOT need to verify purchases.

**Endpoint:** `POST /google/verify` (called automatically by IAP module)

**Purpose:** Verify purchase with Google Play/App Store for ALL product types. For subscription products, also grants subscription access with credits.

**Implementation:** See `shared/libs/iap/main/src/commonMain/kotlin/com/yral/shared/iap/verification/PurchaseVerificationService.kt`

The IAP module automatically calls this API after:
- `purchaseProduct()` - Verifies before returning purchase
- `restorePurchases()` - Verifies all purchases before returning

## Module Structure

**Location:** `shared/features/subscription/`

```
subscription/
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

## Data Models

### Domain Models

```kotlin
data class SubscriptionStatus(
    val isActive: Boolean,
    val planType: String?,
    val renewalDate: Long?,
    val expirationDate: Long?,
    val creditsRemaining: Int,
    val creditsTotal: Int,
    val nextCreditResetDate: Long?,
    val entitlements: List<Entitlement>,
    val isSharedSubscription: Boolean = false,
    val sharedFromAccountId: String? = null,
)

data class Entitlement(
    val creditType: CreditType,
    val enabled: Boolean,
    val creditCost: Int,
    val benefitTitle: String? = null,
    val benefitDescription: String? = null,
    val benefitIcon: String? = null,
)

enum class CreditType {
    AI_VIDEO,
    TOURNAMENT,
    GLOBAL_FEED,
    AI_CHATBOTS;
    
    fun toApiString(): String = when (this) {
        AI_VIDEO -> "ai_video"
        TOURNAMENT -> "tournament"
        GLOBAL_FEED -> "global_feed"
        AI_CHATBOTS -> "ai_chatbots"
    }
}
```

## Integration with IAP Module

### Purchase Flow

```kotlin
// 1. Check subscription status first
val status = subscriptionRepository.getSubscriptionStatus(userId)

if (status.isActive) {
    // User has subscription - show benefits
} else {
    // 2. No subscription - fetch products and show purchase flow
    val products = iapManager.fetchProducts(listOf(ProductId.YRAL_PRO))
    
    // 3. User purchases via IAP module (automatically verifies with backend)
    val purchase = iapManager.purchaseProduct(ProductId.YRAL_PRO, context)
    
    purchase.onSuccess { purchase ->
        // 4. Purchase is already verified by IAP module
        // For subscription products, refresh status to get credits/entitlements
        if (productId.getProductType() == ProductType.SUBS) {
            val updatedStatus = subscriptionRepository.getSubscriptionStatus(userId)
            // Update UI with credits and entitlements
        }
    }
}
```

### Auto-Restore on App Launch

```kotlin
// In SubscriptionViewModel
init {
    viewModelScope.launch {
        sessionManager.observeSessionProperty { it.userPrincipal }
            .collect { userId ->
                if (userId != null) {
                    // IAP module automatically restores and verifies purchases
                    iapManager.restorePurchases(userId)
                        .onSuccess { purchases ->
                            // Purchases are already verified by IAP module
                            // Refresh subscription status (backend is source of truth)
                            refreshSubscriptionStatus()
                        }
                        .onFailure { error ->
                            // Store restore failed - still check backend status
                            refreshSubscriptionStatus()
                        }
                }
            }
    }
}
```

## Feature Integration Examples

### AI Video Creation

```kotlin
fun generateAiVideo() {
    viewModelScope.launch {
        val userId = sessionManager.userPrincipal ?: return@launch
        
        // Check subscription status
        val subscriptionStatus = getSubscriptionStatusUseCase(userId).getOrNull()
        val aiVideoEntitlement = subscriptionStatus?.entitlements?.find { 
            it.creditType == CreditType.AI_VIDEO 
        }
        
        val hasFreeCredit = subscriptionStatus?.isActive == true &&
                           aiVideoEntitlement?.enabled == true &&
                           subscriptionStatus.creditsRemaining >= aiVideoEntitlement.creditCost
        
        if (hasFreeCredit) {
            // Consume credit
            consumeCreditUseCase(
                ConsumeCreditParams(
                    userId = userId,
                    creditType = CreditType.AI_VIDEO,
                    videoId = null
                )
            ).onSuccess { result ->
                // Proceed with AI video generation using FREE token type
                generateVideoWithTokenType(TokenType.FREE)
            }.onFailure { error ->
                // Fallback to YRAL tokens
                generateVideoWithTokenType(TokenType.SATS)
            }
        } else {
            // Use existing YRAL token flow
            generateVideoWithTokenType(TokenType.SATS)
        }
    }
}
```

### Tournament Registration

```kotlin
fun registerForTournament(tournament: Tournament, useCredit: Boolean = false) {
    viewModelScope.launch {
        val userId = sessionManager.userPrincipal ?: return@launch
        
        if (useCredit) {
            val subscriptionStatus = getSubscriptionStatusUseCase(userId).getOrNull()
            val tournamentEntitlement = subscriptionStatus?.entitlements?.find { 
                it.creditType == CreditType.TOURNAMENT 
            }
            
            val canUseCredit = subscriptionStatus?.isActive == true &&
                              tournamentEntitlement?.enabled == true &&
                              subscriptionStatus.creditsRemaining >= tournamentEntitlement.creditCost
            
            if (canUseCredit) {
                consumeCreditUseCase(
                    ConsumeCreditParams(
                        userId = userId,
                        creditType = CreditType.TOURNAMENT,
                        tournamentId = tournament.id
                    )
                ).onSuccess { result ->
                    // Register for tournament (no YRAL token deduction)
                    registerForTournamentUseCase(/* ... */)
                }.onFailure { error ->
                    // Fallback to YRAL token flow
                    registerWithYralTokens(tournament)
                }
            } else {
                registerWithYralTokens(tournament)
            }
        } else {
            registerWithYralTokens(tournament)
        }
    }
}
```

## Child Account Sharing

**Note:** Child account sharing is handled automatically by the backend. No special client-side handling is needed.

### How It Works

- Backend handles all logic internally - client doesn't need to know about parent-child relationships
- When `/subscription/status` is called for any `user_id`, backend automatically:
  - Checks for direct subscription
  - If not found, checks for shared subscription from parent account
  - Returns subscription status transparently (direct or shared)
- Client simply queries `/subscription/status` for any user_id (parent or child)
- Backend automatically manages credit pool sharing (child accounts use parent's credits)

### Response Format

The `/subscription/status` response includes:
```json
{
  "subscription": {
    "isSharedSubscription": true,
    "sharedFromAccountId": "parent_account_id",
    // ... other subscription fields
  }
}
```

## Dynamic UI Rendering

Benefits are configurable from the backend via entitlements:

- Each entitlement can include optional `benefitTitle`, `benefitDescription`, and `benefitIcon` fields
- **Entitlements are always provided** (even when subscription is inactive) for dynamic UI rendering
- When inactive: `enabled: false` for all entitlements, but benefit metadata is still provided
- When active: `enabled: true` for entitlements, user has access
- Client UI dynamically renders benefits from the `entitlements` array
- If `benefitTitle` is null, the entitlement is not displayed as a benefit (but may still be used for access control)

**Example UI Rendering:**

```kotlin
@Composable
fun SubscriptionBenefitsSheet(
    subscription: SubscriptionStatus?,
    onPurchaseClick: () -> Unit,
) {
    Column {
        // Show credits benefit (from subscription status)
        subscription?.let { sub ->
            if (sub.creditsTotal > 0) {
                BenefitItem(
                    title = "${sub.creditsTotal} Credits Monthly",
                    description = "Get ${sub.creditsTotal} credits every billing cycle"
                )
            }
        }
        
        // Render benefits from entitlements (configurable from backend)
        subscription?.entitlements?.forEach { entitlement ->
            if (entitlement.benefitTitle != null) {
                BenefitItem(
                    icon = getIconForBenefit(entitlement.benefitIcon),
                    title = entitlement.benefitTitle,
                    description = entitlement.benefitDescription ?: "",
                    enabled = entitlement.enabled
                )
            }
        }
    }
}
```

## Error Handling

### Common Error Scenarios

1. **Network Error**: Show retry option, allow offline functionality
2. **No Active Subscription**: Show purchase flow
3. **Insufficient Credits**: Show upgrade prompt or fallback to YRAL tokens
4. **Subscription Expired**: Show renewal prompt
5. **Purchase Cancelled**: No error (user intentionally cancelled)
6. **Verification Failed**: Handled by IAP module - show restore purchases option

### Error Handling Pattern

```kotlin
when (error) {
    is SubscriptionError.NoActiveSubscription -> {
        // Show purchase flow
        fetchProductsAndShowPurchaseFlow()
    }
    is SubscriptionError.InsufficientCredits -> {
        // Show upgrade prompt or fallback
        showUpgradePrompt()
    }
    is SubscriptionError.SubscriptionExpired -> {
        // Show renewal prompt
        showRenewalPrompt()
    }
    is SubscriptionError.NetworkError -> {
        // Show retry option
        showRetryOption()
    }
    else -> {
        // Generic error handling
        showError(error.message)
    }
}
```

## Best Practices

1. **Always check subscription status first** before showing purchase flow
2. **IAP module handles verification automatically** - No manual verification needed
3. **Backend is source of truth** - Always query `/subscription/status` for subscription state
4. **For subscription products: Auto-refresh status** after successful purchase (IAP module auto-verifies)
5. **Use entitlements for dynamic UI** - Always provided (even when inactive) for purchase UI rendering
6. **Use atomic operations** for credit consumption (backend handles this)
7. **Handle all error cases** gracefully with fallback options
8. **Child account sharing is automatic** - No special client handling needed
9. **Graceful fallback** to YRAL tokens if subscription unavailable
10. **Prevent concurrent operations** (disable buttons during processing)

## Koin DI Setup

**File:** `shared/features/subscription/di/SubscriptionModule.kt`

```kotlin
val subscriptionModule = module {
    // Data Source
    factoryOf(::SubscriptionRemoteDataSourceImpl) bind SubscriptionRemoteDataSource::class
    
    // Repository
    singleOf(::SubscriptionRepositoryImpl) bind SubscriptionRepository::class
    
    // Use Cases
    factoryOf(::GetSubscriptionStatusUseCase)
    factoryOf(::ConsumeCreditUseCase)
    // Note: VerifyPurchaseUseCase not needed - IAP module handles verification automatically
    
    // ViewModel
    viewModelOf(::SubscriptionViewModel)
}
```

**File:** `shared/app/src/commonMain/kotlin/com/yral/shared/app/di/AppDI.kt`

```kotlin
startKoin {
    modules(
        // ... existing modules
        subscriptionModule,
    )
}
```

## Complete Purchase Flow

```
1. App Launch
   → User opens app and logs in
   → IAP module automatically restores purchases and verifies with backend
   → Subscription module refreshes subscription status

2. Check Subscription Status
   → GET /subscription/status?user_id={userId}
   → Always returns subscription object (even when inactive)
   → Includes entitlements for dynamic UI rendering

3. If No Active Subscription
   → Fetch products from store (iapManager.fetchProducts)
   → Render purchase UI using entitlements from status response
   → User initiates purchase

4. Purchase & Verification (Automatic)
   → IAP module purchases from store
   → IAP module automatically verifies with backend (POST /google/verify)
   → Returns verified purchase

5. Refresh Status (For Subscription Products)
   → GET /subscription/status?user_id={userId}
   → Get credits and entitlements
   → Update UI with subscription active state
```

## Edge Cases

### Payment Failure
- Store provides grace period (typically 7-14 days)
- Backend handles grace period logic internally
- User retains access during grace period
- Client polls `/subscription/status` to detect changes

### Refund
- Store processes refund
- Backend handles refund processing internally
- Backend revokes entitlements and resets credits
- Client receives inactive subscription status on next `/subscription/status` call

### App Uninstall & Reinstall
- Subscription continues via store billing
- Backend maintains subscription status internally
- On reinstall: IAP module auto-restores and verifies purchases
- Subscription module refreshes status
- UI reflects active subscription immediately

### Credit Consumption Race Condition
- Backend handles credit consumption atomically (prevents race conditions)
- Backend checks credit balance and subscription status before consumption
- Returns error if insufficient credits or subscription inactive
- Client should disable button during consumption to prevent duplicate requests

---

**Related Documents:**
- [IAP Module README](./IAP_README.md) - IAP module documentation
- [IAP Edge Cases](./IAP_EDGE_CASES.md) - Edge cases and handling strategies
