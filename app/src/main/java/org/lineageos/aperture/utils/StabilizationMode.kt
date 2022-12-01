/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

enum class StabilizationMode {
    OFF,
    DIGITAL,
    OPTICAL,
    HYBRID;

    /**
     * Get the closest stabilization to the requested one.
     */
    fun getClosestMode(camera: Camera, cameraMode: CameraMode): StabilizationMode {
        return when {
            camera.supportedStabilizationModes.contains(this) -> this
            this == HYBRID && camera.supportedStabilizationModes.contains(DIGITAL) -> DIGITAL
            this == OPTICAL && cameraMode == CameraMode.VIDEO &&
                    camera.supportedStabilizationModes.contains(DIGITAL) -> DIGITAL
            else -> OFF
        }
    }
}
