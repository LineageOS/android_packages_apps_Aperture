/*
 * SPDX-FileCopyrightText: 2022-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.content.res.Resources
import android.media.MediaActionSound
import android.os.Build
import org.lineageos.aperture.repositories.PreferencesRepository

class CameraSoundsUtils(private val preferencesRepository: PreferencesRepository) {
    private val mediaActionSound = MediaActionSound().apply {
        // Preload all sounds to reduce latency
        load(MediaActionSound.SHUTTER_CLICK)
        load(MediaActionSound.START_VIDEO_RECORDING)
        load(MediaActionSound.STOP_VIDEO_RECORDING)
    }

    fun playShutterClick() {
        if (preferencesRepository.shutterSound.value || mustPlaySounds) {
            mediaActionSound.play(MediaActionSound.SHUTTER_CLICK)
        }
    }

    fun playStartVideoRecording(): Boolean {
        if (preferencesRepository.shutterSound.value || mustPlaySounds) {
            mediaActionSound.play(MediaActionSound.START_VIDEO_RECORDING)
            return true
        }
        return false
    }

    fun playStopVideoRecording() {
        if (preferencesRepository.shutterSound.value || mustPlaySounds) {
            mediaActionSound.play(MediaActionSound.STOP_VIDEO_RECORDING)
        }
    }

    fun release() = mediaActionSound.release()

    companion object {
        val mustPlaySounds: Boolean
            @Suppress("DiscouragedApi")
            get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                MediaActionSound.mustPlayShutterSound()
            } else {
                val resources = Resources.getSystem()
                val id = resources.getIdentifier("config_camera_sound_forced", "bool", "android")
                id > 0 && resources.getBoolean(id)
            }
    }
}
