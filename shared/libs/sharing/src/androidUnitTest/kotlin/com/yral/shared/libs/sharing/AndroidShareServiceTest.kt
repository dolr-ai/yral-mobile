package com.yral.shared.libs.sharing

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import coil3.ImageLoader
import coil3.disk.DiskCache
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okio.Path.Companion.toPath
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AndroidShareServiceTest {
    private val application: Application = RuntimeEnvironment.getApplication()
    private val imageLoader: ImageLoader = mockk(relaxed = true)
    private lateinit var service: AndroidShareService

    @Before
    fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        service = AndroidShareService(application, AppDispatchers(), imageLoader)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun shareImageWithTextFallsBackToTextOnlyShareWhenImageIsNotInCache() =
        runBlocking {
            every { imageLoader.diskCache } returns null

            service.shareImageWithText(
                imageUrl = "https://example.com/avatar.jpg",
                text = "Check out this profile!",
            )

            val started = shadowOf(application).nextStartedActivity
            assertNotNull(started, "Expected startActivity to be called")
            @Suppress("DEPRECATION")
            val inner = started.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
            assertEquals("text/plain", inner?.type)
            assertEquals("Check out this profile!", inner?.getStringExtra(Intent.EXTRA_TEXT))
        }

    @Test
    fun shareImageWithTextSharesImageWhenFoundInDiskCache() =
        runBlocking {
            mockkStatic(FileProvider::class)
            try {
                val fakeImageFile =
                    File(application.cacheDir, "test_avatar.jpg").also { it.writeText("fake") }
                val fakeUri =
                    android.net.Uri.parse("content://com.yral.android.fileprovider/image")

                val snapshot = mockk<DiskCache.Snapshot>()
                every { snapshot.data } returns fakeImageFile.absolutePath.toPath()
                every { snapshot.close() } just Runs

                val diskCache = mockk<DiskCache>()
                every { diskCache.openSnapshot(any()) } returns snapshot
                every { imageLoader.diskCache } returns diskCache

                every { FileProvider.getUriForFile(any(), any(), any()) } returns fakeUri

                service.shareImageWithText(
                    imageUrl = "https://example.com/avatar.jpg",
                    text = "Check out this profile!",
                )

                val started = shadowOf(application).nextStartedActivity
                assertNotNull(started, "Expected startActivity to be called")
                @Suppress("DEPRECATION")
                val inner = started.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                assertEquals("image/*", inner?.type)
                assertEquals("Check out this profile!", inner?.getStringExtra(Intent.EXTRA_TEXT))
            } finally {
                unmockkStatic(FileProvider::class)
            }
        }

    @Test
    fun shareTextSendsTextOnlyActionSendIntent() =
        runBlocking {
            service.shareText("Hello from YRAL!")

            val started = shadowOf(application).nextStartedActivity
            assertNotNull(started, "Expected startActivity to be called")
            @Suppress("DEPRECATION")
            val inner = started.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
            assertEquals("text/plain", inner?.type)
            assertEquals("Hello from YRAL!", inner?.getStringExtra(Intent.EXTRA_TEXT))
        }
}
