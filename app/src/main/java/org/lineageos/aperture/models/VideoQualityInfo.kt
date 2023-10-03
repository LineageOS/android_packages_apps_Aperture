/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

import androidx.camera.video.Quality

/**
 * Video [Quality] info.
 * @param quality The quality
 * @param supportedFrameRates The supported frame rates for this quality
 * @param supportedDynamicRanges The supported dynamic ranges for this quality
 */
data class VideoQualityInfo(
    val quality: Quality,
    val supportedFrameRates: Set<FrameRate>,
    val supportedDynamicRanges: Set<VideoDynamicRange>,
)
