/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import kotlin.math.abs

/**
 * Rotation utils.
 *
 * @property offset The offset added to (360° * k) needed to obtain the wanted rotation.
 */
enum class Rotation(val offset: Int) {
    ROTATION_0(0),
    ROTATION_90(90),
    ROTATION_180(180),
    ROTATION_270(270);

    /**
     * Get the rotation needed to compensate for the rotation compared to 0°.
     */
    val compensationValue = 360 - if (offset > 180) offset - 360 else offset

    private val apertureRanges = mutableListOf<IntRange>().apply {
        // Left side
        if (offset < 45) {
            add(360 - offset - 45 until 360)
            add(0 until offset)
        } else {
            add(offset - 45 until offset)
        }

        // Right side
        if (offset > 360 - 45) {
            add(offset until 360)
            add(0 until 360 - offset + 45)
        } else {
            add(offset until offset + 45)
        }
    }

    companion object {
        /**
         * Get the rotation where the value is in [rotation - 45°, rotation + 45°]
         */
        fun fromDegreesInAperture(degrees: Int) = values().first {
            it.apertureRanges.any { range -> degrees in range }
        }

        /**
         * Get the fastest angle in degrees to apply to the current rotation to reach this rotation.
         */
        fun getDifference(currentRotation: Float, targetRotation: Float): Float {
            val diff = (targetRotation + (360 * (currentRotation / 360).toInt())) - currentRotation

            return if (abs(diff) > 180) {
                diff - 360
            } else {
                diff
            }
        }
    }
}
