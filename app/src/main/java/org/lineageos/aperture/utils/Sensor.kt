/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.util.Size
import android.util.SizeF
import androidx.annotation.IntRange
import org.lineageos.aperture.reversed
import kotlin.math.atan

/**
 * An interface representing a sensor.
 */
interface Sensor {
    /**
     * The area of the image sensor which corresponds to
     * active pixels after any geometric distortion correction has been applied.
     * Units: Pixel coordinates on the image sensor
     */
    val activeArraySize: Size

    /**
     * List of focal lengths that are supported by this sensor.
     */
    val availableFocalLengths: List<Float>

    /**
     * Clockwise angle through which the output image needs to be rotated
     * to be upright on the device screen in its native orientation.
     */
    val orientation: Int

    /**
     * The pixel count of the full pixel array of the image sensor,
     * which covers android.sensor.info.physicalSize area.
     * This represents the full pixel dimensions of the raw buffers produced by this sensor.
     * Units: Pixels
     */
    val pixelArraySize: Size

    /**
     * The physical dimensions of the full pixel array.
     * This is the physical size of the sensor pixel array defined by
     * android.sensor.info.pixelArraySize.
     * Units: Millimeters
     */
    val size: SizeF

    /**
     * Gets the angle of view of a camera.
     */
    val viewAngleDegrees: Int
        get() = focalLengthToViewAngleDegrees(
            availableFocalLengths.first(),
            horizontalLength
        )

    /**
     * Gets the length of the horizontal side of the sensor.
     *
     * The horizontal side is the width of the sensor size after rotated by the sensor
     * orientation.
     */
    private val horizontalLength: Float
        get() {
            var sensorSize = size
            var activeArraySize = activeArraySize
            var pixelArraySize = pixelArraySize

            if (is90or270(orientation)) {
                sensorSize = sensorSize.reversed()
                activeArraySize = activeArraySize.reversed()
                pixelArraySize = pixelArraySize.reversed()
            }

            return sensorSize.width * activeArraySize.width / pixelArraySize.width
        }

    companion object {
        /**
         * Calculates view angle by focal length and sensor length.
         *
         * The returned view angle is inexact and might not be hundred percent accurate comparing
         * to the output image.
         *
         * The returned view angle should between 0 and 360.
         */
        @IntRange(from = 0, to = 360)
        private fun focalLengthToViewAngleDegrees(focalLength: Float, sensorLength: Float): Int {
            assert(focalLength > 0) { "Focal length should be positive." }
            assert(sensorLength > 0) { "Sensor length should be positive." }

            val viewAngleDegrees = Math.toDegrees(
                2 * atan((sensorLength / (2 * focalLength)).toDouble())
            ).toInt()

            assert(viewAngleDegrees in 0..360) {
                "The provided focal length and sensor length" +
                        "result in an invalid view angle degrees."
            }

            return viewAngleDegrees
        }

        private fun is90or270(rotationDegrees: Int) =
            when (rotationDegrees) {
                0 -> false
                90 -> true
                180 -> false
                270 -> true
                else -> throw IllegalArgumentException("Invalid rotation degrees: $rotationDegrees")
            }
    }
}
