package com.yral.shared.rust.service.utils

/**
 * Profile-image URL rewrites, applied where a URL produced elsewhere (canister field or
 * the Rust/uniffi layer) enters Kotlin. Both point at self-hosted Hetzner instead of the
 * legacy locations. Doing this here avoids changing the shared `yral-common` crate.
 *
 * Anything that doesn't match a legacy pattern passes through untouched.
 */

// --- Uploaded profile pictures: standalone yral-profile bucket -> prakash-yral/yral-profile/
internal const val LEGACY_PROFILE_URL_BASE = "https://yral-profile.hel1.your-objectstorage.com/"
internal const val NEW_PROFILE_URL_BASE =
    "https://prakash-yral.hel1.your-objectstorage.com/yral-profile/"

internal fun rewriteProfileImageUrl(url: String?): String? =
    if (url != null && url.startsWith(LEGACY_PROFILE_URL_BASE)) {
        NEW_PROFILE_URL_BASE + url.removePrefix(LEGACY_PROFILE_URL_BASE)
    } else {
        url
    }

// --- GobGob default avatars: Cloudflare Images -> prakash-yral/gobgob/
// Cloudflare: https://imagedelivery.net/<hash>/gob.<N>/public
// Hetzner:    https://prakash-yral.hel1.your-objectstorage.com/gobgob/gob.<N>.png
// The Rust (yral-common) propic_from_principal still returns the Cloudflare form; we
// translate it here so yral-common stays untouched.
internal const val CF_GOB_URL_PREFIX = "https://imagedelivery.net/abXI9nS4DYYtyR1yFFtziA/gob."
internal const val CF_GOB_URL_SUFFIX = "/public"
internal const val HETZNER_GOB_URL_PREFIX =
    "https://prakash-yral.hel1.your-objectstorage.com/gobgob/gob."

internal fun rewriteGobUrl(url: String): String {
    if (!url.startsWith(CF_GOB_URL_PREFIX)) return url
    val index = url.removePrefix(CF_GOB_URL_PREFIX).removeSuffix(CF_GOB_URL_SUFFIX)
    // Guard against anything unexpected between the prefix and /public.
    if (index.isEmpty() || index.any { !it.isDigit() }) return url
    return "$HETZNER_GOB_URL_PREFIX$index.png"
}

internal fun rewriteGobUrlOrNull(url: String?): String? = url?.let { rewriteGobUrl(it) }
