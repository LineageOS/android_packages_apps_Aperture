/*
 * SPDX-FileCopyrightText: 2022-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.content.SharedPreferences
import android.net.Uri
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture
import androidx.camera.extensions.ExtensionMode
import androidx.camera.video.Quality
import androidx.core.content.edit
import org.lineageos.aperture.camera.CameraFacing
import org.lineageos.aperture.camera.CameraMode
import org.lineageos.aperture.camera.FlashMode
import org.lineageos.aperture.camera.FrameRate
import org.lineageos.aperture.utils.GridMode
import org.lineageos.aperture.utils.TimerMode

// Helpers
internal fun SharedPreferences.getBoolean(key: String): Boolean? {
    return if (contains(key)) {
        getBoolean(key, false)
    } else {
        null
    }
}

internal fun SharedPreferences.Editor.putBoolean(key: String, value: Boolean?) {
    if (value == null) {
        remove(key)
    } else {
        putBoolean(key, value)
    }
}

// Generic prefs
private const val LAST_CAMERA_FACING_KEY = "last_camera_facing"
private const val LAST_CAMERA_FACING_DEFAULT = "back"

internal var SharedPreferences.lastCameraFacing: CameraFacing
    get() = when (getString(LAST_CAMERA_FACING_KEY, LAST_CAMERA_FACING_DEFAULT)) {
        "unknown" -> CameraFacing.UNKNOWN
        "front" -> CameraFacing.FRONT
        "back" -> CameraFacing.BACK
        "external" -> CameraFacing.EXTERNAL
        // Default to back
        else -> CameraFacing.BACK
    }
    set(value) = edit {
        putString(
            LAST_CAMERA_FACING_KEY, when (value) {
                CameraFacing.UNKNOWN -> "unknown"
                CameraFacing.FRONT -> "front"
                CameraFacing.BACK -> "back"
                CameraFacing.EXTERNAL -> "external"
            }
        )
    }

private const val LAST_CAMERA_MODE_KEY = "last_camera_mode"
private const val LAST_CAMERA_MODE_DEFAULT = "photo"

internal var SharedPreferences.lastCameraMode: CameraMode
    get() = when (getString(LAST_CAMERA_MODE_KEY, LAST_CAMERA_MODE_DEFAULT)) {
        "qr" -> CameraMode.QR
        "photo" -> CameraMode.PHOTO
        "video" -> CameraMode.VIDEO
        // Default to photo
        else -> CameraMode.PHOTO
    }
    set(value) = edit {
        putString(
            LAST_CAMERA_MODE_KEY, when (value) {
                CameraMode.QR -> "qr"
                CameraMode.PHOTO -> "photo"
                CameraMode.VIDEO -> "video"
            }
        )
    }

private const val LAST_GRID_MODE_KEY = "last_grid_mode"
private const val LAST_GRID_MODE_DEFAULT = "off"

internal var SharedPreferences.lastGridMode: GridMode
    get() = when (getString(LAST_GRID_MODE_KEY, LAST_GRID_MODE_DEFAULT)) {
        "off" -> GridMode.OFF
        "on_3" -> GridMode.ON_3
        "on_4" -> GridMode.ON_4
        "on_goldenratio" -> GridMode.ON_GOLDENRATIO
        // Default to off
        else -> GridMode.OFF
    }
    set(value) = edit {
        putString(
            LAST_GRID_MODE_KEY, when (value) {
                GridMode.OFF -> "off"
                GridMode.ON_3 -> "on_3"
                GridMode.ON_4 -> "on_4"
                GridMode.ON_GOLDENRATIO -> "on_goldenratio"
            }
        )
    }

private const val LAST_MIC_MODE_KEY = "last_mic_mode"
private const val LAST_MIC_MODE_DEFAULT = true

internal var SharedPreferences.lastMicMode: Boolean
    get() = getBoolean(LAST_MIC_MODE_KEY, LAST_MIC_MODE_DEFAULT)
    set(value) = edit {
        putBoolean(LAST_MIC_MODE_KEY, value)
    }

// Photos prefs
private const val PHOTO_CAPTURE_MODE_KEY = "photo_capture_mode"
private const val PHOTO_CAPTURE_MODE_DEFAULT = "minimize_latency"
private const val ENABLE_ZSL_KEY = "enable_zsl"
private const val ENABLE_ZSL_DEFAULT = false

internal val SharedPreferences.photoCaptureMode: Int
    @androidx.camera.core.ExperimentalZeroShutterLag
    get() = when (getString(PHOTO_CAPTURE_MODE_KEY, PHOTO_CAPTURE_MODE_DEFAULT)) {
        "maximize_quality" -> ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
        "minimize_latency" ->
            if (getBoolean(ENABLE_ZSL_KEY, ENABLE_ZSL_DEFAULT)) {
                ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG
            } else {
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
            }
        // Default to minimize latency
        else -> ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
    }

private const val PHOTO_FFC_MIRROR = "photo_ffc_mirror"
private const val PHOTO_FFC_MIRROR_DEFAULT = true

internal var SharedPreferences.photoFfcMirror: Boolean
    get() = getBoolean(PHOTO_FFC_MIRROR, PHOTO_FFC_MIRROR_DEFAULT)
    set(value) = edit {
        putBoolean(PHOTO_FFC_MIRROR, value)
    }

private const val PHOTO_FLASH_MODE_KEY = "photo_flash_mode"
private const val PHOTO_FLASH_MODE_DEFAULT = "auto"

internal var SharedPreferences.photoFlashMode: FlashMode
    get() = when (getString(PHOTO_FLASH_MODE_KEY, PHOTO_FLASH_MODE_DEFAULT)) {
        "off" -> FlashMode.OFF
        "auto" -> FlashMode.AUTO
        "on" -> FlashMode.ON
        // Default to auto
        else -> FlashMode.AUTO
    }
    set(value) = edit {
        putString(
            PHOTO_FLASH_MODE_KEY, when (value) {
                FlashMode.OFF -> "off"
                FlashMode.AUTO -> "auto"
                FlashMode.ON -> "on"
                // Default to auto
                else -> "auto"
            }
        )
    }

private const val VIDEO_FLASH_MODE_KEY = "video_flash_mode"
private const val VIDEO_FLASH_MODE_DEFAULT = "off"

internal var SharedPreferences.videoFlashMode: FlashMode
    get() = when (getString(VIDEO_FLASH_MODE_KEY, VIDEO_FLASH_MODE_DEFAULT)) {
        "off" -> FlashMode.OFF
        "torch" -> FlashMode.TORCH
        // Default to off
        else -> FlashMode.OFF
    }
    set(value) = edit {
        putString(
            VIDEO_FLASH_MODE_KEY, when (value) {
                FlashMode.OFF -> "off"
                FlashMode.TORCH -> "torch"
                // Default to off
                else -> VIDEO_FLASH_MODE_DEFAULT
            }
        )
    }

private const val PHOTO_EFFECT_KEY = "photo_effect"
private const val PHOTO_EFFECT_DEFAULT = "none"

internal var SharedPreferences.photoEffect: Int
    get() = when (getString(PHOTO_EFFECT_KEY, PHOTO_EFFECT_DEFAULT)) {
        "none" -> ExtensionMode.NONE
        "bokeh" -> ExtensionMode.BOKEH
        "hdr" -> ExtensionMode.HDR
        "night" -> ExtensionMode.NIGHT
        "face_retouch" -> ExtensionMode.FACE_RETOUCH
        "auto" -> ExtensionMode.AUTO
        // Default to none
        else -> ExtensionMode.NONE
    }
    set(value) = edit {
        putString(
            PHOTO_EFFECT_KEY, when (value) {
                ExtensionMode.NONE -> "none"
                ExtensionMode.BOKEH -> "bokeh"
                ExtensionMode.HDR -> "hdr"
                ExtensionMode.NIGHT -> "night"
                ExtensionMode.FACE_RETOUCH -> "face_retouch"
                ExtensionMode.AUTO -> "auto"
                // Default to none
                else -> PHOTO_EFFECT_DEFAULT
            }
        )
    }

// Video prefs
private const val VIDEO_FRAME_RATE_KEY = "video_framerate"

internal var SharedPreferences.videoFrameRate: FrameRate?
    get() = FrameRate.fromValue(getInt(VIDEO_FRAME_RATE_KEY, -1))
    set(value) = edit {
        putInt(VIDEO_FRAME_RATE_KEY, value?.value ?: -1)
    }

private const val VIDEO_QUALITY_KEY = "video_quality"
private const val VIDEO_QUALITY_DEFAULT = "fhd"

internal var SharedPreferences.videoQuality: Quality
    get() = when (getString(VIDEO_QUALITY_KEY, VIDEO_QUALITY_DEFAULT)) {
        "sd" -> Quality.SD
        "hd" -> Quality.HD
        "fhd" -> Quality.FHD
        "uhd" -> Quality.UHD
        // Default to fhd
        else -> Quality.FHD
    }
    set(value) = edit {
        putString(
            VIDEO_QUALITY_KEY, when (value) {
                Quality.SD -> "sd"
                Quality.HD -> "hd"
                Quality.FHD -> "fhd"
                Quality.UHD -> "uhd"
                // Default to fhd
                else -> VIDEO_QUALITY_DEFAULT
            }
        )
    }

// Timer mode
private const val TIMER_MODE_KEY = "timer_mode"
private const val TIMER_MODE_DEFAULT = 0

internal var SharedPreferences.timerMode: TimerMode
    get() = TimerMode.fromSeconds(getInt(TIMER_MODE_KEY, TIMER_MODE_DEFAULT)) ?: TimerMode.OFF
    set(value) = edit {
        putInt(TIMER_MODE_KEY, value.seconds)
    }

// Aspect ratio
private const val ASPECT_RATIO_KEY = "aspect_ratio"
private const val ASPECT_RATIO_DEFAULT = "4_3"

internal var SharedPreferences.aspectRatio: Int
    get() = when (getString(ASPECT_RATIO_KEY, ASPECT_RATIO_DEFAULT)) {
        "4_3" -> AspectRatio.RATIO_4_3
        "16_9" -> AspectRatio.RATIO_16_9
        else -> AspectRatio.RATIO_4_3
    }
    set(value) = edit {
        putString(
            ASPECT_RATIO_KEY, when (value) {
                AspectRatio.RATIO_4_3 -> "4_3"
                AspectRatio.RATIO_16_9 -> "16_9"
                // Default to 4:3
                else -> ASPECT_RATIO_DEFAULT
            }
        )
    }

// Bright screen
private const val BRIGHT_SCREEN_KEY = "bright_screen"
private const val BRIGHT_SCREEN_DEFAULT = false

internal var SharedPreferences.brightScreen: Boolean
    get() = getBoolean(BRIGHT_SCREEN_KEY, BRIGHT_SCREEN_DEFAULT)
    set(value) = edit {
        putBoolean(BRIGHT_SCREEN_KEY, value)
    }

// Save location
private const val SAVE_LOCATION = "save_location"
internal var SharedPreferences.saveLocation: Boolean?
    get() = getBoolean(SAVE_LOCATION)
    set(value) = edit {
        putBoolean(SAVE_LOCATION, value)
    }

// Shutter sound
private const val SHUTTER_SOUND_KEY = "shutter_sound"
private const val SHUTTER_SOUND_DEFAULT = true

internal var SharedPreferences.shutterSound: Boolean
    get() = getBoolean(SHUTTER_SOUND_KEY, SHUTTER_SOUND_DEFAULT)
    set(value) = edit {
        putBoolean(SHUTTER_SOUND_KEY, value)
    }

// Leveler
private const val LEVELER_KEY = "leveler"
private const val LEVELER_DEFAULT = false

internal var SharedPreferences.leveler: Boolean
    get() = getBoolean(LEVELER_KEY, LEVELER_DEFAULT)
    set(value) = edit {
        putBoolean(LEVELER_KEY, value)
    }

// Last saved URI
private const val LAST_SAVED_URI_KEY = "saved_uri"

internal var SharedPreferences.lastSavedUri: Uri?
    get() {
        val raw = getString(LAST_SAVED_URI_KEY, null) ?: return null
        return Uri.parse(raw)
    }
    set(value) = edit {
        putString(LAST_SAVED_URI_KEY, value.toString())
    }

// Video stabilization
private const val VIDEO_STABILIZATION_KEY = "video_stabilization"
private const val VIDEO_STABILIZATION_DEFAULT = true
internal val SharedPreferences.videoStabilization: Boolean
    get() = getBoolean(VIDEO_STABILIZATION_KEY, VIDEO_STABILIZATION_DEFAULT)
