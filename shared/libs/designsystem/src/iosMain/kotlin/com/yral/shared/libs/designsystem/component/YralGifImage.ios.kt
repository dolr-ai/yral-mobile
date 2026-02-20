package com.yral.shared.libs.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFDataRef
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSDictionary
import platform.Foundation.NSString
import platform.Foundation.dataWithBytes
import platform.Foundation.valueForKey
import platform.ImageIO.CGImageSourceCopyPropertiesAtIndex
import platform.ImageIO.CGImageSourceCreateImageAtIndex
import platform.ImageIO.CGImageSourceCreateWithData
import platform.ImageIO.CGImageSourceGetCount
import platform.ImageIO.kCGImagePropertyGIFDelayTime
import platform.ImageIO.kCGImagePropertyGIFDictionary
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIViewContentMode

@Suppress("MagicNumber")
@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun YralGifImageFromBytes(
    bytes: ByteArray,
    modifier: Modifier,
    contentScale: ContentScale,
) {
    val nsData = remember(bytes) { bytes.toNSData() }

    val gifKey = (CFBridgingRelease(kCGImagePropertyGIFDictionary) as NSString).toString()
    val delayKey = (CFBridgingRelease(kCGImagePropertyGIFDelayTime) as NSString).toString()

    UIKitView(
        factory = {
            UIImageView().apply {
                this.contentMode = contentScale.toUIViewContentMode()

                val dataRef = CFBridgingRetain(nsData) as? CFDataRef
                val source = CGImageSourceCreateWithData(dataRef, null)

                if (source != null) {
                    val count = CGImageSourceGetCount(source).toInt()
                    val frames = mutableListOf<UIImage>()
                    var totalDuration = 0.0

                    for (i in 0 until count) {
                        val cgImage = CGImageSourceCreateImageAtIndex(source, i.toULong(), null)
                        if (cgImage != null) {
                            frames.add(UIImage.imageWithCGImage(cgImage))

                            // --- EXTRACT REAL SPEED ---
                            val properties = CGImageSourceCopyPropertiesAtIndex(source, i.toULong(), null)
                            val dict = (CFBridgingRelease(properties) as? NSDictionary)
                            val gifDict = dict?.valueForKey(gifKey) as? NSDictionary
                            val frameDelay = gifDict?.valueForKey(delayKey) as? Double ?: 0.1

                            // iOS treats delays < 0.011 as 0.1 (standard browser behavior)
                            totalDuration += if (frameDelay < 0.011) 0.1 else frameDelay
                        }
                    }

                    this.animationImages = frames
                    this.animationDuration = totalDuration
                    this.startAnimating()
                }
                CFBridgingRelease(dataRef)
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), length = size.toULong())
    }

private fun ContentScale.toUIViewContentMode(): UIViewContentMode =
    when (this) {
        ContentScale.Fit -> UIViewContentMode.UIViewContentModeScaleAspectFit
        ContentScale.Crop -> UIViewContentMode.UIViewContentModeScaleAspectFill
        else -> UIViewContentMode.UIViewContentModeScaleToFill
    }
