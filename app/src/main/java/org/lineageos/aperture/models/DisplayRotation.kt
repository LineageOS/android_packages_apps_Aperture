/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

/**
 * Rotation of the display the UI is drawn on.
 *
 * @property rotation The display rotation compared to its natural orientation
 * @property followsDevice Whether the system keeps the display aligned with the device
 */
data class DisplayRotation(
    val rotation: Rotation = Rotation.ROTATION_0,
    val followsDevice: Boolean = false,
)
