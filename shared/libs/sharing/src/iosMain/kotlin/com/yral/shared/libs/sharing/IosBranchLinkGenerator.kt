package com.yral.shared.libs.sharing

import co.touchlab.kermit.Logger
import cocoapods.BranchSDK.BranchLinkProperties
import cocoapods.BranchSDK.BranchUniversalObject
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.setValue
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosBranchLinkGenerator : LinkGenerator {
    override suspend fun generateShareLink(input: LinkInput): String =
        suspendCancellableCoroutine { cont ->
            try {
                val universalObject =
                    BranchUniversalObject().apply {
                        title = input.title ?: ""
                        contentDescription = input.description ?: ""
                        imageUrl = input.contentImageUrl

                        val customMetadata = contentMetadata.customMetadata
                        customMetadata.setValue(input.internalUrl, forKey = "internal_url")
                        input.metadata.forEach { (key, value) ->
                            customMetadata.setValue(value, forKey = key)
                        }
                    }

                val linkProperties =
                    BranchLinkProperties().apply {
                        feature = input.feature ?: ""
                        if (input.tags.isNotEmpty()) {
                            tags = input.tags
                        }

                        val deeplinkPath = input.internalUrl.substringAfter("://")
                        addControlParam(controlParam = "\$deeplink_path", withValue = deeplinkPath)
                    }

                universalObject.getShortUrlWithLinkProperties(
                    linkProperties = linkProperties,
                    andCallback = { url, error ->
                        if (!cont.isActive) return@getShortUrlWithLinkProperties

                        when {
                            !url.isNullOrBlank() -> {
                                cont.resume(url)
                            }

                            error != null -> {
                                val msg = error.localizedDescription
                                Logger.w("BranchLinkGenerator") { "Branch error, using internalUrl: $msg" }
                                cont.resume(input.internalUrl)
                            }

                            else -> {
                                Logger.w("BranchLinkGenerator") { "Branch returned blank URL, using internalUrl" }
                                cont.resume(input.internalUrl)
                            }
                        }
                    },
                )
            } catch (
                @Suppress("TooGenericExceptionCaught") t: Throwable,
            ) {
                Logger.w("BranchLinkGenerator") { "Unexpected error, using internalUrl: ${t.message}" }
                cont.resume(input.internalUrl)
            }
        }
}
