package com.yral.shared.libs.sharing

import com.yral.shared.libs.coroutines.x.dispatchers.AppDispatchers
import kotlinx.coroutines.withContext
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UINavigationController
import platform.UIKit.UITabBarController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

class IosShareService(
    private val appDispatchers: AppDispatchers,
) : ShareService {
    override suspend fun shareImageWithText(
        imageUrl: String,
        text: String,
    ) {
        shareText(text)
    }

    private suspend fun shareText(text: String) =
        withContext(appDispatchers.main) {
            val rootController = findRootViewController()
            if (rootController == null) {
                return@withContext
            }

            val shareController =
                UIActivityViewController(
                    activityItems = listOf(text),
                    applicationActivities = null,
                )

            rootController.presentViewController(
                viewControllerToPresent = shareController,
                animated = true,
                completion = null,
            )
        }
}

@Suppress("ReturnCount")
private fun findRootViewController(): UIViewController? {
    val application = UIApplication.sharedApplication

    application.windows.let { windows ->
        for (windowObj in windows) {
            val window = windowObj as? UIWindow ?: continue
            if (window.isKeyWindow()) {
                return topViewController(window.rootViewController)
            }
        }
    }

    application.keyWindow?.let { keyWindow ->
        return topViewController(keyWindow.rootViewController)
    }

    val connectedScenes = application.connectedScenes
    for (sceneObj in connectedScenes) {
        val windowScene = sceneObj as? UIWindowScene ?: continue
        val windows = windowScene.windows
        for (windowObj in windows) {
            val window = windowObj as? UIWindow ?: continue
            if (window.isKeyWindow()) {
                return topViewController(window.rootViewController)
            }
        }
    }

    return null
}

private fun topViewController(controller: UIViewController?): UIViewController? {
    val presented = controller?.presentedViewController
    return when {
        controller == null -> null
        presented != null -> topViewController(presented)
        controller is UINavigationController -> topViewController(controller.visibleViewController)
        controller is UITabBarController -> topViewController(controller.selectedViewController)
        else -> controller
    }
}
