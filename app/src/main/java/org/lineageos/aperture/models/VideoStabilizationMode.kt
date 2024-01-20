/*
 * SPDX-FileCopyrightText: 2023-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

import org.lineageos.aperture.camera.Camera

enum class VideoStabilizationMode {
    OFF,
    ON,
    ON_PREVIEW;

    companion object {
        @androidx.camera.camera2.interop.ExperimentalCamera2Interop
        fun getMode(camera: Camera) = when {
            camera.supportedVideoStabilizationModes.contains(ON_PREVIEW) -> ON_PREVIEW
            camera.supportedVideoStabilizationModes.contains(ON) -> ON
            else -> OFF
        }
    }
}
