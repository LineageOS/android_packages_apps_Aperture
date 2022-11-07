/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import org.lineageos.aperture.next

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
    fun next() = values().next(this)
}
