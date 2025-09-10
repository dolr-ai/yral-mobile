package com.yral.shared.features.feed.sharing

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.decode.BlackholeDecoder
import coil3.request.ImageRequest
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import kotlinx.coroutines.withContext
import java.io.File

interface ShareService {
    suspend fun shareImageWithText(
        imageUrl: String,
        text: String,
    )
}

class AndroidShareService(
    private val context: Context,
    private val appDispatchers: AppDispatchers,
    private val imageLoader: ImageLoader,
) : ShareService {
    override suspend fun shareImageWithText(
        imageUrl: String,
        text: String,
    ) {
        val file = downloadImage(imageUrl)
        withContext(appDispatchers.main) {
            file?.let {
                val chooser = shareIntent(it, text)
                context.startActivity(chooser)
            }
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    private suspend fun downloadImage(imageUrl: String): File? =
        withContext(appDispatchers.network) {
            val sharedDir =
                File(context.cacheDir, "shared").apply {
                    mkdirs()
                }
            val targetFile = File(sharedDir, "shared_image.jpg")
            val cachedFile = imageLoader.cachedImageOrNull(imageUrl)
            if (cachedFile == null) {
                val request =
                    ImageRequest
                        .Builder(context)
                        .data(imageUrl)
                        .decoderFactory(BlackholeDecoder.Factory())
                        .build()
                imageLoader.execute(request)
            }
            imageLoader.cachedImageOrNull(imageUrl)?.let {
                it.copyTo(targetFile, overwrite = true)
                targetFile
            }
        }

    private fun ImageLoader.cachedImageOrNull(imageUrl: String): File? =
        diskCache
            ?.openSnapshot(imageUrl)
            ?.use { it.data.toFile() }

    private fun shareIntent(
        file: File,
        text: String,
    ): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

        val chooser =
            Intent.createChooser(intent, "Share Via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        return chooser
    }
}
