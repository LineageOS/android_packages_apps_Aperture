/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.content.res.Resources.getSystem
import kotlin.math.roundToInt

val Int.px
    get() = (this * getSystem().displayMetrics.density).roundToInt()

val Int.dp
    get() = (this / getSystem().displayMetrics.density).roundToInt()

internal fun Int.Companion.mapToRange(range: ClosedRange<Int>, percentage: Float): Int {
    return (((range.endInclusive - range.start) * percentage) + range.start).roundToInt()
}
