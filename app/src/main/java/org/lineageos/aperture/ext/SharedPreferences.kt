/*
 * SPDX-FileCopyrightText: 2022-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.lineageos.aperture.models.ColorCorrectionAberrationMode
import org.lineageos.aperture.models.DistortionCorrectionMode
import org.lineageos.aperture.models.EdgeMode
import org.lineageos.aperture.models.GestureAction
import org.lineageos.aperture.models.HardwareKey
import org.lineageos.aperture.models.HotPixelMode
import org.lineageos.aperture.models.NoiseReductionMode
import org.lineageos.aperture.models.ShadingMode
import org.lineageos.aperture.repositories.PreferencesRepository.Companion.sharedPreferencesKeyPrefix

fun <T> SharedPreferences.preferenceFlow(
    vararg keys: String,
    getter: SharedPreferences.() -> T,
) = callbackFlow {
    val update = {
        trySend(getter())
    }

    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
        if (changedKey in keys) {
            update()
        }
    }

    registerOnSharedPreferenceChangeListener(listener)

    update()

    awaitClose {
        unregisterOnSharedPreferenceChangeListener(listener)
    }
}

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

// Save location
private const val SAVE_LOCATION = "save_location"
internal var SharedPreferences.saveLocation: Boolean?
    get() = getBoolean(SAVE_LOCATION)
    set(value) = edit {
        putBoolean(SAVE_LOCATION, value)
    }

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

// Hardware key preferences
internal fun stringToGestureAction(string: String?) = when (string) {
    "shutter" -> GestureAction.SHUTTER
    "focus" -> GestureAction.FOCUS
    "mic_mute" -> GestureAction.MIC_MUTE
    "zoom" -> GestureAction.ZOOM
    "volume", "default" -> GestureAction.DEFAULT // volume for compat
    "nothing" -> GestureAction.NOTHING
    else -> null
}

internal fun SharedPreferences.getHardwareKeyAction(
    hardwareKey: HardwareKey
) = stringToGestureAction(
    getString("${hardwareKey.sharedPreferencesKeyPrefix}_action", null)
) ?: hardwareKey.defaultAction

internal fun SharedPreferences.getHardwareKeyInvert(
    hardwareKey: HardwareKey
) = hardwareKey.isTwoWayKey && getBoolean(
    "${hardwareKey.sharedPreferencesKeyPrefix}_invert", false
)

internal fun gestureActionToString(gestureAction: GestureAction) = when (gestureAction) {
    GestureAction.SHUTTER -> "shutter"
    GestureAction.FOCUS -> "focus"
    GestureAction.MIC_MUTE -> "mic_mute"
    GestureAction.ZOOM -> "zoom"
    GestureAction.DEFAULT -> "default"
    GestureAction.NOTHING -> "nothing"
}
