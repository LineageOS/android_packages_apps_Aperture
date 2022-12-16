/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.util.Size
import android.util.SizeF
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraInfo
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import org.lineageos.aperture.getSupportedModes
import org.lineageos.aperture.physicalCameraIds
import org.lineageos.aperture.toSize
import kotlin.reflect.safeCast

/**
 * Class representing a device camera
 */
@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class Camera(cameraInfo: CameraInfo, cameraManager: CameraManager) {
    val cameraSelector = cameraInfo.cameraSelector

    val camera2CameraInfo = Camera2CameraInfo.from(cameraInfo)
    val cameraId = camera2CameraInfo.cameraId

    val cameraFacing =
        when (camera2CameraInfo.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)) {
            CameraCharacteristics.LENS_FACING_FRONT -> CameraFacing.FRONT
            CameraCharacteristics.LENS_FACING_BACK -> CameraFacing.BACK
            CameraCharacteristics.LENS_FACING_EXTERNAL -> CameraFacing.EXTERNAL
            else -> CameraFacing.UNKNOWN
        }

    val exposureCompensationRange = cameraInfo.exposureState.exposureCompensationRange
    val hasFlashUnit = cameraInfo.hasFlashUnit()

    /**
     * A list of sensors this camera is made of.
     * If it contains a single sensor, this means this is a physical camera device,
     * else it's a logical camera device.
     * This list may be empty if information parsing failed (this can happen with
     * external cameras).
     */
    val sensors = mutableListOf<Sensor>().apply {
        val physicalCameraIds = camera2CameraInfo.physicalCameraIds

        if (physicalCameraIds.isNotEmpty()) {
            for (physicalCameraId in physicalCameraIds) {
                runCatching {
                    val camera2CameraCharacteristics =
                        cameraManager.camera2CameraManager.getCameraCharacteristics(
                            physicalCameraId
                        )
                    this@apply.add(Camera2Sensor(camera2CameraCharacteristics))
                }
            }
        } else {
            runCatching { CameraXSensor(camera2CameraInfo) }.getOrNull()?.let {
                add(it)
            }
        }
    }.toList()

    val isLogical = sensors.size > 1

    var intrinsicZoomRatio = 1f
    val logicalZoomRatios = cameraManager.getLogicalZoomRatios(cameraId)

    private val supportedVideoFramerates =
        camera2CameraInfo.getCameraCharacteristic(
            CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES
        )?.mapNotNull { range ->
            Framerate.fromRange(range)
        }?.distinct()?.sorted() ?: listOf()
    val supportedVideoQualities = QualitySelector.getSupportedQualities(cameraInfo).associateWith {
        supportedVideoFramerates + cameraManager.getAdditionalVideoFramerates(cameraId, it)
    }.toSortedMap { a, b ->
        listOf(Quality.SD, Quality.HD, Quality.FHD, Quality.UHD).let {
            it.indexOf(a) - it.indexOf(b)
        }
    }
    val supportsVideoRecording = supportedVideoQualities.isNotEmpty()

    val supportedExtensionModes = cameraManager.extensionsManager.getSupportedModes(cameraSelector)

    val supportedStabilizationModes = mutableListOf(StabilizationMode.OFF).apply {
        val availableVideoStabilizationModes = camera2CameraInfo.getCameraCharacteristic(
            CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES
        ) ?: IntArray(0)
        val availableOpticalStabilization = camera2CameraInfo.getCameraCharacteristic(
            CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION
        ) ?: IntArray(0)

        if (
            availableVideoStabilizationModes.contains(
                CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON
            )
        ) {
            add(StabilizationMode.DIGITAL)
        }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            availableVideoStabilizationModes.contains(
                CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
            )
        ) {
            add(StabilizationMode.HYBRID)
        }
        if (
            availableOpticalStabilization.contains(
                CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON
            )
        ) {
            add(StabilizationMode.OPTICAL)
        }
    }.toList()

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

    private class Camera2Sensor(private val cameraCharacteristics: CameraCharacteristics) : Sensor {
        override val activeArraySize: Size
            get() = cameraCharacteristics.get(
                CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE
            )!!.toSize()
        override val availableFocalLengths: List<Float>
            get() = cameraCharacteristics.get(
                CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
            )!!.toList()
        override val orientation: Int
            get() = cameraCharacteristics.get(
                CameraCharacteristics.SENSOR_ORIENTATION
            )!!
        override val pixelArraySize: Size
            get() = cameraCharacteristics.get(
                CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE
            )!!
        override val size: SizeF
            get() = cameraCharacteristics.get(
                CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
            )!!
    }

    private class CameraXSensor(private val camera2CameraInfo: Camera2CameraInfo) : Sensor {
        override val activeArraySize: Size
            get() = camera2CameraInfo.getCameraCharacteristic(
                CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE
            )!!.toSize()
        override val availableFocalLengths: List<Float>
            get() = camera2CameraInfo.getCameraCharacteristic(
                CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
            )!!.toList()
        override val orientation: Int
            get() = camera2CameraInfo.getCameraCharacteristic(
                CameraCharacteristics.SENSOR_ORIENTATION
            )!!
        override val pixelArraySize: Size
            get() = camera2CameraInfo.getCameraCharacteristic(
                CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE
            )!!
        override val size: SizeF
            get() = camera2CameraInfo.getCameraCharacteristic(
                CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
            )!!
    }
}
