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
