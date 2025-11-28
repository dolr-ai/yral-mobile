package com.yral.shared.features.profile.ui

/**
 * Checks if storage permission is required for the current platform.
 * On Android Q+ (API 29+), MediaStore API doesn't require WRITE_EXTERNAL_STORAGE permission.
 * On iOS, photo library permission is always required.
 */
expect fun isStoragePermissionRequired(): Boolean
