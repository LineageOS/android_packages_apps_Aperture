/*
 * Copyright (C) 2022 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.graphics.Bitmap
import android.graphics.Matrix
import com.google.zxing.LuminanceSource
import com.google.zxing.RGBLuminanceSource
import java.io.ByteArrayOutputStream

/**
 * Convert the bitmap to a byte array
 */
internal val Bitmap.byteArray: ByteArray
    get() {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

/**
 * Convert the bitmap to an int array (ARGB)
 */
internal val Bitmap.intArray: IntArray
    get() {
        val intArray = IntArray(width * height)
        getPixels(intArray, 0, width, 0, 0, width, height)
        return intArray
    }

/**
 * Convert the bitmap to a ZXing luminance source
 */
internal val Bitmap.luminanceSource: LuminanceSource
    get() {
        return RGBLuminanceSource(width, height, intArray)
    }

internal fun Bitmap.rotate(degrees: Int): Bitmap {
    if (degrees == 0) {
        return this
    }

    val matrix = Matrix().also {
        it.postRotate(degrees.toFloat())
    }

    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
