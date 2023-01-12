/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

/**
 * Rotation utils.
 */
enum class Rotation(val degrees: Int) {
    ROTATION_0(0),
    ROTATION_90(90),
    ROTATION_180(180),
    ROTATION_270(270);

    /**
     * Get the rotation needed to compensate for the rotation compared to 0°.
     */
    val compensationValue = 360 - if (degrees > 180) degrees - 360 else degrees

    private val apertureRanges = mutableListOf<IntRange>().apply {
        // Left side
        if (degrees < 45) {
            add(360 - degrees - 45 until 360)
            add(0 until degrees)
        } else {
            add(degrees - 45 until degrees)
        }

        // Right side
        if (degrees > 360 - 45) {
            add(degrees until 360)
            add(0 until 360 - degrees + 45)
        } else {
            add(degrees until degrees + 45)
        }
    }

    companion object {
        /**
         * Get the rotation where the value is in [rotation - 45°, rotation + 45°]
         */
        fun fromDegreesInAperture(degrees: Int) = values().firstOrNull {
            it.apertureRanges.any { range -> degrees in range }
        }
    }
}
