/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import androidx.exifinterface.media.ExifInterface
import java.io.InputStream

class ExifUtils {
    companion object {
        fun getOrientation(inputStream: InputStream): Int {
            inputStream.mark(Int.MAX_VALUE)
            val orientation =
                ExifInterface(inputStream).getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
            inputStream.reset()
            return orientation % 360
        }
    }
}
