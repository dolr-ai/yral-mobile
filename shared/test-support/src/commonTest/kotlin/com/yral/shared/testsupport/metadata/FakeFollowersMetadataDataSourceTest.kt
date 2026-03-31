package com.yral.shared.testsupport.metadata

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FakeFollowersMetadataDataSourceTest {
    @Test
    fun returns_configured_usernames() =
        runTest {
            val dataSource =
                FakeFollowersMetadataDataSource(
                    usernamesByPrincipal = mapOf("principal-1" to "alice"),
                )

            assertEquals(mapOf("principal-1" to "alice"), dataSource.fetchUsernames(listOf("principal-1")))
        }

    @Test
    fun throws_when_configured() =
        runTest {
            val dataSource = FakeFollowersMetadataDataSource(shouldThrow = true)

            assertFailsWith<IllegalStateException> {
                dataSource.fetchUsernames(listOf("principal-1"))
            }
        }
}
