package com.yral.featureflag

import com.yral.featureflag.core.BasicFeatureFlag
import com.yral.featureflag.core.BooleanCodec
import com.yral.featureflag.core.FlagCodec
import com.yral.featureflag.core.FlagGroup
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeatureFlagsValidatorTest {
    @Test
    fun `validate passes for unique keys and sane defaults`() {
        val group = FlagGroup("test")
        val a = group.boolean("a", "A", "desc", false)
        val b = group.int("b", "B", "desc", 1)

        val result = FeatureFlagsValidator.validate(group.allFlags)
        assertTrue(result.isValid, "Expected valid flags, got errors: ${result.errors}")
    }

    @Test
    fun `duplicate keys are reported`() {
        val a = BasicFeatureFlag("dup.key", "A", "desc", false, codec = BooleanCodec)
        val b = BasicFeatureFlag("dup.key", "B", "desc", true, codec = BooleanCodec)

        val result = FeatureFlagsValidator.validate(listOf(a, b))
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it is ValidationError.DuplicateKey && it.key == "dup.key" })
    }

    @Test
    fun `codec round trip failure is reported`() {
        val badCodec =
            object : FlagCodec<Int> {
                override fun encode(value: Int): String = value.toString()
                override fun decode(raw: String): Int? = null // always fails
            }
        val bad = BasicFeatureFlag(key = "bad.codec", name = "Bad", description = "desc", defaultValue = 1, codec = badCodec)
        val result = FeatureFlagsValidator.validate(listOf(bad))
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it is ValidationError.CodecRoundTripFailed && it.key == "bad.codec" })
    }

    @Test
    fun `prefix policy is enforced when configured`() {
        val a = BasicFeatureFlag("onboarding.flag", "A", "desc", false, codec = BooleanCodec)
        val b = BasicFeatureFlag("profile.flag", "B", "desc", true, codec = BooleanCodec)
        val c = BasicFeatureFlag("misc", "C", "desc", true, codec = BooleanCodec)

        val result = FeatureFlagsValidator.validate(listOf(a, b, c), allowedPrefixes = listOf("onboarding.", "profile."))
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it is ValidationError.PrefixPolicyViolation && it.key == "misc" })
        assertTrue(result.errors.none { it is ValidationError.PrefixPolicyViolation && it.key == "onboarding.flag" })
    }
}
