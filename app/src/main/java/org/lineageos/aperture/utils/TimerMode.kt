/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import org.lineageos.aperture.next

enum class TimerMode(val seconds: Int) {
    OFF(0),
    ON_3S(3),
    ON_10S(10);

    /**
     * Get the next mode.
     */
    fun next() = values().next(this)

    companion object {
        fun fromSeconds(seconds: Int) = values().firstOrNull { it.seconds == seconds }
    }
}
