/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * App's permissions utils.
 */
class PermissionsUtils(private val context: Context) {
    fun mainPermissionsGranted() = permissionsGranted(mainPermissions)
    fun locationPermissionsGranted() = locationPermissions.any {
        permissionGranted(it)
    }

    private fun permissionGranted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun permissionsGranted(permissions: Array<String>) = permissions.all {
        permissionGranted(it)
    }

    companion object {
        /**
         * Permissions required to run the app
         */
        val mainPermissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()

        /**
         * Permissions needed for location tag in saved photos and videos
         */
        val locationPermissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    }
}
