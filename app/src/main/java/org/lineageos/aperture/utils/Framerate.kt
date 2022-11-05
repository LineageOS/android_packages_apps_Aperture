/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.util.Range

enum class Framerate(val value: Int) {
    FPS_AUTO(-1),
    FPS_24(24),
    FPS_30(30),
    FPS_60(60);

    val range = if (value == -1) null else Range(value, value)

    companion object {
        fun fromValue(value: Int) = values().firstOrNull { it.value == value }
        fun fromRange(range: Range<Int>) = if (range.lower == range.upper) {
            fromValue(range.upper)
        } else {
            null
        }
    }
}
