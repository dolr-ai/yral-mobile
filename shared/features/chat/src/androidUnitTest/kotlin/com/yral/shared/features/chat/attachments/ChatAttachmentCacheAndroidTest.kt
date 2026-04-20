package com.yral.shared.features.chat.attachments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ChatAttachmentCacheAndroidTest {
    private lateinit var context: Context

    @BeforeTest
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun `persistUriToChatCache compresses image attachments into bounded jpeg files`() {
        val sourceFile =
            createImageFile(
                fileName = "source_chat_image.png",
                width = 2400,
                height = 1800,
                format = Bitmap.CompressFormat.PNG,
            )

        val attachment = persistUriToChatCache(context, Uri.fromFile(sourceFile))

        assertEquals("image/jpeg", attachment.contentType)
        assertTrue(attachment.fileName.matches(Regex("chat_image_\\d+\\.jpg")))
        assertTrue(File(attachment.filePath).exists())
        assertTrue(File(attachment.filePath).length() <= CHAT_IMAGE_MAX_UPLOAD_BYTES)

        val boundsOptions =
            BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
        BitmapFactory.decodeFile(attachment.filePath, boundsOptions)

        assertTrue(maxOf(boundsOptions.outWidth, boundsOptions.outHeight) <= CHAT_IMAGE_MAX_DIMENSION_PX)
    }

    @Test
    fun `persistUriToChatCache leaves non image attachments unchanged`() {
        val sourceFile = File(context.cacheDir, "chat_attachment.txt").apply { writeText("hello chat") }

        val attachment =
            persistUriToChatCache(
                context = context,
                uri = Uri.fromFile(sourceFile),
                contentTypeOverride = "text/plain",
            )

        assertEquals("text/plain", attachment.contentType)
        assertTrue(attachment.fileName.startsWith("chat_attachment"))
        assertTrue(attachment.fileName.endsWith(".txt"))
        assertContentEquals(sourceFile.readBytes(), File(attachment.filePath).readBytes())
    }

    @Test
    fun `encodeBitmapForChatUpload returns first quality when size limit allows it`() {
        val bitmap = createNoisyBitmap(width = 120, height = 120)
        val config =
            AndroidChatImageCompressionConfig(
                maxUploadBytes = Long.MAX_VALUE,
                jpegQualities = listOf(85, 75, 65, 55),
            )

        val encodedBytes = AndroidChatImagePreprocessor.encodeBitmapForChatUpload(bitmap, config)

        assertContentEquals(compressBitmap(bitmap, quality = 85), encodedBytes)
        bitmap.recycle()
    }

    @Test
    fun `encodeBitmapForChatUpload falls back to lowest quality when none fit`() {
        val bitmap = createNoisyBitmap(width = 120, height = 120)
        val config =
            AndroidChatImageCompressionConfig(
                maxUploadBytes = 1,
                jpegQualities = listOf(85, 75, 65, 55),
            )

        val encodedBytes = AndroidChatImagePreprocessor.encodeBitmapForChatUpload(bitmap, config)

        assertContentEquals(compressBitmap(bitmap, quality = 55), encodedBytes)
        bitmap.recycle()
    }

    @Test
    fun `resizeBitmapForChatUpload does not upscale smaller images`() {
        val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)

        val resizedBitmap =
            AndroidChatImagePreprocessor.resizeBitmapForChatUpload(
                bitmap = bitmap,
                maxDimensionPx = CHAT_IMAGE_MAX_DIMENSION_PX,
            )

        assertEquals(800, resizedBitmap.width)
        assertEquals(600, resizedBitmap.height)
        assertTrue(resizedBitmap === bitmap)
        bitmap.recycle()
    }

    private fun createImageFile(
        fileName: String,
        width: Int,
        height: Int,
        format: Bitmap.CompressFormat,
    ): File {
        val bitmap = createNoisyBitmap(width = width, height = height)
        val file = File(context.cacheDir, fileName)
        file.outputStream().use { output ->
            bitmap.compress(format, 100, output)
        }
        bitmap.recycle()
        return file
    }

    private fun createNoisyBitmap(
        width: Int,
        height: Int,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val random = Random(0)
        val pixels =
            IntArray(width * height) {
                val red = random.nextInt(256)
                val green = random.nextInt(256)
                val blue = random.nextInt(256)
                (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
            }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun compressBitmap(
        bitmap: Bitmap,
        quality: Int,
    ): ByteArray =
        java.io.ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            output.toByteArray()
        }
}
