package com.yral.android.ui.widgets.video

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object VideoPermissionUtils {
    fun getRequiredVideoPermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            listOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_VISUAL_USER_SELECTED)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(READ_MEDIA_VIDEO)
        } else {
            listOf(READ_EXTERNAL_STORAGE)
        }

    fun hasVideoPermissions(context: Context): Boolean {
        val permissions = getRequiredVideoPermissions()
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getMissingVideoPermissions(context: Context): List<String> {
        val permissions = getRequiredVideoPermissions()
        return permissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasPermission(
        context: Context,
        permission: String,
    ): Boolean = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun getPermissionDisplayName(permission: String): String =
        when (permission) {
            READ_MEDIA_VIDEO -> "Access to Videos"
            READ_EXTERNAL_STORAGE -> "Access to Storage"
            else -> "Media Access"
        }
}

fun Context.hasVideoPermissions(): Boolean = VideoPermissionUtils.hasVideoPermissions(this)
