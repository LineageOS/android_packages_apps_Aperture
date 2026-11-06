/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.util.Rational
import androidx.camera.core.AspectRatio
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector

/**
 * A [ResolutionSelector] matching this viewport, if its aspect ratio is a standard one.
 */
val ViewPort.resolutionSelector: ResolutionSelector?
    get() {
        val ratio = when (aspectRatio) {
            Rational(4, 3), Rational(3, 4) -> AspectRatio.RATIO_4_3
            Rational(16, 9), Rational(9, 16) -> AspectRatio.RATIO_16_9
            else -> return null
        }

        return ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(ratio, AspectRatioStrategy.FALLBACK_RULE_AUTO)
            )
            .build()
    }
