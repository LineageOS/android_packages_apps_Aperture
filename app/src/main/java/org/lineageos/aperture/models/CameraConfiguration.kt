/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

import androidx.camera.core.AspectRatio
import androidx.camera.extensions.ExtensionMode
import androidx.camera.video.Quality

/**
 * Camera configuration. Should only contain information about the session that can only be
 * configured at binding time.
 */
sealed interface CameraConfiguration {
    /**
     * Camera2 interop options.
     *
     * @param edgeMode The [EdgeMode] to use
     * @param noiseReductionMode The [NoiseReductionMode] to use
     * @param shadingMode The [ShadingMode] to use
     * @param colorCorrectionAberrationMode The [ColorCorrectionAberrationMode] to use
     * @param distortionCorrectionMode The [DistortionCorrectionMode] to use
     * @param hotPixelMode The [HotPixelMode] to use
     */
    data class Camera2Options(
        val edgeMode: EdgeMode?,
        val noiseReductionMode: NoiseReductionMode?,
        val shadingMode: ShadingMode?,
        val colorCorrectionAberrationMode: ColorCorrectionAberrationMode?,
        val distortionCorrectionMode: DistortionCorrectionMode?,
        val hotPixelMode: HotPixelMode?,
    ) {
        companion object {
            /**
             * Default instance that will not alter Camera2 options.
             */
            val DEFAULT = Camera2Options(
                edgeMode = null,
                noiseReductionMode = null,
                shadingMode = null,
                colorCorrectionAberrationMode = null,
                distortionCorrectionMode = null,
                hotPixelMode = null,
            )
        }
    }

    /**
     * The [Camera] to use.
     */
    val camera: Camera

    /**
     * The [CameraMode] to use.
     */
    val cameraMode: CameraMode

    /**
     * The [ExtensionMode.Mode] to use.
     */
    @ExtensionMode.Mode
    val extensionMode: Int

    /**
     * The [Camera2Options] to use.
     */
    val camera2Options: Camera2Options

    /**
     * Photo mode configuration.
     *
     * @param photoAspectRatio The [AspectRatio.Ratio] to use
     * @param enableHighResolution Whether to enable high resolution or not
     * @param photoCaptureMode The `ImageCapture.CAPTURE_MODE_*` to use
     * @param photoOutputFormat The [PhotoOutputFormat] to use
     */
    data class Photo(
        override val camera: Camera,
        override val extensionMode: Int,
        override val camera2Options: Camera2Options,
        val photoCaptureMode: Int,
        val photoAspectRatio: Int,
        val enableHighResolution: Boolean,
        val photoOutputFormat: PhotoOutputFormat,
    ) : CameraConfiguration {
        override val cameraMode = CameraMode.PHOTO
    }

    /**
     * Video mode configuration.
     *
     * @param videoQuality The [Quality] to use
     * @param videoFrameRate The [FrameRate] to use
     * @param videoDynamicRange The [VideoDynamicRange] to use
     * @param videoMirrorMode The [VideoMirrorMode] to use
     * @param enableVideoStabilization Whether to enable video stabilization or not
     */
    data class Video(
        override val camera: Camera,
        override val camera2Options: Camera2Options,
        val videoQuality: Quality,
        val videoFrameRate: FrameRate?,
        val videoDynamicRange: VideoDynamicRange,
        val videoMirrorMode: VideoMirrorMode,
        val enableVideoStabilization: Boolean,
    ) : CameraConfiguration {
        override val cameraMode = CameraMode.VIDEO
        override val extensionMode = ExtensionMode.NONE
    }

    /**
     * QR mode configuration.
     */
    data class Qr(
        override val camera: Camera,
    ) : CameraConfiguration {
        override val cameraMode = CameraMode.QR
        override val extensionMode = ExtensionMode.NONE
        override val camera2Options = Camera2Options.DEFAULT
    }
}
