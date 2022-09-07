/*
 * Copyright (C) 2022 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import com.google.zxing.LuminanceSource
import com.google.zxing.PlanarYUVLuminanceSource
import org.lineageos.aperture.utils.ImageFormatUtils
import java.io.ByteArrayOutputStream

/**
 * Convert the image to a Bitmap object
 * It's gonna miserably fail if the format is ImageFormat.YUV_*
 */
internal val ImageProxy.bitmap: Bitmap
    get() {
        var bytes = byteArray

        if (format in ImageFormatUtils.androidYuvFormats) {
            val out = ByteArrayOutputStream()
            val yuv = YuvImage(bytes, format, width, height, null)
            yuv.compressToJpeg(Rect(0, 0, width, height), 100, out)
            bytes = out.toByteArray()
        }

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            .rotate(imageInfo.rotationDegrees)
    }

/**
 * Convert the image to a byte array
 */
internal val ImageProxy.byteArray: ByteArray
    get() {
        val plane = planes[0]
        val buffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        return bytes
    }

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

/**
 * Convert the image to a ZXing luminance source
 */
internal val ImageProxy.luminanceSource: LuminanceSource
    get() {
        return if (format in ImageFormatUtils.yuvFormats) {
            var bytes = byteArray

            var width = width
            var height = height

            if (imageInfo.rotationDegrees == 90 || imageInfo.rotationDegrees == 270) {
                bytes = rotateYUVLuminancePlane(bytes, width, height)
                width = height.also { height = width }
            }

            PlanarYUVLuminanceSource(
                bytes, width, height, 0, 0, width, height, true
            )
        } else {
            bitmap.luminanceSource
        }
    }
