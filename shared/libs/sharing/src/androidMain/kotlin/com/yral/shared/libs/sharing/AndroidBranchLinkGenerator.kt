package com.yral.shared.libs.sharing

import android.content.Context
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.util.ContentMetadata
import io.branch.referral.util.LinkProperties
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidBranchLinkGenerator(
    private val context: Context,
) : LinkGenerator {
    override suspend fun generateShareLink(input: LinkInput): String =
        suspendCancellableCoroutine { cont ->
            try {
                val meta =
                    ContentMetadata().also { cm ->
                        // include internal_url for now as a duplicate of android_deeplink_path in
                        // case android_deeplink_path does not work
                        cm.addCustomMetadata("internal_url", input.internalUrl)
                        // Include any caller-provided metadata
                        input.metadata.forEach { (k, v) -> cm.addCustomMetadata(k, v) }
                    }

                val buo =
                    BranchUniversalObject()
                        .setTitle(input.title ?: "")
                        .setContentDescription(input.description ?: "")
                        .setContentImageUrl(input.contentImageUrl ?: "")
                        .setContentMetadata(meta)

                val linkProps =
                    LinkProperties()
                        .setFeature(input.feature ?: "")
                        .addTags(input.tags)
                        .addControlParameter("\$deeplink_path", input.internalUrl.substringAfter("://"))

                buo.generateShortUrl(context, linkProps) { url, error ->
                    if (error != null) {
                        // Fail with the error so caller can handle gracefully
                        cont.resumeWithException(IllegalStateException(error.message))
                    } else if (!url.isNullOrBlank()) {
                        cont.resume(url)
                    } else {
                        // Fallback to internal URL if Branch returned null/blank
                        cont.resumeWithException(IllegalStateException("Branch returned null/blank URL"))
                    }
                }
            } catch (
                @Suppress("TooGenericExceptionCaught") t: Throwable,
            ) {
                // On any unexpected exception, fallback to internal URL
                cont.resumeWithException(t)
            }
        }

    private fun LinkProperties.addTags(tags: List<String>) = tags.fold(this) { lp, tag -> lp.addTag(tag) }
}
