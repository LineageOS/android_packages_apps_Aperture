/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.camera

enum class NoiseReductionMode {
    OFF,
    FAST,
    HIGH_QUALITY,
    MINIMAL,
    ZERO_SHUTTER_LAG;

    companion object {
        /**
         * We don't want to slow down ZSL, when it's enabled allow
         * only default (the HAL will decide what to do), OFF (zero latency) and
         * ZERO_SHUTTER_LAG.
         */
        val ALLOWED_MODES_ON_ZSL = setOf(
            OFF,
            ZERO_SHUTTER_LAG,
        )

        /**
         * We don't want to drop frames, when it's enabled allow
         * only default (the HAL will decide what to do), OFF (zero latency),
         * FAST (no frame drop guaranteed) and ZERO_SHUTTER_LAG.
         */
        val ALLOWED_MODES_ON_VIDEO_MODE = setOf(
            OFF,
            FAST,
            ZERO_SHUTTER_LAG,
        )
    }
}
