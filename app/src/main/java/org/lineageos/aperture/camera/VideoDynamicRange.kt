/*
 * SPDX-FileCopyrightText: 2022-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.camera

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.camera.core.DynamicRange
import org.lineageos.aperture.R

/**
 * Video dynamic range.
 * @param dynamicRange The [DynamicRange] it refers to
 * @param title A string resource used to represent the dynamic range
 * @param icon An icon resource used to represent the dynamic range
 */
enum class VideoDynamicRange(
    val dynamicRange: DynamicRange,
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
) {
    SDR(
        DynamicRange.SDR,
        R.string.video_dynamic_range_sdr,
        R.drawable.ic_hdr_off,
    ),
    HLG_10_BIT(
        DynamicRange.HLG_10_BIT,
        R.string.video_dynamic_range_hlg_10_bit,
        R.drawable.ic_hdr_on,
    ),
    HDR10_10_BIT(
        DynamicRange.HDR10_10_BIT,
        R.string.video_dynamic_range_hdr10_10_bit,
        R.drawable.ic_hdr_on,
    ),
    HDR10_PLUS_10_BIT(
        DynamicRange.HDR10_PLUS_10_BIT,
        R.string.video_dynamic_range_hdr10_plus_10_bit,
        R.drawable.ic_hdr_off,
    ),
    DOLBY_VISION_10_BIT(
        DynamicRange.DOLBY_VISION_10_BIT,
        R.string.video_dynamic_range_dolby_vision_10_bit,
        R.drawable.ic_dolby,
    ),
    DOLBY_VISION_8_BIT(
        DynamicRange.DOLBY_VISION_8_BIT,
        R.string.video_dynamic_range_dolby_vision_8_bit,
        R.drawable.ic_dolby,
    );

    companion object {
        fun fromDynamicRange(dynamicRange: DynamicRange) = values().first {
            it.dynamicRange == dynamicRange
        }
    }
}
