/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import org.lineageos.aperture.ext.permissionsGranted
import org.lineageos.aperture.ext.permissionsGrantedFlow
import org.lineageos.aperture.models.Permission
import org.lineageos.aperture.models.PermissionState

class PermissionsManager(
    private val activity: ComponentActivity,
) {
    // Each permission needs a PermissionsChecker
    private val permissionsCheckers = Permission.entries.associateWith {
        PermissionsChecker(activity, it.toAndroidPermissions())
    }

    fun permissionState(
        permission: Permission
    ) = when (activity.permissionsGranted(permission.toAndroidPermissions())) {
        true -> PermissionState.GRANTED
        false -> PermissionState.NOT_GRANTED
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun permissionStateFlow(
        permission: Permission
    ) = activity.permissionsGrantedFlow(activity.lifecycle, permission.toAndroidPermissions())
        .mapLatest {
            when (it) {
                true -> PermissionState.GRANTED
                false -> PermissionState.NOT_GRANTED
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun withPermissionGranted(
        permission: Permission, block: suspend () -> Unit,
    ) = permissionStateFlow(permission)
        .distinctUntilChanged()
        .collectLatest { permissionState ->
            when (permissionState) {
                PermissionState.GRANTED -> block()
                else -> Unit
            }
        }

    suspend fun requestPermission(
        permission: Permission
    ) = when (permission.permissionsChecker.requestPermissions()) {
        true -> PermissionState.GRANTED
        false -> PermissionState.NOT_GRANTED
    }

    private val Permission.permissionsChecker: PermissionsChecker
        get() = permissionsCheckers[this] ?: error(
            "No PermissionsChecker for permission ${this.name}"
        )

    companion object {
        /**
         * Permissions needed to use the camera and store the captured media.
         */
        private val cameraPermissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()

        /**
         * Permissions needed for location tag in saved photos and videos
         */
        private val locationPermissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

        private fun Permission.toAndroidPermissions() = when (this) {
            Permission.CAMERA -> cameraPermissions
            Permission.LOCATION -> locationPermissions
        }
    }
}
