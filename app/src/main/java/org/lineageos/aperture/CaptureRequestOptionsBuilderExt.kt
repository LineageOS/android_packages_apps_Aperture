/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.camera.camera2.interop.CaptureRequestOptions
import org.lineageos.aperture.utils.Framerate
import org.lineageos.aperture.utils.StabilizationMode

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setFramerate(framerate: Framerate?) {
    framerate?.let {
        setCaptureRequestOption(
            CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, it.range
        )
    } ?: run {
        clearCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE)
    }
}

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setStabilizationMode(stabilizationMode: StabilizationMode) {
    setCaptureRequestOption(
        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
        when (stabilizationMode) {
            StabilizationMode.DIGITAL -> CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON
            StabilizationMode.HYBRID ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
                } else {
                    CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON
                }
            else -> CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF
        }
    )

    setCaptureRequestOption(
        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
        if (stabilizationMode == StabilizationMode.OPTICAL) {
            CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON
        } else {
            CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF
        }
    )
}
