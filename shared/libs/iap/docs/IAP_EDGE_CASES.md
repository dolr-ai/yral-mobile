# IAP Module - Edge Cases and Handling

This document describes edge cases and potential issues when using the IAP module, along with how the module handles them.

## Simplified Architecture

The IAP module follows a simplified, backend-driven architecture:
- **IAP Module**: Returns all purchases from store with automatically verifies purchases with backend
- **Subscription Module**: Handles subscription status management, credit consumption, and subscription lifecycle
- **Backend**: Handles account matching during verification (source of truth)

### Module Responsibilities

**IAP Module (Core + Main):**
- Fetch products from store
- Purchase products from store
- Restore purchases from store
- **Automatically verify ALL purchases with backend** (`POST /google/verify`)
- Return only verified purchases

**Implementation:** `shared/libs/iap/main/src/commonMain/kotlin/com/yral/shared/iap/providers/IAPProviderImpl.kt`

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
```

**Subscription Module:**
- Query subscription status (`GET /subscription/status`)
- Manage subscription lifecycle (active, paused, cancelled, expired)
- Handle credit consumption (`POST /subscription/credits/consume`)
- Manage entitlements and benefits
- Handle child account sharing (transparent to client)

**Location:** `shared/features/subscription/`

## Edge Cases

### 1. App Reinstall

**Scenario:**
- User has active subscription
- User uninstalls app
- User reinstalls app and logs in

**Current Behavior:**
- IAP module automatically restores purchases from store on app launch (via `restorePurchases()`)
- IAP module automatically verifies each purchase with backend
- Backend handles account matching internally
- Subscription module refreshes subscription status from backend
- UI reflects active subscription immediately

**Impact:** ✅ Works automatically - No local account identifier management needed

**Note:** Backend maintains subscription state, so reinstall works seamlessly.

---

### 2. Multiple Google Play Accounts

**Scenario:**
- User has multiple Google Play accounts on device
- User makes purchases with different accounts

**Current Behavior:**
- IAP module returns all purchases from store (no filtering)
- IAP module automatically verifies all purchases with backend
- Backend handles account matching internally
- Backend determines which purchases belong to which user

**Impact:** ✅ Backend handles account matching - No client-side complexity

---

### 3. Purchase Made While User ID is Null

**Scenario:**
- Purchase completes but `sessionManager.userPrincipal` is `null`
- User logs in later

**Current Behavior:**
- IAP module requires `userId` to be present for purchase verification
- If `userId` is null, `purchaseProduct()` throws `IAPError.UnknownError`
- If purchase completes but verification fails due to null userId, purchase is not verified
- On next app launch, `restorePurchases()` will automatically verify all purchases (including unverified ones)

**Impact:** ⚠️ Purchase should be verified after login

**Mitigation:**
- Ensure user is logged in before making purchases (best practice)
- Auto-restore on app launch will automatically verify all purchases via `restorePurchases()`
- IAP module handles verification automatically - no manual verification needed

---

### 4. Store Restore Fails

**Scenario:**
- Store restore operation fails (network error, timeout, etc.)
- User has active subscription on backend

**Current Behavior:**
- IAP module logs error and returns empty list or failure
- Subscription module still refreshes subscription status from backend
- Backend is source of truth - UI reflects backend state

**Impact:** ✅ Backend state is shown even if store restore fails

**Note:** Backend maintains subscription state independently of store restore.

---

### 5. Purchase Verification Fails

**Scenario:**
- Store-side purchase succeeds
- Backend verification fails (network error, invalid receipt, etc.)

**Current Behavior:**
- IAP module throws `IAPError.VerificationFailed` for `purchaseProduct()`
- For `restorePurchases()`, failed verifications are filtered out (logged as warnings)
- On next app launch, auto-restore will retry verification
- Backend should handle duplicate verification gracefully
- User can manually restore purchases to retry verification

**Implementation:**

```kotlin
// In PurchaseVerificationService.kt
suspend fun verifyPurchase(
    purchase: Purchase,
    userId: String,
): Result<Boolean> =
    handleIAPOperation {
        // ... verification logic ...
        if (!response.status.isSuccess()) {
            throw IAPError.VerificationFailed(
                purchase.productId,
                Exception("Backend verification failed with HTTP status: ${response.status.value}"),
            )
        }
        true
    }

// In IAPProviderImpl.kt - purchaseProduct throws error
verificationService.verifyPurchase(purchase, userId).fold(
    onSuccess = { purchase },
    onFailure = { error -> throw error }, // Throws VerificationFailed
)

// In IAPProviderImpl.kt - restorePurchases filters out failures
purchases.filter { purchase ->
    verificationService.verifyPurchase(purchase, userId).fold(
        onSuccess = { true },
        onFailure = { error ->
            Logger.w("IAPProviderImpl", error) {
                "Purchase verification error for product ${purchase.productId} during restore"
            }
            false // Filter out failed verifications
        },
    )
}
```

**Impact:** ⚠️ Verification should be retried

**Mitigation:**
- Auto-restore on app launch retries verification
- Backend should handle duplicate verification gracefully
- Query `/subscription/status` to check actual subscription state

---

### 6. User Switches Google Play Account

**Scenario:**
- User makes purchase with Google Play Account 1
- User switches to Google Play Account 2 in Play Store
- User makes another purchase

**Current Behavior:**
- IAP module returns all purchases from store (both accounts)
- IAP module automatically verifies all purchases with backend
- Backend handles account matching internally
- Backend determines which purchases belong to current user

**Impact:** ✅ Backend handles account matching - No client-side complexity

---

### 7. Multiple App Users, Same Google Play Account

**Scenario:**
- User A (app userId: `userA`) makes purchase with Google Play Account X
- User B (app userId: `userB`) logs in, also uses Google Play Account X
- User B makes purchase

**Current Behavior:**
- IAP module returns all purchases from store
- IAP module automatically verifies purchases with backend using `user_id` from `SessionManager`
- Backend matches purchases to correct user based on `user_id` in verification request
- Each user's subscription is managed independently

**Impact:** ✅ Safe - Backend handles user matching via `user_id` parameter

---

### 8. Subscription Expires During Use

**Scenario:**
- User starts AI video creation
- Subscription expires mid-process

**Current Behavior:**
- Backend checks subscription status before consuming credit
- Returns `SUBSCRIPTION_EXPIRED` if subscription inactive
- Client falls back to YRAL tokens

**Impact:** ✅ Graceful fallback to YRAL tokens

---

### 9. Network Error During Verification

**Scenario:**
- Store purchase succeeds
- Network error during backend verification

**Current Behavior:**
- IAP module catches network exceptions and throws `IAPError.NetworkError`
- For `purchaseProduct()`, throws `IAPError.NetworkError` (purchase fails)
- For `restorePurchases()`, filters out purchases with network errors (logged as warnings)
- On next app launch, auto-restore will retry verification
- Backend state is source of truth - query `/subscription/status` to check actual state

**Implementation:**

```kotlin
// In PurchaseVerificationService.kt
val response = try {
    httpClient.post { /* ... */ }
} catch (
    @Suppress("TooGenericExceptionCaught") e: Exception,
) {
    Logger.e(TAG, e) { "Network error during purchase verification for product ${purchase.productId}" }
    throw IAPError.NetworkError(
        Exception("Network error during purchase verification", e),
    )
}
```

**Impact:** ⚠️ Verification should be retried

**Mitigation:**
- Auto-restore on app launch retries verification
- Backend maintains subscription state independently
- Use common error handling utilities (`handleIAPOperation`, `handleIAPResultOperation`) for consistent error handling

---

### 10. Backend Verification Returns Error

**Scenario:**
- Store purchase succeeds
- Backend verification returns error (invalid receipt, user mismatch, etc.)

**Current Behavior:**
- IAP module throws `IAPError.VerificationFailed` with HTTP status code
- Purchase is not considered verified
- For `purchaseProduct()`, throws `IAPError.VerificationFailed` (purchase fails)
- For `restorePurchases()`, filters out failed verifications (logged as warnings)
- On next app launch, auto-restore will retry verification
- Backend should handle duplicate verification gracefully

**Implementation:**

```kotlin
// In PurchaseVerificationService.kt
val isSuccess = response.status.isSuccess()
if (!isSuccess) {
    Logger.w(TAG) {
        "Purchase verification failed for product ${purchase.productId}. " +
            "HTTP status: ${response.status.value}"
    }
    throw IAPError.VerificationFailed(
        purchase.productId,
        Exception("Backend verification failed with HTTP status: ${response.status.value}"),
    )
}
```

**Impact:** ⚠️ Verification should be retried

**Mitigation:**
- Auto-restore on app launch retries verification
- Backend should handle duplicate verification gracefully
- Query `/subscription/status` to check actual subscription state
- Handle `IAPError.VerificationFailed` in UI to show appropriate error message

---

## Best Practices

1. **Auto-restore on app launch**: IAP module should automatically restore purchases and verify with backend; Subscription module should refresh status from backend
2. **Backend is source of truth**: Always query `/subscription/status` to get subscription state - don't rely on local purchase state
3. **Verify ALL purchases**: IAP module automatically verifies all purchases (subscriptions, consumables, one-time) with backend
4. **Handle verification failures gracefully**: 
   - For `purchaseProduct()`, handle `IAPError.VerificationFailed` and `IAPError.NetworkError`
   - For `restorePurchases()`, failed verifications are automatically filtered out
   - Log errors and retry on next app launch
5. **Ensure user is logged in**: User should be logged in before making purchases (best practice)
6. **Query subscription status**: Always check `/subscription/status` to get current subscription state
7. **Use common error handling utilities**: Use `handleIAPOperation` and `handleIAPResultOperation` for consistent error handling
8. **Handle all error types**: Be aware of `IAPError.VerificationFailed` for verification failures

## Troubleshooting

### User Cannot Access Subscription

1. **Check subscription status**: Query `/subscription/status` - backend is source of truth
2. **Check auto-restore**: Ensure auto-restore runs on app launch
3. **Check verification**: Ensure purchases are verified with backend
4. **Check logs**: Look for verification errors or network issues

### Purchase Not Reflected in Subscription Status

1. **Verify purchase**: Ensure purchase was verified with backend (`POST /google/verify`)
2. **Check backend logs**: Backend should have processed the verification
3. **Refresh status**: Query `/subscription/status` to get latest state
4. **Retry restore**: Manually restore purchases to retry verification

### Store Restore Returns Empty List

1. **Check backend status**: Query `/subscription/status` - backend may have subscription even if store restore fails
2. **Check network**: Ensure network connectivity for store restore
3. **Check store account**: Ensure correct store account is active
4. **Backend is source of truth**: Always check backend status regardless of store restore result

### Verification Fails

1. **Check network**: Ensure network connectivity for verification
2. **Check purchase token**: Ensure purchase token is valid
3. **Check backend logs**: Backend should log verification errors
4. **Retry on next launch**: Auto-restore will retry verification on next app launch
5. **Manual restore**: User can manually restore purchases to retry verification
