/*
 * SPDX-FileCopyrightText: 2022-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.content.SharedPreferences
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture
import androidx.camera.extensions.ExtensionMode
import androidx.camera.video.Quality
import androidx.core.content.edit
import org.lineageos.aperture.models.CameraFacing
import org.lineageos.aperture.models.CameraMode
import org.lineageos.aperture.models.ColorCorrectionAberrationMode
import org.lineageos.aperture.models.DistortionCorrectionMode
import org.lineageos.aperture.models.EdgeMode
import org.lineageos.aperture.models.FlashMode
import org.lineageos.aperture.models.FrameRate
import org.lineageos.aperture.models.GestureAction
import org.lineageos.aperture.models.GridMode
import org.lineageos.aperture.models.HardwareKey
import org.lineageos.aperture.models.HotPixelMode
import org.lineageos.aperture.models.NoiseReductionMode
import org.lineageos.aperture.models.ShadingMode
import org.lineageos.aperture.models.TimerMode
import org.lineageos.aperture.models.VideoDynamicRange
import org.lineageos.aperture.models.VideoMirrorMode

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
        "on_goldenratio" -> GridMode.ON_GOLDEN_RATIO
        // Default to off
        else -> GridMode.OFF
    }
    set(value) = edit {
        putString(
            LAST_GRID_MODE_KEY, when (value) {
                GridMode.OFF -> "off"
                GridMode.ON_3 -> "on_3"
                GridMode.ON_4 -> "on_4"
                GridMode.ON_GOLDEN_RATIO -> "on_goldenratio"
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
        "torch" -> FlashMode.TORCH
        "screen" -> FlashMode.SCREEN
        // Default to auto
        else -> FlashMode.AUTO
    }
    set(value) = edit {
        putString(
            PHOTO_FLASH_MODE_KEY, when (value) {
                FlashMode.OFF -> "off"
                FlashMode.AUTO -> "auto"
                FlashMode.ON -> "on"
                FlashMode.TORCH -> "torch"
                FlashMode.SCREEN -> "screen"
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

// Video stabilization
private const val VIDEO_STABILIZATION_KEY = "video_stabilization"
private const val VIDEO_STABILIZATION_DEFAULT = true
internal val SharedPreferences.videoStabilization: Boolean
    get() = getBoolean(VIDEO_STABILIZATION_KEY, VIDEO_STABILIZATION_DEFAULT)

// Edge mode
private const val EDGE_MODE_KEY = "edge_mode"
private const val EDGE_MODE_DEFAULT = "default"
internal val SharedPreferences.edgeMode: EdgeMode?
    get() = when (getString(EDGE_MODE_KEY, EDGE_MODE_DEFAULT)) {
        "default" -> null
        "off" -> EdgeMode.OFF
        "fast" -> EdgeMode.FAST
        "high_quality" -> EdgeMode.HIGH_QUALITY
        // Default to default
        else -> null
    }

// Noise reduction mode
private const val NOISE_REDUCTION_MODE_KEY = "noise_reduction_mode"
private const val NOISE_REDUCTION_MODE_DEFAULT = "default"
internal val SharedPreferences.noiseReductionMode: NoiseReductionMode?
    get() = when (getString(NOISE_REDUCTION_MODE_KEY, NOISE_REDUCTION_MODE_DEFAULT)) {
        "default" -> null
        "off" -> NoiseReductionMode.OFF
        "fast" -> NoiseReductionMode.FAST
        "high_quality" -> NoiseReductionMode.HIGH_QUALITY
        "minimal" -> NoiseReductionMode.MINIMAL
        // Default to default
        else -> null
    }

// Shading mode
private const val SHADING_MODE_KEY = "shading_mode"
private const val SHADING_MODE_DEFAULT = "default"
internal val SharedPreferences.shadingMode: ShadingMode?
    get() = when (getString(SHADING_MODE_KEY, SHADING_MODE_DEFAULT)) {
        "default" -> null
        "off" -> ShadingMode.OFF
        "fast" -> ShadingMode.FAST
        "high_quality" -> ShadingMode.HIGH_QUALITY
        // Default to default
        else -> null
    }

// Color correction aberration mode
private const val COLOR_CORRECTION_ABERRATION_MODE_KEY = "color_correction_aberration_mode"
private const val COLOR_CORRECTION_ABERRATION_MODE_DEFAULT = "default"
internal val SharedPreferences.colorCorrectionAberrationMode: ColorCorrectionAberrationMode?
    get() = when (getString(
        COLOR_CORRECTION_ABERRATION_MODE_KEY, COLOR_CORRECTION_ABERRATION_MODE_DEFAULT
    )) {
        "default" -> null
        "off" -> ColorCorrectionAberrationMode.OFF
        "fast" -> ColorCorrectionAberrationMode.FAST
        "high_quality" -> ColorCorrectionAberrationMode.HIGH_QUALITY
        // Default to default
        else -> null
    }

// Distortion correction mode
private const val DISTORTION_CORRECTION_MODE_KEY = "distortion_correction_mode"
private const val DISTORTION_CORRECTION_MODE_DEFAULT = "default"
internal val SharedPreferences.distortionCorrectionMode: DistortionCorrectionMode?
    get() = when (getString(DISTORTION_CORRECTION_MODE_KEY, DISTORTION_CORRECTION_MODE_DEFAULT)) {
        "default" -> null
        "off" -> DistortionCorrectionMode.OFF
        "fast" -> DistortionCorrectionMode.FAST
        "high_quality" -> DistortionCorrectionMode.HIGH_QUALITY
        // Default to default
        else -> null
    }

// Hot pixel mode
private const val HOT_PIXEL_MODE_KEY = "hot_pixel_mode"
private const val HOT_PIXEL_MODE_DEFAULT = "default"
internal val SharedPreferences.hotPixelMode: HotPixelMode?
    get() = when (getString(HOT_PIXEL_MODE_KEY, HOT_PIXEL_MODE_DEFAULT)) {
        "default" -> null
        "off" -> HotPixelMode.OFF
        "fast" -> HotPixelMode.FAST
        "high_quality" -> HotPixelMode.HIGH_QUALITY
        // Default to default
        else -> null
    }

// Force torch mode help shown
private const val FORCE_TORCH_HELP_SHOWN_KEY = "force_torch_help_shown"
private const val FORCE_TORCH_HELP_SHOWN_DEFAULT = false
internal var SharedPreferences.forceTorchHelpShown: Boolean
    get() = getBoolean(FORCE_TORCH_HELP_SHOWN_KEY, FORCE_TORCH_HELP_SHOWN_DEFAULT)
    set(value) = edit {
        putBoolean(FORCE_TORCH_HELP_SHOWN_KEY, value)
    }

// Video dynamic range
private const val VIDEO_DYNAMIC_RANGE_KEY = "video_dynamic_range"
private const val VIDEO_DYNAMIC_RANGE_DEFAULT = "sdr"
internal var SharedPreferences.videoDynamicRange: VideoDynamicRange
    get() = when (getString(VIDEO_DYNAMIC_RANGE_KEY, VIDEO_DYNAMIC_RANGE_DEFAULT)) {
        "sdr" -> VideoDynamicRange.SDR
        "hlg_10_bit" -> VideoDynamicRange.HLG_10_BIT
        "hdr10_10_bit" -> VideoDynamicRange.HDR10_10_BIT
        "hdr10_plus_10_bit" -> VideoDynamicRange.HDR10_PLUS_10_BIT
        "dolby_vision_10_bit" -> VideoDynamicRange.DOLBY_VISION_10_BIT
        "dolby_vision_8_bit" -> VideoDynamicRange.DOLBY_VISION_8_BIT
        // Default to sdr
        else -> VideoDynamicRange.SDR
    }
    set(value) = edit {
        putString(
            VIDEO_DYNAMIC_RANGE_KEY, when (value) {
                VideoDynamicRange.SDR -> "sdr"
                VideoDynamicRange.HLG_10_BIT -> "hlg_10_bit"
                VideoDynamicRange.HDR10_10_BIT -> "hdr10_10_bit"
                VideoDynamicRange.HDR10_PLUS_10_BIT -> "hdr10_plus_10_bit"
                VideoDynamicRange.DOLBY_VISION_10_BIT -> "dolby_vision_10_bit"
                VideoDynamicRange.DOLBY_VISION_8_BIT -> "dolby_vision_8_bit"
            }
        )
    }

// Video mirror mode
private const val VIDEO_MIRROR_MODE_KEY = "video_mirror_mode"
private const val VIDEO_MIRROR_MODE_DEFAULT = "off"
internal val SharedPreferences.videoMirrorMode: VideoMirrorMode
    get() = when (getString(VIDEO_MIRROR_MODE_KEY, VIDEO_MIRROR_MODE_DEFAULT)) {
        "off" -> VideoMirrorMode.OFF
        "on" -> VideoMirrorMode.ON
        "on_ffc_only" -> VideoMirrorMode.ON_FFC_ONLY
        // Default to off
        else -> VideoMirrorMode.OFF
    }

// Hardware key preferences
internal fun SharedPreferences.getHardwareKeyAction(
    hardwareKey: HardwareKey
) = when (getString("${hardwareKey.sharedPreferencesKeyPrefix}_action", null)) {
    "shutter" -> GestureAction.SHUTTER
    "focus" -> GestureAction.FOCUS
    "zoom" -> GestureAction.ZOOM
    "volume", "default" -> GestureAction.DEFAULT // volume for compat
    "nothing" -> GestureAction.NOTHING
    else -> hardwareKey.defaultAction
}

internal fun SharedPreferences.getHardwareKeyInvert(
    hardwareKey: HardwareKey
) = hardwareKey.isTwoWayKey && getBoolean(
    "${hardwareKey.sharedPreferencesKeyPrefix}_invert", false
)

internal fun gestureActionToString(gestureAction: GestureAction) = when (gestureAction) {
    GestureAction.SHUTTER -> "shutter"
    GestureAction.FOCUS -> "focus"
    GestureAction.ZOOM -> "zoom"
    GestureAction.DEFAULT -> "default"
    GestureAction.NOTHING -> "nothing"
}
