/*
 * Copyright (C) 2022 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.graphics.ImageFormat

object ImageFormatUtils {
    val androidYuvFormats = intArrayOf(
        ImageFormat.NV21,
        ImageFormat.YUY2,
    )

    val yuvFormats = intArrayOf(
        ImageFormat.YUV_420_888,
        ImageFormat.YUV_422_888,
        ImageFormat.YUV_444_888,
    )
}
