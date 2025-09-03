package com.yral.shared.features.feed.sharing

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.withContext
import java.io.File

interface ShareService {
    suspend fun shareImageWithText(imageUrl: String, text: String)
}

class AndroidShareService(
    private val context: Context,
    private val httpClient: HttpClient,
    private val appDispatchers: AppDispatchers
) :
    ShareService {
    override suspend fun shareImageWithText(imageUrl: String, text: String) {
        val file = downloadImage(imageUrl)
        withContext(appDispatchers.main) {
            val chooser = shareIntent(file, text)
            context.startActivity(chooser)
        }
    }

    private suspend fun downloadImage(imageUrl: String): File =
        withContext(appDispatchers.network) {
            val sharedDir = File(context.cacheDir, "shared").apply {
                mkdirs()
            }
            val file = File(sharedDir, "shared_image.png")
            httpClient.get(imageUrl).bodyAsChannel().copyAndClose(file.writeChannel())
            file
        }

    private fun shareIntent(file: File, text: String): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share Image").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return chooser
    }
}
