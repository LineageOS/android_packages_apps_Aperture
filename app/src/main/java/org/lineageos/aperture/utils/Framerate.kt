/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.util.Range

enum class Framerate(val value: Int) {
    FPS_24(24),
    FPS_30(30),
    FPS_60(60),
    FPS_120(120);

    val range = Range(value, value)

    /**
     * Get the closer framerate to the requested one, first finding a lower one
     * then checking for a higher one if no one exists.
     */
    fun getLowerOrHigher(framerates: List<Framerate>): Framerate? {
        val smaller = framerates.filter { it <= this }.sortedDescending()
        val bigger = framerates.filter { it > this }.sorted()

        return (smaller + bigger).firstOrNull()
    }

    companion object {
        fun fromValue(value: Int) = values().firstOrNull { it.value == value }
        fun fromRange(range: Range<Int>) = if (range.lower == range.upper) {
            fromValue(range.upper)
        } else {
            null
        }
    }
}
