/*
 * SPDX-FileCopyrightText: 2022-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

import androidx.camera.core.DynamicRange

/**
 * Video dynamic range.
 * @param dynamicRange The [DynamicRange] it refers to
 */
enum class VideoDynamicRange(
    val dynamicRange: DynamicRange,
) {
    SDR(DynamicRange.SDR),
    HLG_10_BIT(DynamicRange.HLG_10_BIT),
    HDR10_10_BIT(DynamicRange.HDR10_10_BIT),
    HDR10_PLUS_10_BIT(DynamicRange.HDR10_PLUS_10_BIT),
    DOLBY_VISION_10_BIT(DynamicRange.DOLBY_VISION_10_BIT),
    DOLBY_VISION_8_BIT(DynamicRange.DOLBY_VISION_8_BIT);

    companion object {
        fun fromDynamicRange(dynamicRange: DynamicRange) = entries.first {
            it.dynamicRange == dynamicRange
        }
    }
}
