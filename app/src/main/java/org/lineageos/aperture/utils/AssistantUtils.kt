/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.os.Bundle

object AssistantUtils {
    private val EXTRA_USE_FRONT_CAMERA = listOf(
        "android.intent.extra.USE_FRONT_CAMERA",
        "com.google.assistant.extra.USE_FRONT_CAMERA",
    )

    private val EXTRA_CAMERA_OPEN_ONLY = listOf(
        "android.intent.extra.CAMERA_OPEN_ONLY",
        "com.google.assistant.extra.CAMERA_OPEN_ONLY",
    )

    private val EXTRA_TIMER_DURATION_SECONDS = listOf(
        "android.intent.extra.TIMER_DURATION_SECONDS",
        "com.google.assistant.extra.TIMER_DURATION_SECONDS",
    )

    fun hasUseFrontCamera(extras: Bundle?): Boolean {
        return extras != null && EXTRA_USE_FRONT_CAMERA.any { extras.containsKey(it) }
    }

    fun useFrontCamera(extras: Bundle?): Boolean {
        return extras != null && EXTRA_USE_FRONT_CAMERA.any { extras.getBoolean(it) }
    }

    fun hasCameraOpenOnly(extras: Bundle?): Boolean {
        return extras != null && EXTRA_CAMERA_OPEN_ONLY.any { extras.containsKey(it) }
    }

    fun cameraOpenOnly(extras: Bundle?): Boolean {
        return extras != null && EXTRA_CAMERA_OPEN_ONLY.any { extras.getBoolean(it) }
    }

    fun hasTimerDurationSeconds(extras: Bundle?): Boolean {
        return extras != null && EXTRA_TIMER_DURATION_SECONDS.any { extras.containsKey(it) }
    }

    fun timerDurationSeconds(extras: Bundle?): Int {
        return extras?.getInt(EXTRA_TIMER_DURATION_SECONDS.first { extras.containsKey(it) }) ?: 0
    }
}
