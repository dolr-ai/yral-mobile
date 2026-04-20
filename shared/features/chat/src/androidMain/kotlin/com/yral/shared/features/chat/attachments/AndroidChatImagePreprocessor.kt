package com.yral.shared.features.chat.attachments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

internal data class AndroidChatImageCompressionConfig(
    val maxDimensionPx: Int = CHAT_IMAGE_MAX_DIMENSION_PX,
    val maxUploadBytes: Long = CHAT_IMAGE_MAX_UPLOAD_BYTES,
    val jpegQualities: List<Int> = CHAT_IMAGE_JPEG_QUALITIES,
)

internal object AndroidChatImagePreprocessor {
    private val defaultConfig = AndroidChatImageCompressionConfig()

    fun persistProcessedImageToCache(
        context: Context,
        uri: Uri,
        cacheDir: File,
        timestampMs: Long = System.currentTimeMillis(),
        config: AndroidChatImageCompressionConfig = defaultConfig,
    ): FilePathChatAttachment {
        val decodedBitmap = decodeBitmapForChatUpload(context, uri, config)
        val orientedBitmap = normalizeBitmapOrientation(context, uri, decodedBitmap)
        if (orientedBitmap !== decodedBitmap) {
            decodedBitmap.recycle()
        }

        val resizedBitmap = resizeBitmapForChatUpload(orientedBitmap, config.maxDimensionPx)
        if (resizedBitmap !== orientedBitmap) {
            orientedBitmap.recycle()
        }

        val encodedBytes = encodeBitmapForChatUpload(resizedBitmap, config)
        resizedBitmap.recycle()

        val metadata = buildPickedChatImageMetadata(timestampMs, ChatPickedImageFormat.JPEG)
        val destinationFile = File(cacheDir, metadata.fileName)
        FileOutputStream(destinationFile).use { output ->
            output.write(encodedBytes)
        }

        return FilePathChatAttachment(
            filePath = destinationFile.absolutePath,
            fileName = metadata.fileName,
            contentType = metadata.contentType,
        )
    }

    internal fun resizeBitmapForChatUpload(
        bitmap: Bitmap,
        maxDimensionPx: Int,
    ): Bitmap {
        val longestEdge = maxOf(bitmap.width, bitmap.height)
        if (longestEdge <= maxDimensionPx) {
            return bitmap
        }

        val scaleRatio = maxDimensionPx.toFloat() / longestEdge.toFloat()
        val targetWidth = (bitmap.width * scaleRatio).roundToInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scaleRatio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    internal fun encodeBitmapForChatUpload(
        bitmap: Bitmap,
        config: AndroidChatImageCompressionConfig = defaultConfig,
    ): ByteArray {
        require(config.jpegQualities.isNotEmpty()) { "JPEG qualities must not be empty" }

        var fallbackBytes: ByteArray? = null
        config.jpegQualities.forEach { quality ->
            val encodedBytes = compressBitmapToJpeg(bitmap, quality)
            fallbackBytes = encodedBytes
            if (encodedBytes.size.toLong() <= config.maxUploadBytes) {
                return encodedBytes
            }
        }

        return requireNotNull(fallbackBytes)
    }

    private fun decodeBitmapForChatUpload(
        context: Context,
        uri: Uri,
        config: AndroidChatImageCompressionConfig,
    ): Bitmap {
        val boundsOptions =
            BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
        context.contentResolver.openInputStream(uri).use { inputStream ->
            requireNotNull(inputStream) { "Unable to open input stream for $uri" }
            BitmapFactory.decodeStream(inputStream, null, boundsOptions)
        }

        require(boundsOptions.outWidth > 0 && boundsOptions.outHeight > 0) {
            "Unable to decode image bounds for $uri"
        }

        val decodeOptions =
            BitmapFactory.Options().apply {
                inSampleSize =
                    calculateInSampleSize(
                        width = boundsOptions.outWidth,
                        height = boundsOptions.outHeight,
                        targetLongestEdge = config.maxDimensionPx * DECODE_SAMPLING_MULTIPLIER,
                    )
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

        return context.contentResolver.openInputStream(uri).use { inputStream ->
            requireNotNull(inputStream) { "Unable to open input stream for $uri" }
            requireNotNull(BitmapFactory.decodeStream(inputStream, null, decodeOptions)) {
                "Unable to decode image for $uri"
            }
        }
    }

    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        targetLongestEdge: Int,
    ): Int {
        var sampleSize = 1
        val longestEdge = maxOf(width, height)
        while (longestEdge / sampleSize > targetLongestEdge) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun normalizeBitmapOrientation(
        context: Context,
        uri: Uri,
        bitmap: Bitmap,
    ): Bitmap {
        val transformationMatrix = buildOrientationMatrix(readExifOrientation(context, uri))

        return if (transformationMatrix == null) {
            bitmap
        } else {
            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                transformationMatrix,
                true,
            )
        }
    }

    private fun buildOrientationMatrix(orientation: Int): Matrix? =
        buildOrientationTransformation(orientation)?.let { transformation ->
            Matrix().apply {
                transformation.rotationDegrees?.let(::postRotate)
                if (transformation.scaleX != FLIP_HORIZONTAL_DEFAULT ||
                    transformation.scaleY != FLIP_VERTICAL_DEFAULT
                ) {
                    postScale(transformation.scaleX, transformation.scaleY)
                }
            }
        }

    private fun readExifOrientation(
        context: Context,
        uri: Uri,
    ): Int =
        runCatching {
            context.contentResolver.openInputStream(uri).use { inputStream ->
                if (inputStream == null) {
                    ExifInterface.ORIENTATION_NORMAL
                } else {
                    ExifInterface(inputStream)
                        .getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL,
                        )
                }
            }
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    private fun compressBitmapToJpeg(
        bitmap: Bitmap,
        quality: Int,
    ): ByteArray =
        ByteArrayOutputStream().use { outputStream ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)) {
                "Unable to compress bitmap at quality=$quality"
            }
            outputStream.toByteArray()
        }
}

internal const val CHAT_IMAGE_MAX_DIMENSION_PX = 1600
internal const val CHAT_IMAGE_MAX_UPLOAD_BYTES = 2L * 1024L * 1024L
internal val CHAT_IMAGE_JPEG_QUALITIES =
    listOf(
        CHAT_IMAGE_JPEG_QUALITY_HIGH,
        CHAT_IMAGE_JPEG_QUALITY_MEDIUM,
        CHAT_IMAGE_JPEG_QUALITY_LOW,
        CHAT_IMAGE_JPEG_QUALITY_MINIMUM,
    )

private const val DECODE_SAMPLING_MULTIPLIER = 2
private const val CHAT_IMAGE_JPEG_QUALITY_HIGH = 85
private const val CHAT_IMAGE_JPEG_QUALITY_MEDIUM = 75
private const val CHAT_IMAGE_JPEG_QUALITY_LOW = 65
private const val CHAT_IMAGE_JPEG_QUALITY_MINIMUM = 55
private const val ROTATION_90_DEGREES = 90f
private const val ROTATION_180_DEGREES = 180f
private const val ROTATION_270_DEGREES = 270f
private const val FLIP_HORIZONTAL = -1f
private const val FLIP_HORIZONTAL_DEFAULT = 1f
private const val FLIP_VERTICAL = -1f
private const val FLIP_VERTICAL_DEFAULT = 1f

private fun buildOrientationTransformation(orientation: Int): OrientationTransformation? {
    val rotationDegrees = orientationToRotationDegrees(orientation)
    val scaleX = orientationToHorizontalScale(orientation)
    val scaleY = orientationToVerticalScale(orientation)

    return if (rotationDegrees == null &&
        scaleX == FLIP_HORIZONTAL_DEFAULT &&
        scaleY == FLIP_VERTICAL_DEFAULT
    ) {
        null
    } else {
        OrientationTransformation(
            rotationDegrees = rotationDegrees,
            scaleX = scaleX,
            scaleY = scaleY,
        )
    }
}

private fun orientationToRotationDegrees(orientation: Int): Float? =
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_180 -> ROTATION_180_DEGREES

        ExifInterface.ORIENTATION_TRANSPOSE,
        ExifInterface.ORIENTATION_ROTATE_90,
        -> ROTATION_90_DEGREES

        ExifInterface.ORIENTATION_TRANSVERSE,
        ExifInterface.ORIENTATION_ROTATE_270,
        -> ROTATION_270_DEGREES

        else -> null
    }

private fun orientationToHorizontalScale(orientation: Int): Float =
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL,
        ExifInterface.ORIENTATION_TRANSPOSE,
        ExifInterface.ORIENTATION_TRANSVERSE,
        -> FLIP_HORIZONTAL

        else -> FLIP_HORIZONTAL_DEFAULT
    }

private fun orientationToVerticalScale(orientation: Int): Float =
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> FLIP_VERTICAL
        else -> FLIP_VERTICAL_DEFAULT
    }

private data class OrientationTransformation(
    val rotationDegrees: Float?,
    val scaleX: Float,
    val scaleY: Float,
)
