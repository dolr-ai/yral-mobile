package com.yral.shared.features.uploadvideo.utils

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.os.Build
import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale

object VideoPermissionUtils {
    fun getRequiredVideoPermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            listOf(READ_MEDIA_VIDEO, READ_MEDIA_VISUAL_USER_SELECTED)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(READ_MEDIA_VIDEO)
        } else {
            listOf(READ_EXTERNAL_STORAGE)
        }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun rememberVideoPermissionsState(onPermissionsResult: (Map<String, Boolean>) -> Unit): MultiplePermissionsState {
        val permissions = getRequiredVideoPermissions()
        return rememberMultiplePermissionsState(
            permissions = permissions,
            onPermissionsResult = onPermissionsResult,
        )
    }

    /**
     * Check if we have sufficient permissions to access videos
     * For Android 14+: Either full access (READ_MEDIA_VIDEO) OR limited access (READ_MEDIA_VISUAL_USER_SELECTED)
     * For Android 13: READ_MEDIA_VIDEO
     * For Android 12 and below: READ_EXTERNAL_STORAGE
     */
    @OptIn(ExperimentalPermissionsApi::class)
    fun hasSufficientVideoPermissions(permissionState: MultiplePermissionsState): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+: Check if we have either full access OR limited access
            val hasFullAccess =
                permissionState.permissions.any {
                    it.permission == READ_MEDIA_VIDEO && it.status.isGranted
                }
            val hasLimitedAccess =
                permissionState.permissions.any {
                    it.permission == READ_MEDIA_VISUAL_USER_SELECTED && it.status.isGranted
                }
            hasFullAccess || hasLimitedAccess
        } else {
            // For older versions, use the standard all permissions granted check
            permissionState.allPermissionsGranted
        }

    /**
     * Check if permissions are permanently denied (user selected "Don't allow" or "Don't ask again")
     * This should NOT trigger for limited access scenarios
     */
    @OptIn(ExperimentalPermissionsApi::class)
    fun arePermissionsPermanentlyDenied(permissionState: MultiplePermissionsState): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // For Android 14+, only consider it permanently denied if BOTH permissions are denied without rationale
            val videoPermission =
                permissionState.permissions.find { it.permission == READ_MEDIA_VIDEO }
            val limitedPermission =
                permissionState.permissions.find { it.permission == READ_MEDIA_VISUAL_USER_SELECTED }

            // If user has limited access, it's not permanently denied
            if (limitedPermission?.status?.isGranted == true) {
                return false
            }

            // Only permanently denied if both permissions are denied and no rationale should be shown
            val videoPermissionDenied =
                videoPermission?.let {
                    !it.status.isGranted && !it.status.shouldShowRationale
                } ?: false

            val limitedPermissionDenied =
                limitedPermission?.let {
                    !it.status.isGranted && !it.status.shouldShowRationale
                } ?: false

            videoPermissionDenied && limitedPermissionDenied
        } else {
            // For older versions, use standard logic
            permissionState.permissions.any {
                !it.status.isGranted && !it.status.shouldShowRationale
            }
        }
    }
}
