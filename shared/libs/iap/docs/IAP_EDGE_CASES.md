# IAP Module - Edge Cases and Handling

This document describes edge cases and potential issues when using the IAP module with account validation, along with how the module handles them.

## Account Identifier Mapping

The IAP module maintains a one-to-many mapping between app user IDs and Google Play account identifiers. This allows users to have purchases from multiple Google Play accounts while maintaining security.

### Edge Cases

#### 1. User Switches Google Play Account After Purchase

**Scenario:**
- User makes purchase with Google Play Account 1 → stores mapping `userId → [account1]`
- User switches to Google Play Account 2 in Play Store
- User makes another purchase → new purchase has `account2`, adds to mapping: `userId → [account1, account2]`

**Current Behavior:**
- Both account identifiers are stored
- User can access purchases from both accounts
- Warning is logged when account identifier changes

**Impact:** ✅ Positive - User maintains access to purchases from both accounts

**Recommendation:** Monitor logs for account identifier changes to understand user behavior patterns.

---

#### 2. Multiple App Users, Same Google Play Account

**Scenario:**
- User A (app userId: `userA`) makes purchase with Google Play Account X → stores `userA → [accountX]`
- User B (app userId: `userB`) logs in, also uses Google Play Account X
- User B makes purchase → stores `userB → [accountX]`

**Current Behavior:**
- Each app user has their own mapping
- Filtering by `userId` prevents cross-user access
- Both users can have purchases from the same Google Play account

**Impact:** ✅ Safe - No cross-user access due to userId filtering

**Note:** This is expected behavior for family sharing scenarios where multiple app users share a Google Play account.

---

#### 3. Account Identifier Overwrite Prevention

**Scenario:**
- User makes purchase with Account 1 → stores `userId → [account1]`
- User switches Google Play account
- User makes purchase with Account 2 → adds to mapping: `userId → [account1, account2]`

**Current Behavior:**
- New account identifier is added to the set (not overwritten)
- User maintains access to purchases from both accounts
- Warning is logged for monitoring

**Impact:** ✅ Positive - No data loss, user maintains access to all purchases

---

#### 4. Null Account Identifier in Purchase

**Scenario:**
- Purchase succeeds but `purchase.accountIdentifiers?.obfuscatedAccountId` is `null`
- Mapping is not stored
- Future purchase checks return `false`

**Current Behavior:**
- Mapping is only stored if `accountIdentifier != null`
- Warning is logged when account identifier is null
- User will need backend rehydration to restore access

**Impact:** ⚠️ User loses access until backend provides account identifier

**Mitigation:**
- Backend should store account identifier when purchase is validated server-side
- Backend can rehydrate using `setAccountIdentifier()` after login

---

#### 5. Backend Provides Incorrect Account Identifier

**Scenario:**
- Backend returns wrong `accountIdentifier` for user
- App calls `setAccountIdentifier(userId, wrongAccountId)`
- Validation checks if identifier matches any purchases

**Current Behavior:**
- `setAccountIdentifier()` validates the identifier against actual purchases
- If identifier doesn't match any purchases, it's not stored
- Warning is logged

**Impact:** ✅ Safe - Invalid identifiers are rejected

**Recommendation:** Backend should always return the account identifier from the most recent purchase validation.

---

#### 6. User Logs Out and Different User Logs In

**Scenario:**
- User A logs in, makes purchase → stores `userA → [account1]`
- User A logs out
- User B logs in (different app user, same device)
- User B's purchases are checked

**Current Behavior:**
- Mapping is per `userId`, so no conflict
- User B's purchases are filtered by their own `userId`
- Account identifier mappings are preserved per user

**Impact:** ✅ Safe - No cross-user access

**Note:** Mappings are not cleared on logout, allowing users to restore access when they log back in.

---

#### 7. Account Identifier Format Changes

**Scenario:**
- Google Play changes `obfuscatedAccountId` format
- Old stored identifiers no longer match new format
- User loses access

**Current Behavior:**
- Module uses identifiers as-is from Google Play Billing Library
- If format changes, stored identifiers won't match new purchases

**Impact:** ⚠️ Breaking change from Google Play

**Mitigation:**
- Google Play typically maintains backward compatibility
- If format changes, users may need to re-authenticate purchases
- Backend rehydration can restore access with new format

---

#### 8. User Reinstalls App, Backend Provides Stale Account Identifier

**Scenario:**
- User makes purchase with Account 1
- User switches to Account 2, makes purchase
- User reinstalls app
- Backend returns old Account 1 identifier
- User loses access to Account 2 purchases

**Current Behavior:**
- `setAccountIdentifier()` validates identifier against actual purchases
- Only identifiers that match purchases are stored
- If backend provides stale identifier, it may not match current purchases

**Impact:** ⚠️ User may lose access if backend provides wrong identifier

**Recommendation:**
- Backend should return the most recent account identifier
- Backend should support multiple account identifiers per user
- Consider calling `restorePurchases()` first to discover current account identifiers

---

#### 9. Purchase Made While User ID is Null

**Scenario:**
- Purchase completes but `sessionManager.userPrincipal` is `null`
- Account identifier is not stored
- User logs in later, but mapping is missing

**Current Behavior:**
- Warning is logged when userId is null
- Account identifier is not stored
- User will need backend rehydration to restore access

**Impact:** ⚠️ User loses access until backend provides mapping

**Mitigation:**
- Ensure user is logged in before making purchases
- Backend should store account identifier when validating purchase server-side
- Backend can rehydrate using `setAccountIdentifier()` after login

---

#### 10. Family Sharing / Multiple Devices

**Scenario:**
- User has purchases on Device A with Account 1
- User logs in on Device B with same app userId
- Device B has different Google Play account active
- Backend provides Account 1 identifier
- Device B cannot find purchases because active account is different

**Current Behavior:**
- Module filters purchases by stored account identifiers
- If Device B's active account doesn't match stored identifiers, no purchases are found

**Impact:** ⚠️ User must switch to correct Google Play account on Device B

**Recommendation:**
- User should switch Google Play account on Device B to match Account 1
- Or backend should provide all account identifiers for the user
- Module supports multiple identifiers, so both accounts can be stored

---

#### 11. Account Identifier Collision (Same Identifier, Different Users)

**Scenario:**
- User A and User B both use the same Google Play account
- Both have purchases from the same account
- Both users store the same account identifier

**Current Behavior:**
- Filtering by `userId` prevents cross-access
- Both users can have the same account identifier in their mappings
- Each user only sees their own purchases (filtered by userId)

**Impact:** ✅ Safe - No cross-user access due to userId filtering

---

#### 12. Backend Rehydration with Multiple Account Identifiers

**Scenario:**
- User has purchases from Account 1 and Account 2
- User reinstalls app
- Backend provides only Account 1 identifier
- User loses access to Account 2 purchases

**Current Behavior:**
- `setAccountIdentifier()` adds identifier to the set (doesn't replace)
- If backend provides multiple identifiers, call `setAccountIdentifier()` for each
- Module supports multiple identifiers per user

**Impact:** ⚠️ User may lose access to some purchases if backend doesn't provide all identifiers

**Recommendation:**
- Backend should return all account identifiers for the user
- Call `setAccountIdentifier()` for each identifier provided by backend
- Or implement a batch method: `setAccountIdentifiers(userId, List<String>)`

---

## Best Practices

1. **Always Pass userId**: Use `SessionManager.userPrincipal` when calling IAP methods
2. **Backend Storage**: Store account identifier on backend when validating purchases server-side
3. **Backend Rehydration**: Return all account identifiers for user during login/authentication
4. **Monitor Logs**: Watch for account identifier change warnings to understand user behavior
5. **Handle Null Cases**: Ensure user is logged in before making purchases
6. **Multiple Identifiers**: Support users with purchases from multiple Google Play accounts

## Implementation Details

### Account Identifier Storage Format

Account identifiers are stored as comma-separated values:
```
key: "iap_account_identifier_{userId}"
value: "account1,account2,account3"
```

### Validation

- `setAccountIdentifier()` validates that the identifier matches actual purchases before storing
- Invalid identifiers are rejected with a warning log
- This prevents storing incorrect mappings from backend

### Multiple Identifiers Support

- Module supports multiple account identifiers per user (one-to-many mapping)
- When checking purchases, all stored identifiers are checked
- New identifiers are added to the set (not overwritten)
- This allows users to maintain access to purchases from multiple Google Play accounts

## Troubleshooting

### User Cannot Access Purchases

1. Check if `userId` is being passed correctly
2. Verify account identifier is stored: Check Preferences for key `iap_account_identifier_{userId}`
3. Check if account identifier matches any purchases: Call `restorePurchases(userId)` and inspect results
4. Verify backend rehydration: Ensure backend provides correct account identifier(s)
5. Check logs for warnings about null account identifiers or validation failures

### Account Identifier Not Stored

1. Check if purchase has account identifier: Inspect `Purchase.accountIdentifier` field
2. Verify user is logged in: Check `SessionManager.userPrincipal` is not null
3. Check logs for warnings about null userId or account identifier
4. Ensure backend stores account identifier when validating purchase server-side

### Multiple Account Identifiers

1. User can have purchases from multiple Google Play accounts
2. All account identifiers are stored and checked
3. User maintains access to purchases from all stored accounts
4. Backend should provide all account identifiers during rehydration

