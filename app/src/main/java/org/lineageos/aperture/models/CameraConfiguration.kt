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
    )

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

    fun clone(
        camera: Camera = this.camera,
        cameraMode: CameraMode = this.cameraMode,
        @ExtensionMode.Mode extensionMode: Int = this.extensionMode,
    ): CameraConfiguration

    /**
     * Photo mode configuration.
     *
     * @param photoAspectRatio The [AspectRatio.Ratio] to use
     * @param enableHighResolution Whether to enable high resolution or not
     * @param photoCaptureMode The `ImageCapture.CAPTURE_MODE_*` to use
     */
    data class Photo(
        override val camera: Camera,
        override val extensionMode: Int,
        val photoCaptureMode: Int,
        val photoAspectRatio: Int,
        val enableHighResolution: Boolean,
    ) : CameraConfiguration {
        override val cameraMode = CameraMode.PHOTO

        override fun clone(
            camera: Camera,
            cameraMode: CameraMode,
            @ExtensionMode.Mode extensionMode: Int,
        ) = copy(
            camera = camera,
            extensionMode = extensionMode,
        )
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
        val videoQuality: Quality,
        val videoFrameRate: FrameRate?,
        val videoDynamicRange: VideoDynamicRange,
        val videoMirrorMode: VideoMirrorMode,
        val enableVideoStabilization: Boolean,
    ) : CameraConfiguration {
        override val cameraMode = CameraMode.VIDEO
        override val extensionMode = ExtensionMode.NONE

        override fun clone(
            camera: Camera,
            cameraMode: CameraMode,
            extensionMode: Int,
        ) = copy(
            camera = camera,
        )
    }

    /**
     * QR mode configuration.
     */
    data class Qr(
        override val camera: Camera,
    ) : CameraConfiguration {
        override val cameraMode = CameraMode.QR
        override val extensionMode = ExtensionMode.NONE

        override fun clone(
            camera: Camera,
            cameraMode: CameraMode,
            extensionMode: Int,
        ) = copy(
            camera = camera,
        )
    }
}
