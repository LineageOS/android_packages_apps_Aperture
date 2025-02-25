/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.repository

import android.content.Context
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture
import androidx.camera.extensions.ExtensionMode
import androidx.camera.video.Quality
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
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
import org.lineageos.aperture.models.Preference.Companion.preference
import org.lineageos.aperture.models.Preference.Companion.enumPreference
import org.lineageos.aperture.models.ShadingMode
import org.lineageos.aperture.models.TimerMode
import org.lineageos.aperture.models.VideoDynamicRange
import org.lineageos.aperture.models.VideoMirrorMode

/**
 * User preferences repository.
 */
class PreferencesRepository(
    context: Context,
    private val coroutineScope: CoroutineScope,
) {
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    /**
     * The last [CameraFacing] used.
     */
    val lastCameraFacing = sharedPreferences.enumPreference(
        key = "last_camera_facing",
        defaultValue = CameraFacing.BACK,
        mapper = {
            when (this) {
                CameraFacing.UNKNOWN -> "unknown"
                CameraFacing.FRONT -> "front"
                CameraFacing.BACK -> "back"
                CameraFacing.EXTERNAL -> "external"
            }
        },
    )

    /**
     * The last [CameraMode] used.
     */
    val lastCameraMode = sharedPreferences.enumPreference(
        key = "last_camera_mode",
        defaultValue = CameraMode.PHOTO,
        mapper = {
            when (this) {
                CameraMode.QR -> "qr"
                CameraMode.PHOTO -> "photo"
                CameraMode.VIDEO -> "video"
            }
        },
    )

    /**
     * The last [GridMode] used.
     */
    val lastGridMode = sharedPreferences.enumPreference(
        key = "last_grid_mode",
        defaultValue = GridMode.OFF,
        mapper = {
            when (this) {
                GridMode.OFF -> "off"
                GridMode.ON_3 -> "on_3"
                GridMode.ON_4 -> "on_4"
                GridMode.ON_GOLDEN_RATIO -> "on_goldenratio"
            }
        },
    )

    /**
     * The last mic mode used.
     */
    val videoMicMode = sharedPreferences.preference(
        key = "last_mic_mode",
        defaultValue = true,
    )

    /**
     * Desired [ImageCapture.CaptureMode] for photos.
     */
    val photoCaptureMode = sharedPreferences.enumPreference<@ImageCapture.CaptureMode Int, _>(
        key = "photo_capture_mode",
        defaultValue = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
        valueToEnumMap = mapOf(
            "maximize_quality" to ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY,
            "minimize_latency" to ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
        ),
    )

    /**
     * Whether zero shutter lag should be used when available.
     */
    val enableZsl = sharedPreferences.preference(
        key = "enable_zsl",
        defaultValue = false,
    )

    /**
     * Toggle left-right mirroring of the front facing camera images.
     */
    val photoFfcMirror = sharedPreferences.preference(
        key = "photo_ffc_mirror",
        defaultValue = true,
    )

    /**
     * Desired [FlashMode] for photos.
     */
    val photoFlashMode = sharedPreferences.enumPreference(
        key = "photo_flash_mode",
        defaultValue = FlashMode.AUTO,
        mapper = {
            when (this) {
                FlashMode.OFF -> "off"
                FlashMode.AUTO -> "auto"
                FlashMode.ON -> "on"
                FlashMode.TORCH -> "torch"
                FlashMode.SCREEN -> "screen"
            }
        },
    )

    /**
     * Desired [FlashMode] for videos.
     */
    val videoFlashMode = sharedPreferences.enumPreference(
        key = "video_flash_mode",
        defaultValue = FlashMode.OFF,
        mapper = {
            when (this) {
                FlashMode.OFF -> "off"
                FlashMode.AUTO -> "auto"
                FlashMode.ON -> "on"
                FlashMode.TORCH -> "torch"
                FlashMode.SCREEN -> "screen"
            }
        },
    )

    /**
     * Desired [ExtensionMode] for photos.
     */
    val photoEffect = sharedPreferences.enumPreference<@ExtensionMode.Mode Int, _>(
        key = "photo_effect",
        defaultValue = ExtensionMode.NONE,
        valueToEnumMap = mapOf(
            "none" to ExtensionMode.NONE,
            "bokeh" to ExtensionMode.BOKEH,
            "hdr" to ExtensionMode.HDR,
            "night" to ExtensionMode.NIGHT,
            "face_retouch" to ExtensionMode.FACE_RETOUCH,
            "auto" to ExtensionMode.AUTO,
        ),
    )

    /**
     * Desired [FrameRate] for videos.
     */
    val videoFrameRate = sharedPreferences.enumPreference<FrameRate?, _, _>(
        key = "video_framerate",
        defaultValue = null,
        mapper = {
            when (this) {
                null -> null
                FrameRate.FPS_24 -> 24
                FrameRate.FPS_30 -> 30
                FrameRate.FPS_60 -> 60
                FrameRate.FPS_120 -> 120
            }
        },
    )

    /**
     * Desired [Quality] for videos.
     */
    val videoQuality = sharedPreferences.enumPreference(
        key = "video_quality",
        defaultValue = Quality.FHD,
        valueToEnumMap = mapOf(
            "sd" to Quality.SD,
            "hd" to Quality.HD,
            "fhd" to Quality.FHD,
            "uhd" to Quality.UHD,
        ),
    )

    /**
     * Desired [TimerMode].
     */
    val timerMode = sharedPreferences.enumPreference(
        key = "timer_mode",
        defaultValue = TimerMode.OFF,
        mapper = {
            when (this) {
                TimerMode.OFF -> 0
                TimerMode.ON_3S -> 3
                TimerMode.ON_10S -> 10
            }
        },
    )

    /**
     * Desired [AspectRatio] for photos.
     */
    val photoAspectRatio = sharedPreferences.enumPreference(
        key = "aspect_ratio",
        defaultValue = AspectRatio.RATIO_4_3,
        valueToEnumMap = mapOf(
            "4_3" to AspectRatio.RATIO_4_3,
            "16_9" to AspectRatio.RATIO_16_9,
        ),
    )

    /**
     * Whether bright screen should be enabled.
     */
    val brightScreen = sharedPreferences.preference(
        key = "bright_screen",
        defaultValue = false,
    )

    /**
     * Whether location should be saved.
     */
    val saveLocation = sharedPreferences.preference<Boolean?>(
        key = "save_location",
        defaultValue = null,
    )

    /**
     * Whether shutter sound should be played.
     */
    val shutterSound = sharedPreferences.preference(
        key = "shutter_sound",
        defaultValue = true,
    )

    /**
     * Whether leveler should be visible.
     */
    val leveler = sharedPreferences.preference(
        key = "leveler",
        defaultValue = false,
    )

    /**
     * Whether video stabilization should be enabled.
     */
    val videoStabilization = sharedPreferences.preference(
        key = "video_stabilization",
        defaultValue = true,
    )

    /**
     * Desired [EdgeMode].
     */
    val edgeMode = sharedPreferences.enumPreference<EdgeMode?, _, _>(
        key = "edge_mode",
        defaultValue = null,
        mapper = {
            when (this) {
                null -> "default"
                EdgeMode.OFF -> "off"
                EdgeMode.FAST -> "fast"
                EdgeMode.HIGH_QUALITY -> "high_quality"
                EdgeMode.ZERO_SHUTTER_LAG -> null
            }
        },
    )

    /**
     * Desired [NoiseReductionMode].
     */
    val noiseReductionMode = sharedPreferences.enumPreference<NoiseReductionMode?, _, _>(
        key = "noise_reduction_mode",
        defaultValue = null,
        mapper = {
            when (this) {
                null -> "default"
                NoiseReductionMode.OFF -> "off"
                NoiseReductionMode.FAST -> "fast"
                NoiseReductionMode.HIGH_QUALITY -> "high_quality"
                NoiseReductionMode.MINIMAL -> "minimal"
                NoiseReductionMode.ZERO_SHUTTER_LAG -> null
            }
        },
    )

    /**
     * Desired [ShadingMode].
     */
    val shadingMode = sharedPreferences.enumPreference<ShadingMode?, _, _>(
        key = "shading_mode",
        defaultValue = null,
        mapper = {
            when (this) {
                null -> "default"
                ShadingMode.OFF -> "off"
                ShadingMode.FAST -> "fast"
                ShadingMode.HIGH_QUALITY -> "high_quality"
            }
        },
    )

    /**
     * Desired [ColorCorrectionAberrationMode].
     */
    val colorCorrectionAberrationMode =
        sharedPreferences.enumPreference<ColorCorrectionAberrationMode?, _, _>(
            key = "color_correction_aberration_mode",
            defaultValue = null,
            mapper = {
                when (this) {
                    null -> "default"
                    ColorCorrectionAberrationMode.OFF -> "off"
                    ColorCorrectionAberrationMode.FAST -> "fast"
                    ColorCorrectionAberrationMode.HIGH_QUALITY -> "high_quality"
                }
            },
        )

    /**
     * Desired [DistortionCorrectionMode].
     */
    val distortionCorrectionMode =
        sharedPreferences.enumPreference<DistortionCorrectionMode?, _, _>(
            key = "distortion_correction_mode",
            defaultValue = null,
            mapper = {
                when (this) {
                    null -> "default"
                    DistortionCorrectionMode.OFF -> "off"
                    DistortionCorrectionMode.FAST -> "fast"
                    DistortionCorrectionMode.HIGH_QUALITY -> "high_quality"
                }
            },
        )

    /**
     * Desired [HotPixelMode].
     */
    val hotPixelMode = sharedPreferences.enumPreference<HotPixelMode?, _, _>(
        key = "hot_pixel_mode",
        defaultValue = null,
        mapper = {
            when (this) {
                null -> "default"
                HotPixelMode.OFF -> "off"
                HotPixelMode.FAST -> "fast"
                HotPixelMode.HIGH_QUALITY -> "high_quality"
            }
        },
    )

    /**
     * Whether force torch help dialog has been shown.
     */
    val forceTorchHelpShown = sharedPreferences.preference(
        key = "force_torch_help_shown",
        defaultValue = false,
    )

    /**
     * Desired [VideoDynamicRange] for videos.
     */
    val videoDynamicRange = sharedPreferences.enumPreference(
        key = "video_dynamic_range",
        defaultValue = VideoDynamicRange.SDR,
        mapper = {
            when (this) {
                VideoDynamicRange.SDR -> "sdr"
                VideoDynamicRange.HLG_10_BIT -> "hlg_10_bit"
                VideoDynamicRange.HDR10_10_BIT -> "hdr10_10_bit"
                VideoDynamicRange.HDR10_PLUS_10_BIT -> "hdr10_plus_10_bit"
                VideoDynamicRange.DOLBY_VISION_10_BIT -> "dolby_vision_10_bit"
                VideoDynamicRange.DOLBY_VISION_8_BIT -> "dolby_vision_8_bit"
            }
        },
    )

    /**
     * Video mirror mode.
     */
    val videoMirrorMode = sharedPreferences.enumPreference(
        key = "video_mirror_mode",
        defaultValue = VideoMirrorMode.OFF,
        mapper = {
            when (this) {
                VideoMirrorMode.OFF -> "off"
                VideoMirrorMode.ON -> "on"
                VideoMirrorMode.ON_FFC_ONLY -> "on_ffc_only"
            }
        },
    )

    /**
     * The hardware key action preferences for all [HardwareKey]s.
     */
    val hardwareKeyActionPreferences = HardwareKey.entries.associateWith {
        sharedPreferences.enumPreference(
            key = "${it.sharedPreferencesKeyPrefix}_action",
            defaultValue = it.defaultAction,
            mapper = {
                when (this) {
                    GestureAction.SHUTTER -> "shutter"
                    GestureAction.FOCUS -> "focus"
                    GestureAction.MIC_MUTE -> "mic_mute"
                    GestureAction.ZOOM -> "zoom"
                    GestureAction.DEFAULT -> "default"
                    GestureAction.NOTHING -> "nothing"
                }
            },
        )
    }

    /**
     * The hardware key invert preferences for all [HardwareKey]s.
     */
    val hardwareKeyInvertPreferences = HardwareKey.entries.associateWith {
        sharedPreferences.preference(
            key = "${it.sharedPreferencesKeyPrefix}_invert",
            defaultValue = it.isTwoWayKey,
        )
    }
}
