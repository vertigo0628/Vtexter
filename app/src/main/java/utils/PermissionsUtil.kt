package com.university.vtexter.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionsUtil {

    // Camera permission
    val CAMERA_PERMISSION = Manifest.permission.CAMERA

    // Storage permissions (different for Android 13+)
    val STORAGE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    // Audio permissions
    val AUDIO_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.MODIFY_AUDIO_SETTINGS
    )

    // Call permissions
    val CALL_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.MODIFY_AUDIO_SETTINGS
    )

    // Contact permissions
    val CONTACTS_PERMISSION = Manifest.permission.READ_CONTACTS

    // Notification permission (Android 13+)
    val NOTIFICATION_PERMISSION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        null
    }

    /**
     * Check if permission is granted
     */
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if all permissions are granted
     */
    fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        return permissions.all { hasPermission(context, it) }
    }

    /**
     * Check camera permission
     */
    fun hasCameraPermission(context: Context): Boolean {
        return hasPermission(context, CAMERA_PERMISSION)
    }

    /**
     * Check storage permissions
     */
    fun hasStoragePermission(context: Context): Boolean {
        return hasPermissions(context, STORAGE_PERMISSIONS)
    }

    /**
     * Check audio permissions
     */
    fun hasAudioPermission(context: Context): Boolean {
        return hasPermissions(context, AUDIO_PERMISSIONS)
    }

    /**
     * Check call permissions
     */
    fun hasCallPermissions(context: Context): Boolean {
        return hasPermissions(context, CALL_PERMISSIONS)
    }

    /**
     * Check notification permission
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (NOTIFICATION_PERMISSION != null) {
            hasPermission(context, NOTIFICATION_PERMISSION)
        } else {
            true // Not needed for older Android versions
        }
    }
}