package com.yral.shared.libs.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import co.touchlab.kermit.Logger
import com.yral.shared.core.exceptions.YralException
import com.yral.shared.core.logging.YralLogger
import com.yral.shared.crashlytics.core.CrashlyticsManager
import com.yral.shared.koin.koinInstance
import com.yral.shared.libs.designsystem.theme.YralColors
import com.yral.shared.libs.designsystem.theme.toUiColor
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSURLErrorCancelled
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLRequest
import platform.UIKit.UIApplication
import platform.UIKit.UIView
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Suppress("CyclomaticComplexMethod", "LongMethod")
@Composable
actual fun YralWebView(
    url: String,
    modifier: Modifier,
    maxRetries: Int,
    retryDelayMillis: Long,
) {
    val logger = koinInject<YralLogger>().withTag("YralWebView")
    var isLoading by remember { mutableStateOf(true) }
    var retryCount by remember { mutableStateOf(0) }
    var mainFrameUrl by remember { mutableStateOf(url.trim()) }
    val scope = rememberCoroutineScope()
    val crashlytics = remember { koinInstance.get<CrashlyticsManager>() }
    var webViewRef by remember { mutableStateOf<WKWebView?>(null) }
    var navigationDelegateRef by remember { mutableStateOf<WebViewNavigationDelegate?>(null) }

    fun loadUrlInternal(
        webView: WKWebView,
        targetUrl: String,
        resetRetries: Boolean,
    ) {
        val trimmedUrl = targetUrl.trim()
        if (trimmedUrl.isEmpty()) {
            logger.d { "Ignoring empty URL load request." }
            isLoading = false
            return
        }

        val nsUrl = NSURL(string = trimmedUrl)

        if (resetRetries) {
            retryCount = 0
        }
        mainFrameUrl = trimmedUrl
        isLoading = true
        webView.loadRequest(NSURLRequest(uRL = nsUrl))
    }

    @Suppress("ReturnCount")
    fun handleNavigationError(
        webView: WKWebView,
        error: NSError,
    ) {
        if (error.domain == NSURLErrorDomain && error.code == NSURLErrorCancelled.toLong()) {
            return
        }

        crashlytics.recordException(
            YralException("WebView Error: ${error.localizedDescription}"),
        )

        val targetUrl = mainFrameUrl
        if (targetUrl.isEmpty()) {
            isLoading = false
            return
        }

        if (retryCount >= maxRetries) {
            logger.d { "Max retry attempts reached for $targetUrl" }
            return
        }

        retryCount += 1
        scope.launch {
            delay(retryDelayMillis)
            loadUrlInternal(webView, targetUrl, resetRetries = false)
        }
    }

    @Suppress("ReturnCount")
    fun handleExternalUrl(urlToOpen: NSURL): Boolean {
        val scheme = urlToOpen.scheme?.lowercase()
        if (scheme == null || scheme == "http" || scheme == "https") {
            return false
        }

        val application = UIApplication.sharedApplication
        val canOpen = application.canOpenURL(urlToOpen)
        if (!canOpen) {
            logger.d { "No application found to open url: ${urlToOpen.absoluteString ?: "unknown"}" }
            crashlytics.recordException(
                YralException("No application available to open url: ${urlToOpen.absoluteString ?: "unknown"}"),
            )
            return true
        }

        @Suppress("DEPRECATION")
        val opened = application.openURL(urlToOpen)
        if (!opened) {
            logger.d { "Failed to open url externally: ${urlToOpen.absoluteString ?: "unknown"}" }
            crashlytics.recordException(
                YralException("Failed to open url: ${urlToOpen.absoluteString ?: "unknown"}"),
            )
        }
        return true
    }

    LaunchedEffect(url, webViewRef) {
        val trimmedUrl = url.trim()
        mainFrameUrl = trimmedUrl
        val webView = webViewRef ?: return@LaunchedEffect
        loadUrlInternal(webView, trimmedUrl, resetRetries = true)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        UIKitView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                val container =
                    WebViewContainer(
                        configuration = WKWebViewConfiguration(),
                    )
                val delegate =
                    WebViewNavigationDelegate(
                        onNavigationStarted = { isLoading = true },
                        onNavigationFinished = { isLoading = false },
                        onError = { view, error -> handleNavigationError(view, error) },
                        onMainFrameNavigation = { navigatedUrl ->
                            navigatedUrl?.let { mainFrameUrl = it }
                        },
                        handleExternal = { handleExternalUrl(it) },
                        logger = logger,
                    )
                navigationDelegateRef = delegate
                container.webView.navigationDelegate = delegate
                webViewRef = container.webView
                container
            },
            update = { container ->
                val webView = container.webView
                if (webView.navigationDelegate !== navigationDelegateRef) {
                    navigationDelegateRef?.let { webView.navigationDelegate = it }
                }
                if (webViewRef !== webView) {
                    webViewRef = webView
                }
            },
            onReset = { container ->
                val webView = container.webView
                webView.navigationDelegate = null
                webView.stopLoading()
                if (webViewRef === webView) {
                    webViewRef = null
                }
                navigationDelegateRef = null
            },
            onRelease = { container ->
                val webView = container.webView
                webView.navigationDelegate = null
                webView.stopLoading()
                if (webViewRef === webView) {
                    webViewRef = null
                }
                navigationDelegateRef = null
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

@OptIn(ExperimentalForeignApi::class)
private class WebViewContainer(
    configuration: WKWebViewConfiguration,
) : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    val webView: WKWebView =
        WKWebView(
            frame = bounds,
            configuration = configuration,
        ).apply {
            autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
            backgroundColor = YralColors.Neutral900.toUiColor()
            opaque = false
        }

    init {
        backgroundColor = YralColors.Neutral900.toUiColor()
        addSubview(webView)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        webView.setFrame(bounds)
    }
}

private class WebViewNavigationDelegate(
    private val onNavigationStarted: () -> Unit,
    private val onNavigationFinished: () -> Unit,
    private val onError: (WKWebView, NSError) -> Unit,
    private val onMainFrameNavigation: (String?) -> Unit,
    private val handleExternal: (NSURL) -> Boolean,
    private val logger: Logger,
) : NSObject(),
    WKNavigationDelegateProtocol {
    @ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit,
    ) {
        logger.d { "webView decidePolicyForNavigationAction" }
        val requestUrl = decidePolicyForNavigationAction.request.URL
        val targetFrame = decidePolicyForNavigationAction.targetFrame
        val isMainFrame = targetFrame?.mainFrame != false

        if (requestUrl != null && isMainFrame && handleExternal(requestUrl)) {
            logger.d { "webView decidePolicyForNavigationAction WKNavigationActionPolicyCancel" }
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            return
        }

        if (isMainFrame) {
            logger.d { "webView decidePolicyForNavigationAction isMainFrame" }
            onMainFrameNavigation(requestUrl?.absoluteString)
        }

        decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
        logger.d { "webView decidePolicyForNavigationAction WKNavigationActionPolicyAllow" }
    }

    @ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        didStartProvisionalNavigation: WKNavigation?,
    ) {
        logger.d { "webView didStartProvisionalNavigation" }
        onNavigationStarted()
    }

    @ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        didFinishNavigation: WKNavigation?,
    ) {
        logger.d { "webView didFinishNavigation" }
        onNavigationFinished()
    }

    @ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        didFailProvisionalNavigation: WKNavigation?,
        withError: NSError,
    ) {
        logger.d { "webView didFailProvisionalNavigation" }
        onError(webView, withError)
    }

    @ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        didFailNavigation: WKNavigation?,
        withError: NSError,
    ) {
        logger.d { "webView didFailNavigation" }
        onError(webView, withError)
    }
}

actual fun LazyItemScope.yralWebViewModifier(): Modifier = Modifier.fillParentMaxHeight().fillMaxWidth()
