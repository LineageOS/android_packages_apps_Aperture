/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.util.Size
import android.util.SizeF

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

    val mm35AvailableFocalLengths: List<Float>
        get() = size.let { sensorSize ->
            availableFocalLengths.map { getMm35FocalLength(it, sensorSize) }
        }

    companion object {
        fun getMm35FocalLength(focalLength: Float, sensorSize: SizeF): Float {
            return (36.0f / sensorSize.width) * focalLength
        }
    }
}
