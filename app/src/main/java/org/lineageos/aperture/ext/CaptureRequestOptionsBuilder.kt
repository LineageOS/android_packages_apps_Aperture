/*
 * SPDX-FileCopyrightText: 2022-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.camera.camera2.interop.CaptureRequestOptions
import org.lineageos.aperture.camera.FrameRate
import org.lineageos.aperture.camera.VideoStabilizationMode

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setFrameRate(frameRate: FrameRate?) {
    frameRate?.let {
        setCaptureRequestOption(
            CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, it.range
        )
    } ?: run {
        clearCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE)
    }
}

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setVideoStabilizationMode(videoStabilizationMode: VideoStabilizationMode) {
    setCaptureRequestOption(
        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
        when (videoStabilizationMode) {
            VideoStabilizationMode.OFF -> CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF
            VideoStabilizationMode.ON -> CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON
            VideoStabilizationMode.ON_PREVIEW ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
                } else {
                    CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON
                }
        }
    )
}
