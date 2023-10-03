/*
 * SPDX-FileCopyrightText: 2022-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.camera2.interop.CaptureRequestOptions
import org.lineageos.aperture.models.ColorCorrectionAberrationMode
import org.lineageos.aperture.models.DistortionCorrectionMode
import org.lineageos.aperture.models.EdgeMode
import org.lineageos.aperture.models.FrameRate
import org.lineageos.aperture.models.HotPixelMode
import org.lineageos.aperture.models.NoiseReductionMode
import org.lineageos.aperture.models.ShadingMode
import org.lineageos.aperture.models.VideoStabilizationMode

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

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setEdgeMode(edgeMode: EdgeMode) {
    setCaptureRequestOption(
        CaptureRequest.EDGE_MODE,
        when (edgeMode) {
            EdgeMode.OFF -> CameraMetadata.EDGE_MODE_OFF
            EdgeMode.FAST -> CameraMetadata.EDGE_MODE_FAST
            EdgeMode.HIGH_QUALITY -> CameraMetadata.EDGE_MODE_HIGH_QUALITY
            EdgeMode.ZERO_SHUTTER_LAG -> CameraMetadata.EDGE_MODE_ZERO_SHUTTER_LAG
        }
    )
}

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setNoiseReductionMode(noiseReductionMode: NoiseReductionMode) {
    setCaptureRequestOption(
        CaptureRequest.NOISE_REDUCTION_MODE,
        when (noiseReductionMode) {
            NoiseReductionMode.OFF -> CameraMetadata.NOISE_REDUCTION_MODE_OFF
            NoiseReductionMode.FAST -> CameraMetadata.NOISE_REDUCTION_MODE_FAST
            NoiseReductionMode.HIGH_QUALITY -> CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY
            NoiseReductionMode.MINIMAL -> CameraMetadata.NOISE_REDUCTION_MODE_MINIMAL
            NoiseReductionMode.ZERO_SHUTTER_LAG ->
                CameraMetadata.NOISE_REDUCTION_MODE_ZERO_SHUTTER_LAG
        }
    )
}

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setShadingMode(shadingMode: ShadingMode) {
    setCaptureRequestOption(
        CaptureRequest.SHADING_MODE,
        when (shadingMode) {
            ShadingMode.OFF -> CameraMetadata.SHADING_MODE_OFF
            ShadingMode.FAST -> CameraMetadata.SHADING_MODE_FAST
            ShadingMode.HIGH_QUALITY -> CameraMetadata.SHADING_MODE_HIGH_QUALITY
        }
    )
}

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setColorCorrectionAberrationMode(
    colorCorrectionAberrationMode: ColorCorrectionAberrationMode
) {
    setCaptureRequestOption(
        CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE,
        when (colorCorrectionAberrationMode) {
            ColorCorrectionAberrationMode.OFF -> CameraMetadata.COLOR_CORRECTION_ABERRATION_MODE_OFF
            ColorCorrectionAberrationMode.FAST ->
                CameraMetadata.COLOR_CORRECTION_ABERRATION_MODE_FAST

            ColorCorrectionAberrationMode.HIGH_QUALITY ->
                CameraMetadata.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY
        }
    )
}

@RequiresApi(Build.VERSION_CODES.P)
@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setDistortionCorrectionMode(
    distortionCorrectionMode: DistortionCorrectionMode
) {
    setCaptureRequestOption(
        CaptureRequest.DISTORTION_CORRECTION_MODE,
        when (distortionCorrectionMode) {
            DistortionCorrectionMode.OFF -> CameraMetadata.DISTORTION_CORRECTION_MODE_OFF
            DistortionCorrectionMode.FAST -> CameraMetadata.DISTORTION_CORRECTION_MODE_FAST
            DistortionCorrectionMode.HIGH_QUALITY ->
                CameraMetadata.DISTORTION_CORRECTION_MODE_HIGH_QUALITY
        }
    )
}

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun CaptureRequestOptions.Builder.setHotPixel(hotPixelMode: HotPixelMode) {
    setCaptureRequestOption(
        CaptureRequest.HOT_PIXEL_MODE,
        when (hotPixelMode) {
            HotPixelMode.OFF -> CameraMetadata.HOT_PIXEL_MODE_OFF
            HotPixelMode.FAST -> CameraMetadata.HOT_PIXEL_MODE_FAST
            HotPixelMode.HIGH_QUALITY -> CameraMetadata.HOT_PIXEL_MODE_HIGH_QUALITY
        }
    )
}
