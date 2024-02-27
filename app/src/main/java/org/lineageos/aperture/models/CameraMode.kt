/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

import androidx.annotation.StringRes
import org.lineageos.aperture.R

enum class CameraMode(
    @StringRes val title: Int,
    val supportedFlashModes: Set<FlashMode> = setOf(FlashMode.OFF),
) {
    PHOTO(
        R.string.camera_mode_photo,
        setOf(
            FlashMode.OFF,
            FlashMode.AUTO,
            FlashMode.ON,
            FlashMode.SCREEN,
        ),
    ),
    VIDEO(
        R.string.camera_mode_video,
        setOf(
            FlashMode.OFF,
            FlashMode.TORCH,
        ),
    ),
    QR(R.string.camera_mode_qr),
}
