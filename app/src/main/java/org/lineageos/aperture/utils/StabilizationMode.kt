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
        if (camera.supportedStabilizationModes.contains(this)) {
            return this
        }

        return when (this) {
            HYBRID -> {
                if (camera.supportedStabilizationModes.contains(DIGITAL)) {
                    DIGITAL
                } else {
                    OFF
                }
            }
            OPTICAL -> {
                if (
                    cameraMode == CameraMode.VIDEO &&
                    camera.supportedStabilizationModes.contains(DIGITAL)
                ) {
                    DIGITAL
                } else {
                    OFF
                }
            }
            else -> OFF
        }
    }
}
