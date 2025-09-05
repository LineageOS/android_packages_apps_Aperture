/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import kotlin.math.pow

fun Float.previousPowerOfTwo(): Float {
    if (this <= 0) return 0f

    val power = kotlin.math.floor(kotlin.math.log2(this.toDouble())).toInt()
    val result = 2.0.pow(power).toFloat()

    return if (result >= this) {
        result / 2
    } else {
        result
    }
}

fun Float.nextPowerOfTwo(): Float {
    if (this <= 0) return 0f

    val power = kotlin.math.ceil(kotlin.math.log2(this.toDouble())).toInt()
    val result = 2.0.pow(power).toFloat()

    return if (result <= this) {
        result * 2
    } else {
        result
    }
}
