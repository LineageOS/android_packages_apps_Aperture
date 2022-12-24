/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import androidx.camera.core.CameraSelector

enum class CameraFacing {
    UNKNOWN,
    FRONT,
    BACK,
    EXTERNAL;

    companion object {
        @androidx.camera.core.ExperimentalLensFacing
        fun fromCameraX(@CameraSelector.LensFacing lensFacing: Int) =
            when (lensFacing) {
                CameraSelector.LENS_FACING_FRONT -> FRONT
                CameraSelector.LENS_FACING_BACK -> BACK
                CameraSelector.LENS_FACING_EXTERNAL -> EXTERNAL
                CameraSelector.LENS_FACING_UNKNOWN -> UNKNOWN
                else -> throw Exception("Unknown lens facing value")
            }
    }
}
