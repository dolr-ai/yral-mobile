package com.yral.featureflag

import com.yral.featureflag.core.FlagGroup
import com.yral.featureflag.core.FlagResult
import com.yral.featureflag.core.MutableFeatureFlagProvider
import com.yral.featureflag.providers.FirebaseRemoteConfigProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeProvider(
    override val id: String,
    override val name: String,
    private val map: MutableMap<String, String> = mutableMapOf(),
    override val isRemote: Boolean = false,
) : MutableFeatureFlagProvider {
    override fun getRaw(key: String): FlagResult<String> = map[key]?.let { FlagResult.Sourced(it) } ?: FlagResult.NotSet

    override fun setRaw(
        key: String,
        value: String,
    ) {
        map[key] = value
    }

    override fun clear(key: String) {
        map.remove(key)
    }
}

class FeatureFlagManagerTest {
    @Test
    fun precedence_local_over_remote_over_default() {
        val local = FakeProvider(id = "local_overrides", name = "Local")
        val remote = FakeProvider(id = "remote", name = "Remote", isRemote = true)
        val flag = FlagGroup("group").boolean("flag", "Flag", "desc", defaultValue = false)

        val mgr =
            FeatureFlagManager(
                providersInPriority = listOf(local, remote),
                localProviderId = local.id,
            )

        // default
        assertFalse(mgr.get(flag))

        // remote only
        remote.setRaw(flag.key, "true")
        mgr.clearResolvedCaches()
        assertTrue(mgr.get(flag))

        // local overrides
        mgr.setLocalOverride(flag, false)
        assertFalse(mgr.get(flag))
    }

    @Test
    fun provider_gating_via_local_flag() {
        val local = FakeProvider(id = "local_overrides", name = "Local")
        val remote = FakeProvider(id = "firebase_remote_config", name = "Remote", isRemote = true)
        val toggle = ProviderFlags.FirebaseEnabled
        val flag = FlagGroup("group").boolean("flag", "Flag", "desc", defaultValue = false)

        val mgr =
            FeatureFlagManager(
                providersInPriority = listOf(local, remote),
                localProviderId = local.id,
                providerControls = mapOf(FirebaseRemoteConfigProvider.ID to toggle),
            )

        // Remote provides true but is disabled by default? default is true for toggle, so enabled
        remote.setRaw(flag.key, "true")
        mgr.clearResolvedCaches()
        assertTrue(mgr.get(flag))

        // Disable provider via local override
        mgr.setLocalOverride(toggle, false)
        // Now remote should not be consulted, fallback to default value
        assertFalse(mgr.get(flag))
    }

    @Test
    fun import_export_round_trip() {
        val local = FakeProvider(id = "local_overrides", name = "Local")
        val remote = FakeProvider(id = "remote", name = "Remote", isRemote = true)
        val flagA = FlagGroup("a").string("flag", "A", "desc", defaultValue = "x")
        val flagB = FlagGroup("b").int("flag", "B", "desc", defaultValue = 1)

        val mgr =
            FeatureFlagManager(
                providersInPriority = listOf(local, remote),
                localProviderId = local.id,
            )

        mgr.setLocalOverride(flagA, "hello")
        mgr.setLocalOverride(flagB, 42)

        val exported = mgr.exportLocalOverrides(listOf(flagA, flagB))
        val mgr2 =
            FeatureFlagManager(
                providersInPriority = listOf(local, remote),
                localProviderId = local.id,
            )
        val applied = mgr2.importLocalOverrides(exported, listOf(flagA, flagB))
        assertEquals(2, applied)
        assertEquals("hello", mgr2.get(flagA))
        assertEquals(42, mgr2.get(flagB))
    }
}
