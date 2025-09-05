/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

import androidx.camera.core.ExposureState
import kotlin.math.roundToInt

/**
 * Exposure compensation information.
 *
 * - `EV`: Exposure value, the user facing value of exposure compensation, as a decimal number.
 * - `index`: Exposure value as an integer.
 * - `step`: Value different between each index.
 *
 * ```
 * EV = index * step
 * ```
 *
 * @param indexRange The exposure compensation index range
 * @param step The exposure compensation step as a fractional number
 */
data class ExposureCompensationInfo(
    val indexRange: IntRange,
    val step: Pair<Int, Int>,
) {
    /**
     * Get the exposure value (EV) from the exposure index.
     */
    fun getExposureValue(index: Int) = (step.first * index).toFloat() / step.second

    /**
     * Get the closest exposure index from the exposure value (EV). The resulting value will be
     * clamped if it is out of range.
     */
    fun getExposureIndex(
        value: Float,
    ) = (value * step.second / step.first).roundToInt().coerceIn(indexRange)

    companion object {
        fun fromExposureState(
            exposureState: ExposureState,
        ) = when (exposureState.isExposureCompensationSupported) {
            true -> ExposureCompensationInfo(
                indexRange = exposureState.exposureCompensationRange.let {
                    it.lower..it.upper
                },
                step = exposureState.exposureCompensationStep.let {
                    it.numerator to it.denominator
                },
            )

            false -> null
        }
    }
}
