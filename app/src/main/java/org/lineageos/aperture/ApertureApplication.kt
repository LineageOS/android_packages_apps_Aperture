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
import org.lineageos.aperture.ext.getStringArray
import org.lineageos.aperture.repository.PreferencesRepository

class ApertureApplication : Application() {
    private val coroutineScope = MainScope()

    val preferencesRepository by lazy {
        PreferencesRepository(this)
    }

    @SuppressLint("RestrictedApi")
    override fun onCreate() {
        super.onCreate()

        // Observe dynamic colors changes
        DynamicColors.applyToActivitiesIfAvailable(this)

        // Set backward compatible camera ids
        CameraIdUtil.setBackwardCompatibleCameraIds(
            resources.getStringArray(this, R.array.config_backwardCompatibleCameraIds).asList()
        )
    }
}
