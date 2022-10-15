/*
 * Copyright (C) 2022 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.hardware.Camera.CameraInfo
import android.media.MediaActionSound
import android.os.Build
import org.lineageos.aperture.shutterSound

class CameraSoundsUtils(private val sharedPreferences: SharedPreferences) {
    private val mediaActionSound = MediaActionSound().apply {
        // Preload all sounds to reduce latency
        load(MediaActionSound.SHUTTER_CLICK)
        load(MediaActionSound.START_VIDEO_RECORDING)
        load(MediaActionSound.STOP_VIDEO_RECORDING)
    }

    fun playShutterClick() {
        if (sharedPreferences.shutterSound || mustPlaySounds) {
            mediaActionSound.play(MediaActionSound.SHUTTER_CLICK)
        }
    }

    fun playStartVideoRecording(): Boolean {
        if (sharedPreferences.shutterSound || mustPlaySounds) {
            mediaActionSound.play(MediaActionSound.START_VIDEO_RECORDING)
            return true
        }
        return false
    }

    fun playStopVideoRecording() {
        if (sharedPreferences.shutterSound || mustPlaySounds) {
            mediaActionSound.play(MediaActionSound.STOP_VIDEO_RECORDING)
        }
    }

    companion object {
        val mustPlaySounds: Boolean
            @SuppressLint("DiscouragedApi")
            get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                MediaActionSound.mustPlayShutterSound()
            } else {
                // CameraInfo is deprecated
                val info = CameraInfo()
                !info.canDisableShutterSound
            }
    }
}
