/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeUtils {
    fun convertNanosToString(nanos: Long): String {
        return java.lang.String.format(
            Locale.US, "%02d:%02d:%02d",
            TimeUnit.NANOSECONDS.toHours(nanos),
            TimeUnit.NANOSECONDS.toMinutes(nanos) % TimeUnit.HOURS.toMinutes(1),
            TimeUnit.NANOSECONDS.toSeconds(nanos) % TimeUnit.MINUTES.toSeconds(1)
        )
    }
}
