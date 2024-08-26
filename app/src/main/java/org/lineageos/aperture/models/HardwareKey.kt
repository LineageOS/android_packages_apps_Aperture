/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

import android.content.SharedPreferences
import android.view.KeyEvent

/**
 * Collection of keys that can be used to do things.
 * @param firstKeycode The main [KeyEvent] for this key. If [secondKeycode] is defined, this keycode
 *   will be treated as the increase (or up) keycode
 * @param secondKeycode The [KeyEvent] keycode for decrease (or down)
 * @param sharedPreferencesKeyPrefix The key prefix for [SharedPreferences] settings
 * @param supportsDefault Whether it makes sense to let Android handle these keycodes (e.g. volume
 *   control for [HardwareKey.VOLUME])
 * @param defaultAction The default [GestureAction] if the user didn't specify any
 */
enum class HardwareKey(
    val firstKeycode: Int,
    val secondKeycode: Int?,
    val sharedPreferencesKeyPrefix: String,
    val supportsDefault: Boolean,
    val defaultAction: GestureAction,
) {
    CAMERA(
        KeyEvent.KEYCODE_CAMERA,
        null,
        "camera_button",
        false,
        GestureAction.SHUTTER,
    ),
    FOCUS(
        KeyEvent.KEYCODE_FOCUS,
        null,
        "focus_button",
        false,
        GestureAction.FOCUS,
    ),
    VOLUME(
        KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_VOLUME_DOWN,
        "volume_buttons",
        true,
        GestureAction.SHUTTER,
    ),
    ZOOM(
        KeyEvent.KEYCODE_ZOOM_IN,
        KeyEvent.KEYCODE_ZOOM_OUT,
        "zoom_buttons",
        false,
        GestureAction.ZOOM,
    );

    val isTwoWayKey = secondKeycode != null

    companion object {
        /**
         * keycode to ([HardwareKey], first or increase)
         */
        private val ALL_KEYCODES = mutableMapOf<Int, Pair<HardwareKey, Boolean>>().apply {
            for (key in HardwareKey.entries) {
                this[key.firstKeycode] = Pair(key, true)
                key.secondKeycode?.let {
                    this[it] = Pair(key, false)
                }
            }
        }.toMap()

        /**
         * Check if the [keyCode] matches one of the [HardwareKey] and returns
         * ([TwoWayKey, first or increase]).
         *
         * @param keyCode The [KeyEvent] keycode
         */
        fun match(keyCode: Int) = ALL_KEYCODES[keyCode]
    }
}
