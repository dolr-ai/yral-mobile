package com.yral.shared.app

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.yral.shared.app.nav.RootComponent
import com.yral.shared.app.ui.MyApplicationTheme
import com.shortform.video.ui.ShortformVideoDemoScreen
import com.yral.shared.libs.designsystem.theme.LocalAppTopography
import com.yral.shared.libs.designsystem.theme.appTypoGraphy
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIStatusBarAnimation
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController

@OptIn(ExperimentalMaterial3Api::class)
fun rootViewController(rootComponent: RootComponent): UIViewController = RootComposeViewController(rootComponent)

internal object SystemBarsControllerHolder {
    private var lastVisibility: Boolean = true

    var controller: ((Boolean) -> Unit)? = null
        set(value) {
            field = value
            value?.invoke(lastVisibility)
        }

    fun updateVisibility(show: Boolean) {
        lastVisibility = show
        controller?.invoke(show)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private class RootComposeViewController(
    rootComponent: RootComponent,
) : UIViewController(nibName = null, bundle = null) {
    private val composeController =
        ComposeUIViewController {
            CompositionLocalProvider(LocalRippleConfiguration provides null) {
                CompositionLocalProvider(LocalAppTopography provides appTypoGraphy()) {
                    MyApplicationTheme {
                        ShortformVideoDemoScreen()
                    }
                }
            }
        }

    private var statusBarHidden: Boolean = false
    private val visibilityHandler: (Boolean) -> Unit = { visible -> setSystemBarsVisible(visible) }

    @OptIn(ExperimentalForeignApi::class)
    override fun viewDidLoad() {
        super.viewDidLoad()
        SystemBarsControllerHolder.controller = visibilityHandler
        val composeView = composeController.view
        composeView.setFrame(view.bounds)
        composeView.autoresizingMask =
            UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        addChildViewController(composeController)
        view.addSubview(composeView)
        composeController.didMoveToParentViewController(this)
    }

    override fun viewDidDisappear(animated: Boolean) {
        super.viewDidDisappear(animated)
        if ((isBeingDismissed() || isMovingFromParentViewController()) &&
            SystemBarsControllerHolder.controller === visibilityHandler
        ) {
            SystemBarsControllerHolder.controller = null
        }
    }

    private fun setSystemBarsVisible(visible: Boolean) {
        val shouldHide = !visible
        if (statusBarHidden == shouldHide) {
            return
        }
        statusBarHidden = shouldHide
        setNeedsStatusBarAppearanceUpdate()
    }

    override fun prefersStatusBarHidden(): Boolean = statusBarHidden

    @Suppress("MaxLineLength")
    override fun preferredStatusBarUpdateAnimation(): UIStatusBarAnimation = UIStatusBarAnimation.UIStatusBarAnimationFade
}
