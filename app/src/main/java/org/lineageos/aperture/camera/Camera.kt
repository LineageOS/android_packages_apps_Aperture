/*
 * SPDX-FileCopyrightText: 2022-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.util.Range
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalZeroShutterLag
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.video.Quality
import androidx.camera.video.Recorder
import androidx.lifecycle.LiveData
import org.lineageos.aperture.ext.getSupportedModes
import org.lineageos.aperture.models.CameraFacing
import org.lineageos.aperture.models.CameraMode
import org.lineageos.aperture.models.ColorCorrectionAberrationMode
import org.lineageos.aperture.models.DistortionCorrectionMode
import org.lineageos.aperture.models.EdgeMode
import org.lineageos.aperture.models.FlashMode
import org.lineageos.aperture.models.FrameRate
import org.lineageos.aperture.models.HotPixelMode
import org.lineageos.aperture.models.NoiseReductionMode
import org.lineageos.aperture.models.ShadingMode
import org.lineageos.aperture.models.VideoDynamicRange
import org.lineageos.aperture.models.VideoQualityInfo
import org.lineageos.aperture.models.VideoStabilizationMode
import org.lineageos.aperture.repositories.OverlaysRepository
import java.util.SortedMap

/**
 * Class representing a device camera.
 */
@androidx.annotation.OptIn(ExperimentalCamera2Interop::class, ExperimentalZeroShutterLag::class)
class Camera private constructor(
    cameraInfo: CameraInfo,
    val logicalZoomRatios: SortedMap<Float, Float>,
    additionalVideoFrameRates: Map<Quality, Map<FrameRate, Boolean>>,
    val supportedExtensionModes: Set<Int>,
) : BaseCamera(cameraInfo) {
    override val cameraSelector: CameraSelector = cameraInfo.cameraSelector

    val exposureCompensationRange: Range<Int> = cameraInfo.exposureState.exposureCompensationRange
    private val hasFlashUnit = cameraInfo.hasFlashUnit()

    private val physicalCameras = cameraInfo.physicalCameraInfos.map {
        PhysicalCamera(it)
    }
    val isLogical = physicalCameras.size > 1

    val intrinsicZoomRatio = cameraInfo.intrinsicZoomRatio

    private val supportedVideoFrameRates = cameraInfo.supportedFrameRateRanges.mapNotNull {
        FrameRate.fromRange(it)
    }.toSet()

    private val videoCapabilities = Recorder.getVideoCapabilities(cameraInfo)

    private val supportedVideoDynamicRanges = videoCapabilities.supportedDynamicRanges.map {
        VideoDynamicRange.fromDynamicRange(it)
    }

    private val videoQualityForDynamicRanges = supportedVideoDynamicRanges.associateWith {
        videoCapabilities.getSupportedQualities(it.dynamicRange)
    }

    val supportedVideoQualities =
        videoQualityForDynamicRanges.values.flatten().toSet().associateWith {
            VideoQualityInfo(
                it,
                supportedVideoFrameRates.toMutableSet().apply {
                    additionalVideoFrameRates[it].orEmpty().forEach { (frameRate, remove) ->
                        if (remove) {
                            remove(frameRate)
                        } else {
                            add(frameRate)
                        }
                    }
                }.toSet(),
                videoQualityForDynamicRanges.entries.filter { dynamicRangeToQualities ->
                    dynamicRangeToQualities.value.contains(it)
                }.map { dynamicRangeToQualities -> dynamicRangeToQualities.key }.toSet()
            )
        }

    val supportsVideoRecording = supportedVideoQualities.isNotEmpty()

    val supportedVideoStabilizationModes = buildList {
        add(VideoStabilizationMode.OFF)

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
    }

    val supportsZsl = cameraInfo.isZslSupported

    val cameraState: LiveData<androidx.camera.core.CameraState> = cameraInfo.cameraState

    val supportedEdgeModes = camera2CameraInfo.getAndMapCameraCharacteristics(
        CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES,
    ) {
        when (it) {
            CameraCharacteristics.EDGE_MODE_OFF -> EdgeMode.OFF
            CameraCharacteristics.EDGE_MODE_FAST -> EdgeMode.FAST
            CameraCharacteristics.EDGE_MODE_HIGH_QUALITY -> EdgeMode.HIGH_QUALITY
            CameraCharacteristics.EDGE_MODE_ZERO_SHUTTER_LAG -> EdgeMode.ZERO_SHUTTER_LAG
            else -> null
        }
    }

    val supportedNoiseReductionModes = camera2CameraInfo.getAndMapCameraCharacteristics(
        CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES,
    ) {
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

    val supportedShadingModes = camera2CameraInfo.getAndMapCameraCharacteristics(
        CameraCharacteristics.SHADING_AVAILABLE_MODES,
    ) {
        when (it) {
            CameraCharacteristics.SHADING_MODE_OFF -> ShadingMode.OFF
            CameraCharacteristics.SHADING_MODE_FAST -> ShadingMode.FAST
            CameraCharacteristics.SHADING_MODE_HIGH_QUALITY -> ShadingMode.HIGH_QUALITY
            else -> null
        }
    }

    val supportedColorCorrectionAberrationModes = camera2CameraInfo.getAndMapCameraCharacteristics(
        CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES,
    ) {
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

    val supportedDistortionCorrectionModes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        camera2CameraInfo.getAndMapCameraCharacteristics(
            CameraCharacteristics.DISTORTION_CORRECTION_AVAILABLE_MODES,
        ) {
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
    } else {
        setOf()
    }

    val supportedHotPixelModes = camera2CameraInfo.getAndMapCameraCharacteristics(
        CameraCharacteristics.HOT_PIXEL_AVAILABLE_HOT_PIXEL_MODES,
    ) {
        when (it) {
            CameraCharacteristics.HOT_PIXEL_MODE_OFF -> HotPixelMode.OFF
            CameraCharacteristics.HOT_PIXEL_MODE_FAST -> HotPixelMode.FAST
            CameraCharacteristics.HOT_PIXEL_MODE_HIGH_QUALITY -> HotPixelMode.HIGH_QUALITY
            else -> null
        }
    }

    /**
     * The supported flash modes of this camera.
     * Keep in mind that support also depends on the camera mode used.
     */
    val supportedFlashModes = buildSet {
        add(FlashMode.OFF)

        if (hasFlashUnit) {
            add(FlashMode.AUTO)
            add(FlashMode.ON)
            add(FlashMode.TORCH)
        }

        if (cameraFacing == CameraFacing.FRONT) {
            add(FlashMode.SCREEN)
        }
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

    private inline fun <T : Enum<T>> Camera2CameraInfo.getAndMapCameraCharacteristics(
        key: CameraCharacteristics.Key<IntArray>,
        mapper: (Int) -> T?,
    ): Set<T> = getCameraCharacteristic(key)?.toSet().orEmpty().mapNotNull {
        mapper(it)
    }.toSet()

    companion object {
        fun fromCameraX(
            cameraXCameraInfo: CameraInfo,
            extensionsManager: ExtensionsManager,
            overlaysRepository: OverlaysRepository,
        ): Camera {
            val cameraId = Camera2CameraInfo.from(cameraXCameraInfo).cameraId

            val logicalZoomRatios = buildMap {
                put(1f, 1f)
                overlaysRepository.logicalZoomRatios[cameraId]?.let {
                    putAll(it)
                }
            }.toSortedMap()
            val additionalVideoFrameRates =
                overlaysRepository.additionalVideoConfigurations[cameraId].orEmpty()
            val supportedExtensionModes = extensionsManager.getSupportedModes(
                cameraXCameraInfo.cameraSelector
            )

            return Camera(
                cameraXCameraInfo,
                logicalZoomRatios,
                additionalVideoFrameRates,
                supportedExtensionModes,
            )
        }
    }
}
