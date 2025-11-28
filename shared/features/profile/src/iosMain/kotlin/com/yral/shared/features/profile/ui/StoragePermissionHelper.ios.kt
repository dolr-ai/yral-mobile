package com.yral.shared.features.profile.ui

/**
 * On iOS, photo library permission is always required to save files to the photo library.
 */
actual fun isStoragePermissionRequired(): Boolean = true
