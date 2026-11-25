/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.content.res.Configuration

private const val LARGE_SCREEN_MIN_SMALLEST_WIDTH_DP = 600

val Configuration.isLargeScreen: Boolean
    get() = smallestScreenWidthDp >= LARGE_SCREEN_MIN_SMALLEST_WIDTH_DP
