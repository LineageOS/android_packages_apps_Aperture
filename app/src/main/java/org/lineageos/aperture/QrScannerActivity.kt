/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import org.lineageos.aperture.models.CameraMode

class QrScannerActivity : CameraActivity() {
    override fun overrideInitialCameraMode() = CameraMode.QR
}
