/*
 * SPDX-FileCopyrightText: 2023-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

/**
 * Available gesture actions.
 * @param isTwoWayAction Whether this action requires two buttons
 */
enum class GestureAction(
    val isTwoWayAction: Boolean,
) {
    /**
     * Simulate a shutter click.
     */
    SHUTTER(false),

    /**
     * Simulate a focus click.
     */
    FOCUS(false),

    /**
     * Microphone mute during video recording.
     */
    MIC_MUTE(false),

    /**
     * Zoom in or out.
     */
    ZOOM(true),

    /**
     * Let Android handle the key event.
     */
    DEFAULT(false),

    /**
     * Do nothing.
     */
    NOTHING(false),
}
