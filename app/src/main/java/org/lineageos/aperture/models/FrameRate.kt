/*
 * SPDX-FileCopyrightText: 2022-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

enum class FrameRate(val value: Int) {
    FPS_24(24),
    FPS_30(30),
    FPS_60(60),
    FPS_120(120);

    val range = value..value

    companion object {
        fun fromValue(value: Int) = entries.firstOrNull { it.value == value }
        fun fromRange(range: ClosedRange<Int>) = if (range.start == range.endInclusive) {
            fromValue(range.endInclusive)
        } else {
            null
        }
    }
}
