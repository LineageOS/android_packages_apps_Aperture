/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.camera

import org.lineageos.aperture.ext.*

enum class CameraMode {
    PHOTO,
    VIDEO,
    QR;

    /**
     * Get the next mode.
     */
    fun next() = values().next(this)

    /**
     * Get the previous mode.
     */
    fun previous() = values().previous(this)
}
