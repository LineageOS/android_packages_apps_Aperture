/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import androidx.camera.core.ImageProxy
import com.google.zxing.PlanarYUVLuminanceSource

private fun rotateYUVLuminancePlane(data: ByteArray, width: Int, height: Int): ByteArray {
    val yuv = ByteArray(width * height)
    // Rotate the Y luma
    var i = 0
    for (x in 0 until width) {
        for (y in height - 1 downTo 0) {
            yuv[i] = data[y * width + x]
            i++
        }
    }
    return yuv
}

internal val ImageProxy.planarYUVLuminanceSource: PlanarYUVLuminanceSource
    get() {
        val plane = planes[0]
        val buffer = plane.buffer
        var bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        var width = width
        var height = height

        if (imageInfo.rotationDegrees == 90 || imageInfo.rotationDegrees == 270) {
            bytes = rotateYUVLuminancePlane(bytes, width, height)
            width = height.also { height = width }
        }

        return PlanarYUVLuminanceSource(
            bytes, width, height, 0, 0, width, height, true
        )
    }
