/*
 * Copyright (C) 2022 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

enum class FlashMode {
    /**
     * Flash will not be fired.
     */
    OFF,

    /**
     * Flash will be fired automatically when required
     */
    AUTO,

    /**
     * Flash will always be fired during snapshot.
     */
    ON,

    /**
     * Flash will be fired in red-eye reduction mode.
     * Currently not supported by CameraX.
     */
    // RED_EYE,

    /**
     * Constant emission of light during preview, auto-focus and snapshot.
     */
    TORCH;

    /**
     * Get the next mode.
     */
    fun next(): FlashMode {
        val allModes = values()
        return allModes.getOrElse(allModes.indexOf(this) + 1) { allModes.first() }
    }
}
