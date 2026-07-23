package com.yral.shared.rust.service.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileUrlRewritesTest {
    // --- uploaded profile pictures ---

    @Test
    fun legacyProfileUrl_isRewrittenToNewBucketFolder() {
        assertEquals(
            expected =
                "https://prakash-yral.hel1.your-objectstorage.com/yral-profile/users/abc-def/profile-123.jpg",
            actual =
                rewriteProfileImageUrl(
                    "https://yral-profile.hel1.your-objectstorage.com/users/abc-def/profile-123.jpg",
                ),
        )
    }

    @Test
    fun currentProfileUrl_passesThroughUnchanged() {
        val current =
            "https://prakash-yral.hel1.your-objectstorage.com/yral-profile/users/abc/profile-9.jpg"
        assertEquals(current, rewriteProfileImageUrl(current))
    }

    @Test
    fun nullProfileUrl_staysNull() {
        assertNull(rewriteProfileImageUrl(null))
    }

    // --- GobGob defaults ---

    @Test
    fun cloudflareGobUrl_isRewrittenToHetzner() {
        assertEquals(
            expected = "https://prakash-yral.hel1.your-objectstorage.com/gobgob/gob.5.png",
            actual = rewriteGobUrl("https://imagedelivery.net/abXI9nS4DYYtyR1yFFtziA/gob.5/public"),
        )
    }

    @Test
    fun cloudflareGobUrl_highIndex_isRewritten() {
        assertEquals(
            expected = "https://prakash-yral.hel1.your-objectstorage.com/gobgob/gob.18557.png",
            actual =
                rewriteGobUrl("https://imagedelivery.net/abXI9nS4DYYtyR1yFFtziA/gob.18557/public"),
        )
    }

    @Test
    fun hetznerGobUrl_passesThroughUnchanged() {
        val current = "https://prakash-yral.hel1.your-objectstorage.com/gobgob/gob.42.png"
        assertEquals(current, rewriteGobUrl(current))
    }

    @Test
    fun uploadedPropicUrl_isNotTreatedAsGob() {
        val uploaded =
            "https://prakash-yral.hel1.your-objectstorage.com/yral-profile/users/abc/profile-1.jpg"
        assertEquals(uploaded, rewriteGobUrl(uploaded))
    }

    @Test
    fun malformedGobUrl_passesThroughUnchanged() {
        val weird = "https://imagedelivery.net/abXI9nS4DYYtyR1yFFtziA/gob.notanumber/public"
        assertEquals(weird, rewriteGobUrl(weird))
    }

    @Test
    fun rewriteGobUrlOrNull_handlesNull() {
        assertNull(rewriteGobUrlOrNull(null))
        assertEquals(
            expected = "https://prakash-yral.hel1.your-objectstorage.com/gobgob/gob.1.png",
            actual =
                rewriteGobUrlOrNull("https://imagedelivery.net/abXI9nS4DYYtyR1yFFtziA/gob.1/public"),
        )
    }
}
