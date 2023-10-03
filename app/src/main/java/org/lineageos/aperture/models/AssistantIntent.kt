/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

import android.content.Intent

data class AssistantIntent(
    val useFrontCamera: Boolean? = null,
    val cameraOpenOnly: Boolean? = null,
    val timerDurationSeconds: Int? = null,
) {
    companion object {
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

        fun fromIntent(intent: Intent): AssistantIntent? {
            val extras = intent.extras ?: return null

            val useFrontCamera = EXTRA_USE_FRONT_CAMERA.firstOrNull {
                extras.containsKey(it)
            }?.let { extras.getBoolean(it) }
            val cameraOpenOnly = EXTRA_CAMERA_OPEN_ONLY.firstOrNull {
                extras.containsKey(it)
            }?.let { extras.getBoolean(it) }
            val timerDurationSeconds = EXTRA_TIMER_DURATION_SECONDS.firstOrNull {
                extras.containsKey(it)
            }?.let { extras.getInt(it) }

            if (listOfNotNull(useFrontCamera, cameraOpenOnly, timerDurationSeconds).isEmpty()) {
                return null
            }

            return AssistantIntent(useFrontCamera, cameraOpenOnly, timerDurationSeconds)
        }
    }
}
