package com.yral.shared.libs.sharing

import cocoapods.BranchSDK.BranchLinkProperties
import cocoapods.BranchSDK.BranchUniversalObject
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.setValue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
                            error != null -> {
                                val message = error.localizedDescription
                                cont.resumeWithException(IllegalStateException(message))
                            }

                            !url.isNullOrBlank() -> cont.resume(url)
                            else ->
                                cont.resumeWithException(
                                    IllegalStateException("Branch returned null/blank URL"),
                                )
                        }
                    },
                )
            } catch (
                @Suppress("TooGenericExceptionCaught") t: Throwable,
            ) {
                cont.resumeWithException(t)
            }
        }
}
