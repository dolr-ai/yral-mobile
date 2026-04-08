package com.yral.shared.libs.sharing

import android.content.Context
import co.touchlab.kermit.Logger
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.util.ContentMetadata
import io.branch.referral.util.LinkProperties
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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
                    when {
                        !url.isNullOrBlank() -> {
                            cont.resume(url)
                        }

                        error != null -> {
                            Logger.w("BranchLinkGenerator") { "Branch error, using internalUrl: ${error.message}" }
                            cont.resume(input.internalUrl)
                        }

                        else -> {
                            Logger.w("BranchLinkGenerator") { "Branch returned blank URL, using internalUrl" }
                            cont.resume(input.internalUrl)
                        }
                    }
                }
            } catch (
                @Suppress("TooGenericExceptionCaught") t: Throwable,
            ) {
                Logger.w("BranchLinkGenerator") { "Unexpected error, using internalUrl: ${t.message}" }
                cont.resume(input.internalUrl)
            }
        }

    private fun LinkProperties.addTags(tags: List<String>) = tags.fold(this) { lp, tag -> lp.addTag(tag) }
}
