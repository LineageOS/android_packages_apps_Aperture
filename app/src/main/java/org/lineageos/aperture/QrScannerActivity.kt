/*
 * Copyright (C) 2022 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.os.Bundle
import org.lineageos.aperture.utils.CameraMode

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class QrScannerActivity : CameraActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        changeCameraMode(CameraMode.QR)
    }
}
