/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.interop.CaptureRequestOptions
import org.lineageos.aperture.utils.Framerate

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setFramerate(framerate: Framerate) {
    framerate.range?.let {
        setCaptureRequestOption(
            CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, it
        )
    } ?: run {
        clearCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE)
    }
}
