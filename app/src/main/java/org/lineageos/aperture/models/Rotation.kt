/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

import android.view.Surface

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
     * Value will be between [-180°, +180°].
     */
    val compensationValue = 360 - if (offset > 180) offset - 360 else offset

    /**
     * List of ranges that combined represents [offset - 45° .. offset + 45°).
     */
    private val apertureRanges = mutableListOf<IntRange>().apply {
        if (offset < 45) {
            add(315 + offset until 360)
            add(0 until 45 - offset)
        } else if (offset > 315) {
            add(offset - 45 until 360)
            add(0 until offset - 315)
        } else { // offset in 45..315
            add(offset - 45..offset + 45)
        }
    }

    companion object {
        fun fromSurfaceRotation(surfaceRotation: Int) = when (surfaceRotation) {
            Surface.ROTATION_0 -> ROTATION_0
            Surface.ROTATION_90 -> ROTATION_90
            Surface.ROTATION_180 -> ROTATION_180
            Surface.ROTATION_270 -> ROTATION_270
            else -> throw Exception("Unknown surface rotation $surfaceRotation")
        }

        /**
         * Get the rotation where the value is in [rotation - 45°, rotation + 45°]
         */
        fun fromDegreesInAperture(degrees: Int) = toPositiveAngle(degrees).let {
            values().first { rotation ->
                rotation.apertureRanges.any { range -> it in range }
            }
        }

        /**
         * Returns an angle in the range (-360°, 360°) in the same quadrant.
         */
        private fun normalizeAngle(angle: Float) = angle.rem(360f)

        /**
         * Returns the same angle in the range [0°, 360°)
         */
        private fun toPositiveAngle(angle: Int) = angle.mod(360)

        /**
         * Returns the same angle in the range [0°, 360°)
         */
        private fun toPositiveAngle(angle: Float) = angle.mod(360f)

        /**
         * Get the fastest angle in degrees to apply to the current rotation to reach this rotation.
         */
        fun getDifference(currentRotation: Float, targetRotation: Float): Float {
            val diff = normalizeAngle(
                toPositiveAngle(targetRotation) - toPositiveAngle(currentRotation)
            )

            return if (diff > 180f) {
                diff - 360f
            } else if (diff < -180f) {
                diff + 360f
            } else {
                diff
            }
        }
    }
}
