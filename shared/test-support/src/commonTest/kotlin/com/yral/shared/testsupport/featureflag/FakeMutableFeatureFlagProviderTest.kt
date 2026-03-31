package com.yral.shared.testsupport.featureflag

import com.yral.featureflag.core.FlagResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FakeMutableFeatureFlagProviderTest {
    @Test
    fun stores_and_clears_values() {
        val provider = FakeMutableFeatureFlagProvider(id = "test", name = "Test")

        assertIs<FlagResult.NotSet>(provider.getRaw("flag"))

        provider.setRaw("flag", "true")

        assertEquals(FlagResult.Sourced("true"), provider.getRaw("flag"))

        provider.clear("flag")

        assertIs<FlagResult.NotSet>(provider.getRaw("flag"))
    }
}
