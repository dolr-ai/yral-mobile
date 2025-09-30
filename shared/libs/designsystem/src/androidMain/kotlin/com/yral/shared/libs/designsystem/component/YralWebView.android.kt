package com.yral.shared.libs.designsystem.component

import android.content.ActivityNotFoundException
import android.content.Intent
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import co.touchlab.kermit.Logger
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.koin.koinInstance
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
actual fun YralWebView(
    url: String,
    modifier: Modifier,
    maxRetries: Int,
    retryDelayMillis: Long,
) {
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                WebView(it).apply {
                    this.layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    this.webViewClient =
                        object : WebViewClient() {
                            private var retryCount = 0
                            private val crashlytics = koinInstance.get<CrashlyticsManager>()

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): Boolean {
                                val url = request?.url.toString()
                                return handleUrl(url)
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                url: String,
                            ): Boolean = handleUrl(url)

                            private fun handleUrl(url: String): Boolean =
                                if (url.startsWith("http://") || url.startsWith("https://")) {
                                    false
                                } else {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                        context.startActivity(intent)
                                    } catch (e: ActivityNotFoundException) {
                                        Logger.d("No activity found to open url: $url")
                                        crashlytics.recordException(e)
                                    }
                                    true
                                }

                            override fun onPageFinished(
                                view: WebView?,
                                url: String?,
                            ) {
                                super.onPageFinished(view, url)
                                isLoading = false
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?,
                            ) {
                                super.onReceivedError(view, request, error)
                                crashlytics.recordException(
                                    YralException("WebView Error: ${error?.description}"),
                                )
                                handleErrorAndRetry(request)
                            }

                            override fun onReceivedHttpError(
                                view: WebView,
                                request: WebResourceRequest,
                                errorResponse: WebResourceResponse,
                            ) {
                                super.onReceivedHttpError(view, request, errorResponse)
                                crashlytics.recordException(
                                    YralException("WebView Error: ${errorResponse.reasonPhrase}"),
                                )
                                handleErrorAndRetry(request)
                            }

                            private fun handleErrorAndRetry(request: WebResourceRequest?) {
                                // Only handle the main frame error (not favicon, CSS, etc.)
                                if (request?.isForMainFrame == true && retryCount < maxRetries) {
                                    retryCount++
                                    scope.launch {
                                        delay(retryDelayMillis)
                                        loadUrl(url)
                                    }
                                }
                            }
                        }
                }
            },
            update = {
                it.loadUrl(url)
            },
        )
        if (isLoading) {
            Box(
                modifier =
                    Modifier
                        .padding(45.dp)
                        .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                YralLoader()
            }
        }
    }
}
