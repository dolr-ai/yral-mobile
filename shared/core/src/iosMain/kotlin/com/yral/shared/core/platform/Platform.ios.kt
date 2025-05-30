package com.yral.shared.core.platform

import platform.UIKit.UIDevice

actual fun getPlatform(): Platform = IOSPlatform()

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}
