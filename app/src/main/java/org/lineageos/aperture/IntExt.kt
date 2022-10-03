/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.content.res.Resources.getSystem
import kotlin.math.roundToInt

val Int.px
    get() = (this * getSystem().displayMetrics.density).roundToInt()

val Int.dp
    get() = (this / getSystem().displayMetrics.density).roundToInt()
