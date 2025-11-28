package com.yral.shared.features.profile.ui

import android.os.Build

/**
 * On Android Q+ (API 29+), MediaStore API doesn't require WRITE_EXTERNAL_STORAGE permission.
 * Only Android 9 (API 28) and below require this permission.
 */
actual fun isStoragePermissionRequired(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
