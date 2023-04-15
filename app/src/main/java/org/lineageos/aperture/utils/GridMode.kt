/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import org.lineageos.aperture.ext.next

enum class GridMode {
    OFF,
    ON_3,
    ON_4,
    ON_GOLDENRATIO;

    /**
     * Get the next mode.
     */
    fun next() = values().next(this)
}
