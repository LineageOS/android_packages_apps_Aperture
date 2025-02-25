/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.repositories

import android.content.Context
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture
import androidx.camera.extensions.ExtensionMode
import androidx.camera.video.Quality
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import org.lineageos.aperture.ext.preferenceFlow
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
import org.lineageos.aperture.models.Preference
import org.lineageos.aperture.models.Preference.Companion.enumPreference
import org.lineageos.aperture.models.Preference.Companion.primitivePreference
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
    /**
     * [Preference] holder.
     */
    inner class PreferenceHolder<T>(
        private val preference: Preference<T>,
    ) : Flow<T> by preference.preferenceFlow() {
        /**
         * The value.
         */
        var value: T
            get() = preference.getValue(sharedPreferences)
            set(value) = sharedPreferences.edit {
                preference.setValue(this, value)
            }
    }

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    /**
     * The last [CameraFacing] used.
     */
    val lastCameraFacing = enumPreference<CameraFacing, String>(
        key = "last_camera_facing",
        defaultValue = CameraFacing.BACK,
        mapper = { it: CameraFacing ->
            when (it) {
                CameraFacing.UNKNOWN -> "unknown"
                CameraFacing.FRONT -> "front"
                CameraFacing.BACK -> "back"
                CameraFacing.EXTERNAL -> "external"
            }
        },
    ).asPreferenceHolder()

    /**
     * The last [CameraMode] used.
     */
    val lastCameraMode = enumPreference<CameraMode, String>(
        key = "last_camera_mode",
        defaultValue = CameraMode.PHOTO,
        mapper = { it: CameraMode ->
            when (it) {
                CameraMode.QR -> "qr"
                CameraMode.PHOTO -> "photo"
                CameraMode.VIDEO -> "video"
            }
        },
    ).asPreferenceHolder()

    /**
     * The last [GridMode] used.
     */
    val lastGridMode = enumPreference<GridMode, String>(
        key = "last_grid_mode",
        defaultValue = GridMode.OFF,
        mapper = { it: GridMode ->
            when (it) {
                GridMode.OFF -> "off"
                GridMode.ON_3 -> "on_3"
                GridMode.ON_4 -> "on_4"
                GridMode.ON_GOLDEN_RATIO -> "on_goldenratio"
            }
        },
    ).asPreferenceHolder()

    /**
     * The last mic mode used.
     */
    val videoMicMode = primitivePreference(
        key = "last_mic_mode",
        defaultValue = true,
    ).asPreferenceHolder()

    /**
     * Desired [ImageCapture.CaptureMode] for photos.
     */
    val photoCaptureMode = enumPreference(
        key = "photo_capture_mode",
        defaultValue = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
        valueToEnumMap = mapOf(
            "maximize_quality" to ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY,
            "minimize_latency" to ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
        ),
    ).asPreferenceHolder()

    /**
     * Whether zero shutter lag should be used when available.
     */
    val enableZsl = primitivePreference(
        key = "enable_zsl",
        defaultValue = false,
    ).asPreferenceHolder()

    /**
     * Toggle left-right mirroring of the front facing camera images.
     */
    val photoFfcMirror = primitivePreference(
        key = "photo_ffc_mirror",
        defaultValue = true,
    ).asPreferenceHolder()

    /**
     * Desired [FlashMode] for photos.
     */
    val photoFlashMode = enumPreference<FlashMode, String>(
        key = "photo_flash_mode",
        defaultValue = FlashMode.AUTO,
        mapper = { it: FlashMode ->
            when (it) {
                FlashMode.OFF -> "off"
                FlashMode.AUTO -> "auto"
                FlashMode.ON -> "on"
                FlashMode.TORCH -> "torch"
                FlashMode.SCREEN -> "screen"
            }
        },
    ).asPreferenceHolder()

    /**
     * Desired [FlashMode] for videos.
     */
    val videoFlashMode = enumPreference<FlashMode, String>(
        key = "video_flash_mode",
        defaultValue = FlashMode.OFF,
        mapper = { it: FlashMode ->
            when (it) {
                FlashMode.OFF -> "off"
                FlashMode.AUTO -> "auto"
                FlashMode.ON -> "on"
                FlashMode.TORCH -> "torch"
                FlashMode.SCREEN -> "screen"
            }
        },
    ).asPreferenceHolder()

    /**
     * Desired [ExtensionMode] for photos.
     */
    val photoEffect = enumPreference(
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
    ).asPreferenceHolder()

    /**
     * Desired [FrameRate] for videos.
     */
    val videoFrameRate = enumPreference<FrameRate, Int>(
        key = "video_framerate",
        defaultValue = null,
        mapper = { it: FrameRate? ->
            when (it) {
                null -> null
                FrameRate.FPS_24 -> 24
                FrameRate.FPS_30 -> 30
                FrameRate.FPS_60 -> 60
                FrameRate.FPS_120 -> 120
            }
        },
    ).asPreferenceHolder()

    /**
     * Desired [Quality] for videos.
     */
    val videoQuality = enumPreference(
        key = "video_quality",
        defaultValue = Quality.FHD,
        valueToEnumMap = mapOf(
            "sd" to Quality.SD,
            "hd" to Quality.HD,
            "fhd" to Quality.FHD,
            "uhd" to Quality.UHD,
        ),
    ).asPreferenceHolder()

    /**
     * Desired [TimerMode].
     */
    val timerMode = enumPreference<TimerMode, Int>(
        key = "timer_mode",
        defaultValue = TimerMode.OFF,
        mapper = { it: TimerMode ->
            when (it) {
                TimerMode.OFF -> 0
                TimerMode.ON_3S -> 3
                TimerMode.ON_10S -> 10
            }
        },
    ).asPreferenceHolder()

    /**
     * Desired [AspectRatio] for photos.
     */
    val photoAspectRatio = enumPreference(
        key = "aspect_ratio",
        defaultValue = AspectRatio.RATIO_4_3,
        valueToEnumMap = mapOf(
            "4_3" to AspectRatio.RATIO_4_3,
            "16_9" to AspectRatio.RATIO_16_9,
        ),
    ).asPreferenceHolder()

    /**
     * Whether bright screen should be enabled.
     */
    val brightScreen = primitivePreference(
        key = "bright_screen",
        defaultValue = false,
    ).asPreferenceHolder()

    /**
     * Whether location should be saved.
     */
    val saveLocation = primitivePreference<Boolean?>(
        key = "save_location",
        defaultValue = null,
    ).asPreferenceHolder()

    /**
     * Whether shutter sound should be played.
     */
    val shutterSound = primitivePreference(
        key = "shutter_sound",
        defaultValue = true,
    ).asPreferenceHolder()

    /**
     * Whether leveler should be visible.
     */
    val leveler = primitivePreference(
        key = "leveler",
        defaultValue = false,
    ).asPreferenceHolder()

    /**
     * Whether video stabilization should be enabled.
     */
    val videoStabilization = primitivePreference(
        key = "video_stabilization",
        defaultValue = true,
    ).asPreferenceHolder()

    /**
     * Desired [EdgeMode].
     */
    val edgeMode = enumPreference<EdgeMode, String>(
        key = "edge_mode",
        defaultValue = null,
        mapper = { it: EdgeMode? ->
            when (it) {
                null -> "default"
                EdgeMode.OFF -> "off"
                EdgeMode.FAST -> "fast"
                EdgeMode.HIGH_QUALITY -> "high_quality"
                EdgeMode.ZERO_SHUTTER_LAG -> null
            }
        },
    ).asPreferenceHolder()

    /**
     * Desired [NoiseReductionMode].
     */
    val noiseReductionMode = enumPreference<NoiseReductionMode, String>(
        key = "noise_reduction_mode",
        defaultValue = null,
        mapper = { it: NoiseReductionMode? ->
            when (it) {
                null -> "default"
                NoiseReductionMode.OFF -> "off"
                NoiseReductionMode.FAST -> "fast"
                NoiseReductionMode.HIGH_QUALITY -> "high_quality"
                NoiseReductionMode.MINIMAL -> "minimal"
                NoiseReductionMode.ZERO_SHUTTER_LAG -> null
            }
        },
    ).asPreferenceHolder()

    /**
     * Desired [ShadingMode].
     */
    val shadingMode = enumPreference<ShadingMode, String>(
        key = "shading_mode",
        defaultValue = null,
        mapper = { it: ShadingMode? ->
            when (it) {
                null -> "default"
                ShadingMode.OFF -> "off"
                ShadingMode.FAST -> "fast"
                ShadingMode.HIGH_QUALITY -> "high_quality"
            }
        },
    ).asPreferenceHolder()

    /**
     * Desired [ColorCorrectionAberrationMode].
     */
    val colorCorrectionAberrationMode = enumPreference<ColorCorrectionAberrationMode, String>(
        key = "color_correction_aberration_mode",
        defaultValue = null,
        mapper = { it: ColorCorrectionAberrationMode? ->
            when (it) {
                null -> "default"
                ColorCorrectionAberrationMode.OFF -> "off"
                ColorCorrectionAberrationMode.FAST -> "fast"
                ColorCorrectionAberrationMode.HIGH_QUALITY -> "high_quality"
            }
        },
    ).asPreferenceHolder()

    /**
     * Desired [DistortionCorrectionMode].
     */
    val distortionCorrectionMode = enumPreference<DistortionCorrectionMode, String>(
        key = "distortion_correction_mode",
        defaultValue = null,
        mapper = { it: DistortionCorrectionMode? ->
            when (it) {
                null -> "default"
                DistortionCorrectionMode.OFF -> "off"
                DistortionCorrectionMode.FAST -> "fast"
                DistortionCorrectionMode.HIGH_QUALITY -> "high_quality"
            }
        },
    ).asPreferenceHolder()

    /**
     * Desired [HotPixelMode].
     */
    val hotPixelMode = enumPreference<HotPixelMode, String>(
        key = "hot_pixel_mode",
        defaultValue = null,
        mapper = { it: HotPixelMode? ->
            when (it) {
                null -> "default"
                HotPixelMode.OFF -> "off"
                HotPixelMode.FAST -> "fast"
                HotPixelMode.HIGH_QUALITY -> "high_quality"
            }
        },
    ).asPreferenceHolder()

    /**
     * Whether force torch help dialog has been shown.
     */
    val forceTorchHelpShown = primitivePreference(
        key = "force_torch_help_shown",
        defaultValue = false,
    ).asPreferenceHolder()

    /**
     * Desired [VideoDynamicRange] for videos.
     */
    val videoDynamicRange = enumPreference<VideoDynamicRange, String>(
        key = "video_dynamic_range",
        defaultValue = VideoDynamicRange.SDR,
        mapper = { it: VideoDynamicRange ->
            when (it) {
                VideoDynamicRange.SDR -> "sdr"
                VideoDynamicRange.HLG_10_BIT -> "hlg_10_bit"
                VideoDynamicRange.HDR10_10_BIT -> "hdr10_10_bit"
                VideoDynamicRange.HDR10_PLUS_10_BIT -> "hdr10_plus_10_bit"
                VideoDynamicRange.DOLBY_VISION_10_BIT -> "dolby_vision_10_bit"
                VideoDynamicRange.DOLBY_VISION_8_BIT -> "dolby_vision_8_bit"
            }
        },
    ).asPreferenceHolder()

    /**
     * Video mirror mode.
     */
    val videoMirrorMode = enumPreference<VideoMirrorMode, String>(
        key = "video_mirror_mode",
        defaultValue = VideoMirrorMode.OFF,
        mapper = { it: VideoMirrorMode ->
            when (it) {
                VideoMirrorMode.OFF -> "off"
                VideoMirrorMode.ON -> "on"
                VideoMirrorMode.ON_FFC_ONLY -> "on_ffc_only"
            }
        },
    ).asPreferenceHolder()

    /**
     * The hardware key action preferences for all [HardwareKey]s.
     */
    val hardwareKeyActionPreferences = HardwareKey.entries.associateWith { hardwareKey ->
        enumPreference<GestureAction, String>(
            key = "${hardwareKey.sharedPreferencesKeyPrefix}_action",
            defaultValue = hardwareKey.defaultAction,
            mapper = { it: GestureAction -> it.toPreferenceString() },
        ).asPreferenceHolder()
    }

    /**
     * The hardware key invert preferences for all [HardwareKey]s.
     */
    val hardwareKeyInvertPreferences = HardwareKey.entries.associateWith {
        primitivePreference(
            key = "${it.sharedPreferencesKeyPrefix}_invert",
            defaultValue = it.isTwoWayKey,
        ).asPreferenceHolder()
    }

    private fun <T> Preference<T>.preferenceFlow() = sharedPreferences.preferenceFlow(
        key, getter = { getValue(sharedPreferences) }
    )
        .flowOn(Dispatchers.IO)
        .shareIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(),
            replay = 1,
        )

    private fun <T> Preference<T>.asPreferenceHolder() = PreferenceHolder(
        preference = this,
    )

    companion object {
        val HardwareKey.sharedPreferencesKeyPrefix: String
            get() = when (this) {
                HardwareKey.CAMERA -> "camera_button"
                HardwareKey.FOCUS -> "focus_button"
                HardwareKey.MUTE -> "mute_button"
                HardwareKey.VOLUME -> "volume_buttons"
                HardwareKey.ZOOM -> "zoom_buttons"
            }

        fun GestureAction.toPreferenceString() = when (this) {
            GestureAction.SHUTTER -> "shutter"
            GestureAction.FOCUS -> "focus"
            GestureAction.MIC_MUTE -> "mic_mute"
            GestureAction.ZOOM -> "zoom"
            GestureAction.DEFAULT -> "default"
            GestureAction.NOTHING -> "nothing"
        }

        internal fun preferenceStringToGestureAction(string: String?) = when (string) {
            "shutter" -> GestureAction.SHUTTER
            "focus" -> GestureAction.FOCUS
            "mic_mute" -> GestureAction.MIC_MUTE
            "zoom" -> GestureAction.ZOOM
            "volume", "default" -> GestureAction.DEFAULT // volume for compat
            "nothing" -> GestureAction.NOTHING
            else -> null
        }
    }
}
