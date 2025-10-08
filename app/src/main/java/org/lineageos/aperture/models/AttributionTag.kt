/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

import org.lineageos.aperture.CameraActivity

/**
 * Attribution tags. Must be declared in the app's manifest.
 */
enum class AttributionTag(val tag: String) {
    /**
     * Default tag used by [CameraActivity]
     */
    CAMERA_ACTIVITY("cameraActivity"),

    /**
     * Tag used for getting the location of camera captures.
     */
    CAPTURE_LOCATION("captureLocation"),
}
