/*
 * SPDX-FileCopyrightText: 2023-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.annotation.SuppressLint
import android.app.Application
import androidx.camera.camera2.internal.CameraIdUtil
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.MainScope
import org.lineageos.aperture.repositories.CameraRepository
import org.lineageos.aperture.repositories.MediaRepository
import org.lineageos.aperture.repositories.OverlaysRepository
import org.lineageos.aperture.repositories.PreferencesRepository

class ApertureApplication : Application() {
    private val coroutineScope = MainScope()

    val cameraRepository by lazy { CameraRepository(this, coroutineScope, overlaysRepository) }
    val mediaRepository by lazy { MediaRepository(this) }
    val overlaysRepository by lazy { OverlaysRepository(this) }
    val preferencesRepository by lazy { PreferencesRepository(this, coroutineScope) }

    @SuppressLint("RestrictedApi")
    override fun onCreate() {
        super.onCreate()

        // Observe dynamic colors changes
        DynamicColors.applyToActivitiesIfAvailable(this)

        // Set backward compatible camera ids
        CameraIdUtil.setBackwardCompatibleCameraIds(
            overlaysRepository.backwardCompatibleCameraIds.asList()
        )
    }
}
