/*
 * SPDX-FileCopyrightText: 2022-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.os.Build
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ExperimentalLensFacing
import androidx.camera.core.ExperimentalZeroShutterLag
import androidx.camera.core.ImageCapture
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.video.Quality
import androidx.camera.video.Recorder
import androidx.core.util.toClosedRange
import androidx.lifecycle.asFlow
import org.lineageos.aperture.ext.getSupportedModes
import org.lineageos.aperture.repositories.OverlaysRepository
import java.util.SortedMap
import kotlin.reflect.safeCast

/**
 * Class representing a device camera.
 */
@OptIn(
    ExperimentalCamera2Interop::class,
    ExperimentalLensFacing::class,
    ExperimentalZeroShutterLag::class,
)
class Camera private constructor(
    cameraInfo: CameraInfo,
    val logicalZoomRatios: SortedMap<Float, Float>,
    additionalVideoFrameRates: Map<Quality, Map<FrameRate, Boolean>>,
    val supportedExtensionModes: Set<Int>,
) {
    /**
     * The [androidx.camera.core.CameraSelector] for this camera.
     */
    val cameraSelector: CameraSelector = cameraInfo.cameraSelector

    /**
     * The [androidx.camera.camera2.interop.Camera2CameraInfo] of this camera.
     */
    private val camera2CameraInfo: Camera2CameraInfo = Camera2CameraInfo.from(cameraInfo)

    /**
     * Camera2's camera ID.
     */
    val cameraId: String = camera2CameraInfo.cameraId

    /**
     * The [CameraFacing] of this camera.
     */
    val cameraFacing = when (cameraInfo.lensFacing) {
        CameraSelector.LENS_FACING_UNKNOWN -> CameraFacing.UNKNOWN
        CameraSelector.LENS_FACING_FRONT -> CameraFacing.FRONT
        CameraSelector.LENS_FACING_BACK -> CameraFacing.BACK
        CameraSelector.LENS_FACING_EXTERNAL -> CameraFacing.EXTERNAL
        else -> throw Exception("Unknown lens facing value")
    }

    val exposureCompensationRange =
        cameraInfo.exposureState.exposureCompensationRange.toClosedRange<Int>()

    val intrinsicZoomRatio = cameraInfo.intrinsicZoomRatio

    private val imageCaptureCapabilities = ImageCapture.getImageCaptureCapabilities(cameraInfo)

    val supportedPhotoOutputFormats = imageCaptureCapabilities.supportedOutputFormats.map {
        when (it) {
            ImageCapture.OUTPUT_FORMAT_JPEG -> PhotoOutputFormat.JPEG
            ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR -> PhotoOutputFormat.JPEG_ULTRA_HDR
            ImageCapture.OUTPUT_FORMAT_RAW -> PhotoOutputFormat.RAW
            ImageCapture.OUTPUT_FORMAT_RAW_JPEG -> PhotoOutputFormat.RAW_JPEG
            else -> error("Unknown CameraX output format $it")
        }
    }

    private val supportedVideoFrameRates = cameraInfo.supportedFrameRateRanges.mapNotNull {
        FrameRate.fromRange(it.toClosedRange())
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

    val cameraXCameraState = cameraInfo.cameraState.asFlow<CameraState>()

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

        if (cameraInfo.hasFlashUnit()) {
            add(FlashMode.AUTO)
            add(FlashMode.ON)
            add(FlashMode.TORCH)
        }

        if (cameraFacing == CameraFacing.FRONT) {
            add(FlashMode.SCREEN)
        }
    }

    override fun equals(other: Any?) = this::class.safeCast(other)?.let {
        this.cameraId == it.cameraId
    } ?: false

    override fun hashCode() = this::class.qualifiedName.hashCode() + cameraId.hashCode()

    fun supportsExtensionMode(extensionMode: Int): Boolean {
        return supportedExtensionModes.contains(extensionMode)
    }

    fun supportsCameraMode(cameraMode: CameraMode): Boolean {
        return when (cameraMode) {
            CameraMode.VIDEO -> supportedVideoQualities.isNotEmpty()
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