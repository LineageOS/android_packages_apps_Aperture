/*
 * SPDX-FileCopyrightText: 2023-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.annotation.SuppressLint
import android.app.Application
import androidx.camera.camera2.internal.CameraIdUtil
import com.google.android.material.color.DynamicColors
import org.lineageos.aperture.ext.getStringArray

class ApertureApplication : Application() {
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
