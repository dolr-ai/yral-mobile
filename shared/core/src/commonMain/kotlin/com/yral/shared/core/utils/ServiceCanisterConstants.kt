package com.yral.shared.core.utils

/**
 * Placeholder identifier for the legacy "user info service canister".
 *
 * Historically this was an IC canister ID fetched via UniFFI from the Rust layer.
 * With the SpacetimeDB migration, the IC canister no longer exists — all user info
 * is served from SpacetimeDB. This constant is kept as a stable identifier for
 * `CanisterData.canisterId` and route comparisons where a "service canister" id
 * is still expected by existing navigation/analytics code.
 */
const val SERVICE_CANISTER_ID: String = "spacetime-user-info-service"