/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream

class ExifUtils {
    data class Transform(val rotation: Rotation, val mirror: Boolean) {
        fun toMatrix(): Matrix {
            return Matrix().apply {
                if (mirror) {
                    postScale(-1f, 1f)
                }
                postRotate(rotation.offset.toFloat())
            }
        }

        companion object {
            val DEFAULT = Transform(Rotation.ROTATION_0, false)
        }
    }

    companion object {
        private val orientationMap = mapOf(
            ExifInterface.ORIENTATION_UNDEFINED to Transform.DEFAULT,
            ExifInterface.ORIENTATION_NORMAL to Transform.DEFAULT,
            ExifInterface.ORIENTATION_ROTATE_90 to Transform(Rotation.ROTATION_90, false),
            ExifInterface.ORIENTATION_ROTATE_180 to Transform(Rotation.ROTATION_180, false),
            ExifInterface.ORIENTATION_ROTATE_270 to Transform(Rotation.ROTATION_270, false),
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL to Transform(Rotation.ROTATION_0, true),
            ExifInterface.ORIENTATION_FLIP_VERTICAL to Transform(Rotation.ROTATION_180, true),
            ExifInterface.ORIENTATION_TRANSPOSE to Transform(Rotation.ROTATION_270, true),
            ExifInterface.ORIENTATION_TRANSVERSE to Transform(Rotation.ROTATION_90, true),
        )

        private fun getOrientation(inputStream: InputStream): Int {
            inputStream.mark(Int.MAX_VALUE)
            val orientation =
                ExifInterface(inputStream).getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
            inputStream.reset()
            return orientation
        }

        private fun orientationToTransform(exifOrientation: Int): Transform {
            return orientationMap.getOrDefault(exifOrientation, Transform.DEFAULT)
        }

        fun getTransform(inputStream: InputStream): Transform {
            return orientationToTransform(getOrientation(inputStream))
        }
    }
}
