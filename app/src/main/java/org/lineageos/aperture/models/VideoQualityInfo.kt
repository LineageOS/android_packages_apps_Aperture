/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

import androidx.camera.video.Quality

/**
 * Video [Quality] info.
 * @param quality The quality
 * @param supportedFrameRates The supported frame rates for this quality
 * @param supportedDynamicRanges The supported dynamic ranges for this quality
 * @param supportedHighSpeedFrameRates The frame rates only reachable with a high speed session
 */
data class VideoQualityInfo(
    val quality: Quality,
    val supportedFrameRates: Set<FrameRate>,
    val supportedDynamicRanges: Set<VideoDynamicRange>,
    val supportedHighSpeedFrameRates: Set<FrameRate>,
) {
    val supportsSlowMotion = supportedHighSpeedFrameRates.isNotEmpty()

    fun frameRatesFor(slowMotion: Boolean) =
        if (slowMotion) supportedHighSpeedFrameRates else supportedFrameRates
}
