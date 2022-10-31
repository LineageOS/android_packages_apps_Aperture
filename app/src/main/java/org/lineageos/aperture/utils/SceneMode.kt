/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.hardware.camera2.CaptureRequest
import org.lineageos.aperture.R

enum class SceneMode(val value: Int, val title: Int) {
    DISABLED(CaptureRequest.CONTROL_SCENE_MODE_DISABLED, R.string.scene_mode_disabled),
    FACE_PRIORITY(
        CaptureRequest.CONTROL_SCENE_MODE_FACE_PRIORITY, R.string.scene_mode_face_priority
    ),
    ACTION(CaptureRequest.CONTROL_SCENE_MODE_ACTION, R.string.scene_mode_action),
    PORTRAIT(CaptureRequest.CONTROL_SCENE_MODE_PORTRAIT, R.string.scene_mode_portrait),
    LANDSCAPE(CaptureRequest.CONTROL_SCENE_MODE_LANDSCAPE, R.string.scene_mode_landscape),
    NIGHT(CaptureRequest.CONTROL_SCENE_MODE_NIGHT, R.string.scene_mode_night),
    NIGHT_PORTRAIT(
        CaptureRequest.CONTROL_SCENE_MODE_NIGHT_PORTRAIT, R.string.scene_mode_night_portrait
    ),
    THEATRE(CaptureRequest.CONTROL_SCENE_MODE_THEATRE, R.string.scene_mode_theatre),
    BEACH(CaptureRequest.CONTROL_SCENE_MODE_BEACH, R.string.scene_mode_beach),
    SNOW(CaptureRequest.CONTROL_SCENE_MODE_SNOW, R.string.scene_mode_snow),
    SUNSET(CaptureRequest.CONTROL_SCENE_MODE_SUNSET, R.string.scene_mode_sunset),
    STEADYPHOTO(CaptureRequest.CONTROL_SCENE_MODE_STEADYPHOTO, R.string.scene_mode_steadyphoto),
    FIREWORKS(CaptureRequest.CONTROL_SCENE_MODE_FIREWORKS, R.string.scene_mode_fireworks),
    SPORTS(CaptureRequest.CONTROL_SCENE_MODE_SPORTS, R.string.scene_mode_sports),
    PARTY(CaptureRequest.CONTROL_SCENE_MODE_PARTY, R.string.scene_mode_party),
    CANDLELIGHT(CaptureRequest.CONTROL_SCENE_MODE_CANDLELIGHT, R.string.scene_mode_candlelight),
    BARCODE(CaptureRequest.CONTROL_SCENE_MODE_BARCODE, R.string.scene_mode_barcode),
    HDR(CaptureRequest.CONTROL_SCENE_MODE_HDR, R.string.scene_mode_hdr);

    companion object {
        fun fromValue(value: Int) = SceneMode.values().firstOrNull { it.value == value }
    }
}
