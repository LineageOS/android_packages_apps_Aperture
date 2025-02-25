/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

import android.view.KeyEvent
import androidx.annotation.StringRes
import org.lineageos.aperture.R

/**
 * Collection of keys that can be used to do things.
 * @param firstKeycode The main [KeyEvent] for this key. If [secondKeycode] is defined, this keycode
 *   will be treated as the increase (or up) keycode
 * @param secondKeycode The [KeyEvent] keycode for decrease (or down)
 * @param supportsDefault Whether it makes sense to let Android handle these keycodes (e.g. volume
 *   control for [HardwareKey.VOLUME])
 * @param defaultAction The default [GestureAction] if the user didn't specify any
 */
enum class HardwareKey(
    val firstKeycode: Int,
    val secondKeycode: Int?,
    val supportsDefault: Boolean,
    val defaultAction: GestureAction,
    @StringRes val actionPreferenceTitleStringResId: Int,
    @StringRes val preferenceCategoryTitleStringResId: Int? = null,
    @StringRes val invertPreferenceTitleStringResId: Int? = null,
    @StringRes val invertPreferenceSummaryStringResId: Int? = null,
) {
    CAMERA(
        KeyEvent.KEYCODE_CAMERA,
        null,
        false,
        GestureAction.SHUTTER,
        R.string.camera_button_action_title,
    ),
    FOCUS(
        KeyEvent.KEYCODE_FOCUS,
        null,
        false,
        GestureAction.FOCUS,
        R.string.focus_button_action_title,
    ),
    MUTE(
        KeyEvent.KEYCODE_MUTE,
        null,
        false,
        GestureAction.MIC_MUTE,
        R.string.mute_button_action_title,
    ),
    VOLUME(
        KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_VOLUME_DOWN,
        true,
        GestureAction.SHUTTER,
        R.string.volume_buttons_action_title,
        R.string.volume_buttons_title,
        R.string.volume_buttons_invert_title,
        R.string.volume_buttons_invert_summary,
    ),
    ZOOM(
        KeyEvent.KEYCODE_ZOOM_IN,
        KeyEvent.KEYCODE_ZOOM_OUT,
        false,
        GestureAction.ZOOM,
        R.string.zoom_buttons_action_title,
        R.string.zoom_buttons_title,
        R.string.zoom_buttons_invert_title,
        R.string.zoom_buttons_invert_summary,
    );

    val isTwoWayKey = secondKeycode != null

    init {
        require(
            !isTwoWayKey || (preferenceCategoryTitleStringResId != null
                    && invertPreferenceTitleStringResId != null
                    && invertPreferenceSummaryStringResId != null)
        )
    }

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
