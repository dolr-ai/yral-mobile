package com.yral.shared.libs.sharing

import android.app.Application
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.Branch
import io.branch.referral.BranchError
import io.branch.referral.util.LinkProperties
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AndroidBranchLinkGeneratorTest {
    private val application: Application = RuntimeEnvironment.getApplication()
    private lateinit var generator: AndroidBranchLinkGenerator

    private val testInput =
        LinkInput(
            internalUrl = "yral://post/details/canister1/42/principal1",
            title = "Test Post",
            description = "A test post",
            feature = "share",
            contentImageUrl = "https://example.com/thumb.jpg",
        )

    @Before
    fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        generator = AndroidBranchLinkGenerator(application)
        mockkConstructor(BranchUniversalObject::class)
        mockkConstructor(LinkProperties::class)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkConstructor(BranchUniversalObject::class)
        unmockkConstructor(LinkProperties::class)
    }

    @Test
    fun generateShareLinkReturnsBranchUrlOnSuccess() =
        runBlocking {
            every {
                anyConstructed<BranchUniversalObject>().generateShortUrl(
                    any(),
                    any<LinkProperties>(),
                    any<Branch.BranchLinkCreateListener>(),
                )
            } answers {
                arg<Branch.BranchLinkCreateListener>(2).onLinkCreate("https://yral.app.link/abc123", null)
            }

            val result = generator.generateShareLink(testInput)

            assertEquals("https://yral.app.link/abc123", result)
        }

    @Test
    fun generateShareLinkReturnsInternalUrlWhenBranchReturnsError() =
        runBlocking {
            every {
                anyConstructed<BranchUniversalObject>().generateShortUrl(
                    any(),
                    any<LinkProperties>(),
                    any<Branch.BranchLinkCreateListener>(),
                )
            } answers {
                val error = BranchError("Test error", BranchError.ERR_BRANCH_NO_CONNECTIVITY)
                arg<Branch.BranchLinkCreateListener>(2).onLinkCreate(null, error)
            }

            val result = generator.generateShareLink(testInput)

            assertEquals(testInput.internalUrl, result)
        }

    @Test
    fun generateShareLinkReturnsInternalUrlWhenBranchReturnsBlankUrl() =
        runBlocking {
            every {
                anyConstructed<BranchUniversalObject>().generateShortUrl(
                    any(),
                    any<LinkProperties>(),
                    any<Branch.BranchLinkCreateListener>(),
                )
            } answers {
                arg<Branch.BranchLinkCreateListener>(2).onLinkCreate("", null)
            }

            val result = generator.generateShareLink(testInput)

            assertEquals(testInput.internalUrl, result)
        }

    @Test
    fun generateShareLinkReturnsInternalUrlWhenBranchCallbackInvokesNullUrl() =
        runBlocking {
            every {
                anyConstructed<BranchUniversalObject>().generateShortUrl(
                    any(),
                    any<LinkProperties>(),
                    any<Branch.BranchLinkCreateListener>(),
                )
            } answers {
                arg<Branch.BranchLinkCreateListener>(2).onLinkCreate(null, null)
            }

            val result = generator.generateShareLink(testInput)

            assertEquals(testInput.internalUrl, result)
        }

    @Test
    fun generateShareLinkReturnsInternalUrlWhenBranchThrowsUnexpectedException() =
        runBlocking {
            every {
                anyConstructed<BranchUniversalObject>().generateShortUrl(
                    any(),
                    any<LinkProperties>(),
                    any<Branch.BranchLinkCreateListener>(),
                )
            } throws RuntimeException("SDK not initialized")

            val result = generator.generateShareLink(testInput)

            assertEquals(testInput.internalUrl, result)
        }
}
