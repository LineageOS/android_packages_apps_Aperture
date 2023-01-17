/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.os.Bundle

object AssistantUtils {
    private const val EXTRA_USE_FRONT_CAMERA = "android.intent.extra.USE_FRONT_CAMERA"
    private const val EXTRA_USE_FRONT_CAMERA_GOOGLE = "com.google.assistant.extra.USE_FRONT_CAMERA"

    private const val EXTRA_CAMERA_OPEN_ONLY = "android.intent.extra.CAMERA_OPEN_ONLY"
    private const val EXTRA_CAMERA_OPEN_ONLY_GOOGLE = "com.google.assistant.extra.CAMERA_OPEN_ONLY"

    private const val EXTRA_TIMER_DURATION_SECONDS = "android.intent.extra.TIMER_DURATION_SECONDS"
    private const val EXTRA_TIMER_DURATION_SECONDS_GOOGLE =
        "com.google.assistant.extra.TIMER_DURATION_SECONDS"

    fun hasUseFrontCamera(extras: Bundle?): Boolean {
        if (extras == null) return false
        return extras.containsKey(EXTRA_USE_FRONT_CAMERA)
            || extras.containsKey(EXTRA_USE_FRONT_CAMERA_GOOGLE)
    }

    fun useFrontCamera(extras: Bundle?): Boolean {
        if (extras == null) return false
        return extras.getBoolean(EXTRA_USE_FRONT_CAMERA, false)
            || extras.getBoolean(EXTRA_USE_FRONT_CAMERA_GOOGLE, false)
    }

    fun hasCameraOpenOnly(extras: Bundle?): Boolean {
        if (extras == null) return false
        return extras.containsKey(EXTRA_CAMERA_OPEN_ONLY)
            || extras.containsKey(EXTRA_CAMERA_OPEN_ONLY_GOOGLE)
    }

    fun cameraOpenOnly(extras: Bundle?): Boolean {
        if (extras == null) return false
        return extras.getBoolean(EXTRA_CAMERA_OPEN_ONLY, false)
            || extras.getBoolean(EXTRA_CAMERA_OPEN_ONLY_GOOGLE, false)
    }

    fun hasTimerDurationSeconds(extras: Bundle?): Boolean {
        if (extras == null) return false
        return extras.containsKey(EXTRA_TIMER_DURATION_SECONDS)
            || extras.containsKey(EXTRA_TIMER_DURATION_SECONDS_GOOGLE)
    }

    fun timerDurationSeconds(extras: Bundle?): Int {
        if (extras == null) return 0
        return extras.getInt(
            EXTRA_TIMER_DURATION_SECONDS,
            extras.getInt(EXTRA_TIMER_DURATION_SECONDS_GOOGLE, 3)
        )
    }
}
