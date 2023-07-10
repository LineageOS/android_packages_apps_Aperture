/*
 * SPDX-FileCopyrightText: 2022-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.os.Build
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.video.Recorder
import org.lineageos.aperture.ext.*
import kotlin.reflect.safeCast

/**
 * Class representing a device camera
 */
@androidx.camera.camera2.interop.ExperimentalCamera2Interop
@androidx.camera.core.ExperimentalLensFacing
@androidx.camera.core.ExperimentalZeroShutterLag
class Camera(cameraInfo: CameraInfo, cameraManager: CameraManager) {
    val cameraSelector = cameraInfo.cameraSelector

    val camera2CameraInfo = Camera2CameraInfo.from(cameraInfo)
    val cameraId = camera2CameraInfo.cameraId

    val cameraFacing = when (cameraInfo.lensFacing) {
        CameraSelector.LENS_FACING_FRONT -> CameraFacing.FRONT
        CameraSelector.LENS_FACING_BACK -> CameraFacing.BACK
        CameraSelector.LENS_FACING_EXTERNAL -> CameraFacing.EXTERNAL
        CameraSelector.LENS_FACING_UNKNOWN -> CameraFacing.UNKNOWN
        else -> throw Exception("Unknown lens facing value")
    }

    val exposureCompensationRange = cameraInfo.exposureState.exposureCompensationRange
    val hasFlashUnit = cameraInfo.hasFlashUnit()

    val isLogical = camera2CameraInfo.physicalCameraIds.size > 1

    val intrinsicZoomRatio = cameraInfo.intrinsicZoomRatio
    val logicalZoomRatios = cameraManager.getLogicalZoomRatios(cameraId)

    private val supportedVideoFrameRates = cameraInfo.supportedFrameRateRanges.mapNotNull {
        FrameRate.fromRange(it)
    }.toSet()
    val supportedVideoQualities =
        Recorder.getVideoCapabilities(cameraInfo).getSupportedQualities(DynamicRange.SDR)
            .associateWith {
                supportedVideoFrameRates + cameraManager.getAdditionalVideoFrameRates(cameraId, it)
            }.toMap()
    val supportsVideoRecording = supportedVideoQualities.isNotEmpty()

    val supportedExtensionModes = cameraManager.extensionsManager.getSupportedModes(cameraSelector)

    val supportedVideoStabilizationModes = mutableListOf(VideoStabilizationMode.OFF).apply {
        val availableVideoStabilizationModes = camera2CameraInfo.getCameraCharacteristic(
            CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES
        ) ?: IntArray(0)

        if (
            availableVideoStabilizationModes.contains(
                CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON
            )
        ) {
            add(VideoStabilizationMode.ON)
        }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            availableVideoStabilizationModes.contains(
                CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
            )
        ) {
            add(VideoStabilizationMode.ON_PREVIEW)
        }
    }.toList()

    val supportsZsl = cameraInfo.isZslSupported

    val cameraState = cameraInfo.cameraState

    val supportedEdgeModes = mutableSetOf<EdgeMode>().apply {
        val availableEdgeModes = camera2CameraInfo.getCameraCharacteristic(
            CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES
        ) ?: IntArray(0)

        availableEdgeModes.toSet().mapNotNullTo(this) {
            when (it) {
                CameraCharacteristics.EDGE_MODE_OFF -> EdgeMode.OFF
                CameraCharacteristics.EDGE_MODE_FAST -> EdgeMode.FAST
                CameraCharacteristics.EDGE_MODE_HIGH_QUALITY -> EdgeMode.HIGH_QUALITY
                CameraCharacteristics.EDGE_MODE_ZERO_SHUTTER_LAG -> EdgeMode.ZERO_SHUTTER_LAG
                else -> null
            }
        }
    }.toSet()

    val supportedNoiseReductionModes = mutableSetOf<NoiseReductionMode>().apply {
        val availableNoiseReductionModes = camera2CameraInfo.getCameraCharacteristic(
            CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES
        ) ?: IntArray(0)

        availableNoiseReductionModes.toSet().mapNotNullTo(this) {
            when (it) {
                CameraCharacteristics.NOISE_REDUCTION_MODE_OFF -> NoiseReductionMode.OFF
                CameraCharacteristics.NOISE_REDUCTION_MODE_FAST -> NoiseReductionMode.FAST
                CameraCharacteristics.NOISE_REDUCTION_MODE_HIGH_QUALITY ->
                    NoiseReductionMode.HIGH_QUALITY
                CameraCharacteristics.NOISE_REDUCTION_MODE_MINIMAL -> NoiseReductionMode.MINIMAL
                CameraCharacteristics.NOISE_REDUCTION_MODE_ZERO_SHUTTER_LAG ->
                    NoiseReductionMode.ZERO_SHUTTER_LAG
                else -> null
            }
        }
    }.toSet()

    val supportedShadingModes = mutableSetOf<ShadingMode>().apply {
        val availableShadingModes = camera2CameraInfo.getCameraCharacteristic(
            CameraCharacteristics.SHADING_AVAILABLE_MODES
        ) ?: IntArray(0)

        availableShadingModes.toSet().mapNotNullTo(this) {
            when (it) {
                CameraCharacteristics.SHADING_MODE_OFF -> ShadingMode.OFF
                CameraCharacteristics.SHADING_MODE_FAST -> ShadingMode.FAST
                CameraCharacteristics.SHADING_MODE_HIGH_QUALITY -> ShadingMode.HIGH_QUALITY
                else -> null
            }
        }
    }.toSet()

    val supportedColorCorrectionAberrationModes =
        mutableSetOf<ColorCorrectionAberrationMode>().apply {
            val availableColorCorrectionAberrationModes = camera2CameraInfo.getCameraCharacteristic(
                CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES
            ) ?: IntArray(0)

            availableColorCorrectionAberrationModes.toSet().mapNotNullTo(this) {
                when (it) {
                    CameraCharacteristics.COLOR_CORRECTION_ABERRATION_MODE_OFF ->
                        ColorCorrectionAberrationMode.OFF
                    CameraCharacteristics.COLOR_CORRECTION_ABERRATION_MODE_FAST ->
                        ColorCorrectionAberrationMode.FAST
                    CameraCharacteristics.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY ->
                        ColorCorrectionAberrationMode.HIGH_QUALITY
                    else -> null
                }
            }
        }.toSet()

    val supportedDistortionCorrectionModes = mutableSetOf<DistortionCorrectionMode>().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val availableDistortionCorrectionModes = camera2CameraInfo.getCameraCharacteristic(
                CameraCharacteristics.DISTORTION_CORRECTION_AVAILABLE_MODES
            ) ?: IntArray(0)

            availableDistortionCorrectionModes.toSet().mapNotNullTo(this) {
                when (it) {
                    CameraCharacteristics.DISTORTION_CORRECTION_MODE_OFF ->
                        DistortionCorrectionMode.OFF
                    CameraCharacteristics.DISTORTION_CORRECTION_MODE_FAST ->
                        DistortionCorrectionMode.FAST
                    CameraCharacteristics.DISTORTION_CORRECTION_MODE_HIGH_QUALITY ->
                        DistortionCorrectionMode.HIGH_QUALITY
                    else -> null
                }
            }
        }
    }.toSet()

    val supportedHotPixelModes = mutableSetOf<HotPixelMode>().apply {
        val availableHotPixelModes = camera2CameraInfo.getCameraCharacteristic(
            CameraCharacteristics.HOT_PIXEL_AVAILABLE_HOT_PIXEL_MODES
        ) ?: IntArray(0)

        availableHotPixelModes.toSet().mapNotNullTo(this) {
            when (it) {
                CameraCharacteristics.HOT_PIXEL_MODE_OFF -> HotPixelMode.OFF
                CameraCharacteristics.HOT_PIXEL_MODE_FAST -> HotPixelMode.FAST
                CameraCharacteristics.HOT_PIXEL_MODE_HIGH_QUALITY -> HotPixelMode.HIGH_QUALITY
                else -> null
            }
        }
    }.toSet()

    override fun equals(other: Any?): Boolean {
        val camera = this::class.safeCast(other) ?: return false
        return this.cameraId == camera.cameraId
    }

    override fun hashCode(): Int {
        return this::class.qualifiedName.hashCode() + cameraId.hashCode()
    }

    fun supportsExtensionMode(extensionMode: Int): Boolean {
        return supportedExtensionModes.contains(extensionMode)
    }

    fun supportsCameraMode(cameraMode: CameraMode): Boolean {
        return when (cameraMode) {
            CameraMode.VIDEO -> supportsVideoRecording
            else -> true
        }
    }
}
