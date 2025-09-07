package com.yral.shared.features.feed.sharing

import android.content.Context
import co.touchlab.kermit.Logger
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.util.ContentMetadata
import io.branch.referral.util.LinkProperties
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BranchLinkGenerator(
    private val context: Context,
) : LinkGenerator {
    override suspend fun generateShareLink(input: LinkInput): String =
        suspendCancellableCoroutine { cont ->
            try {
                val meta = ContentMetadata().also { cm ->
                    // include internal_url for now as a duplicate of android_deeplink_path in case android_deeplink_path does not work
                    cm.addCustomMetadata("internal_url", input.internalUrl)
                    // Include any caller-provided metadata
                    input.metadata.forEach { (k, v) -> cm.addCustomMetadata(k, v) }
                }

                val buo = BranchUniversalObject()
                    .setTitle(input.title ?: "")
                    .setContentDescription(input.description ?: "")
                    .setContentMetadata(meta)

                val linkProps = LinkProperties()
                    .setChannel(input.channel ?: "")
                    .setFeature(input.feature ?: "")
                    .addControlParameter("\$android_deeplink_path", input.internalUrl.substringAfter("://"))

                input.fallbackUrl?.let { linkProps.addControlParameter("\$fallback_url", it) }

                buo.generateShortUrl(context, linkProps) { url, error ->
                    if (error != null) {
                        Logger.Companion.e("BranchSDK") { "Error generating short url: ${error.message}" }
                        // Fail with the error so caller can handle gracefully
                        cont.resumeWithException(IllegalStateException(error.message))
                    } else if (!url.isNullOrBlank()) {
                        cont.resume(url)
                    } else {
                        // Fallback to internal URL if Branch returned null/blank
                        cont.resumeWithException(IllegalStateException("Branch returned null/blank URL"))
                    }
                }
            } catch (t: Throwable) {
                // On any unexpected exception, fallback to internal URL
                cont.resumeWithException(t)
            }
        }
}