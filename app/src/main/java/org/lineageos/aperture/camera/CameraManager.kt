/*
 * SPDX-FileCopyrightText: 2022-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.camera

import android.content.Context
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.view.LifecycleCameraController
import org.lineageos.aperture.models.CameraFacing
import org.lineageos.aperture.models.CameraMode
import org.lineageos.aperture.models.CameraType
import org.lineageos.aperture.utils.OverlayConfiguration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Class managing an app camera session
 */
@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class CameraManager(context: Context) {
    private val cameraProvider = ProcessCameraProvider.getInstance(context).get()
    val extensionsManager: ExtensionsManager =
        ExtensionsManager.getInstanceAsync(context, cameraProvider).get()
    val cameraController = LifecycleCameraController(context)
    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    val overlayConfiguration = OverlayConfiguration(context)

    private val cameras: List<Camera>
        get() = cameraProvider.availableCameraInfos.map {
            Camera(it, this)
        }.sortedBy { it.cameraId }

    // We expect device cameras to never change
    private val internalCameras = cameras.filter {
        it.cameraType == CameraType.INTERNAL
                && !overlayConfiguration.ignoredAuxCameraIds.contains(it.cameraId)
    }

    private val backCameras = prepareDeviceCamerasList(CameraFacing.BACK)
    private val mainBackCamera = backCameras.firstOrNull()
    private val backCamerasSupportingVideoRecording = backCameras.filter {
        it.supportsVideoRecording
    }

    private val frontCameras = prepareDeviceCamerasList(CameraFacing.FRONT)
    private val mainFrontCamera = frontCameras.firstOrNull()
    private val frontCamerasSupportingVideoRecording = frontCameras.filter {
        it.supportsVideoRecording
    }

    private val externalCameras: List<Camera>
        get() = cameras.filter {
            it.cameraType == CameraType.EXTERNAL
        }
    private val externalCamerasSupportingVideoRecording: List<Camera>
        get() = externalCameras.filter { it.supportsVideoRecording }

    // Google recommends cycling between all externals, back and front
    // We're gonna do back, front and all externals instead, makes more sense
    private val availableCameras: List<Camera>
        get() = mutableListOf<Camera>().apply {
            mainBackCamera?.let {
                add(it)
            }
            mainFrontCamera?.let {
                add(it)
            }
            addAll(externalCameras)
        }
    private val availableCamerasSupportingVideoRecording: List<Camera>
        get() = availableCameras.filter { it.supportsVideoRecording }

    fun getAdditionalVideoFrameRates(cameraId: String, quality: Quality) =
        overlayConfiguration.additionalVideoConfigurations[cameraId]?.get(quality) ?: setOf()

    fun getLogicalZoomRatios(cameraId: String) = mutableMapOf(1.0f to 1.0f).apply {
        overlayConfiguration.logicalZoomRatios[cameraId]?.let {
            putAll(it)
        }
    }.toSortedMap()

    fun getCameras(
        cameraMode: CameraMode, cameraFacing: CameraFacing,
    ): List<Camera> {
        return when (cameraMode) {
            CameraMode.VIDEO -> when (cameraFacing) {
                CameraFacing.BACK -> backCamerasSupportingVideoRecording
                CameraFacing.FRONT -> frontCamerasSupportingVideoRecording
                CameraFacing.EXTERNAL -> externalCamerasSupportingVideoRecording
                else -> throw Exception("Unknown facing")
            }

            else -> when (cameraFacing) {
                CameraFacing.BACK -> backCameras
                CameraFacing.FRONT -> frontCameras
                CameraFacing.EXTERNAL -> externalCameras
                else -> throw Exception("Unknown facing")
            }
        }
    }

    /**
     * Get a suitable [Camera] for the provided [CameraFacing] and [CameraMode].
     * @param cameraFacing The requested [CameraFacing]
     * @param cameraMode The requested [CameraMode]
     * @return A [Camera] that is compatible with the provided configuration or null
     */
    fun getCameraOfFacingOrFirstAvailable(
        cameraFacing: CameraFacing, cameraMode: CameraMode
    ): Camera? {
        val camera = when (cameraFacing) {
            CameraFacing.BACK -> mainBackCamera
            CameraFacing.FRONT -> mainFrontCamera
            CameraFacing.EXTERNAL -> externalCameras.firstOrNull()
            else -> throw Exception("Unknown facing")
        }
        return camera?.let {
            if (cameraMode == CameraMode.VIDEO && !it.supportsVideoRecording) {
                availableCamerasSupportingVideoRecording.firstOrNull()
            } else {
                it
            }
        } ?: when (cameraMode) {
            CameraMode.VIDEO -> availableCamerasSupportingVideoRecording.firstOrNull()
            else -> availableCameras.firstOrNull()
        }
    }

    /**
     * Return the next camera, used for flip camera.
     * @param camera The current [Camera] used
     * @param cameraMode The current [CameraMode]
     * @return The next camera, may return null if all the cameras disappeared
     */
    fun getNextCamera(camera: Camera, cameraMode: CameraMode): Camera? {
        val cameras = when (cameraMode) {
            CameraMode.VIDEO -> availableCamerasSupportingVideoRecording
            else -> availableCameras
        }

        // If value is -1 it will just pick the first available camera
        // This should only happen when an external camera is disconnected
        val newCameraIndex = cameras.indexOf(
            when (camera.cameraFacing) {
                CameraFacing.BACK -> mainBackCamera
                CameraFacing.FRONT -> mainFrontCamera
                CameraFacing.EXTERNAL -> camera
                else -> throw Exception("Unknown facing")
            }
        ) + 1

        return if (newCameraIndex >= cameras.size) {
            cameras.firstOrNull()
        } else {
            cameras[newCameraIndex]
        }
    }

    fun videoRecordingAvailable() = availableCamerasSupportingVideoRecording.isNotEmpty()

    fun shutdown() {
        cameraExecutor.shutdown()
    }

    private fun prepareDeviceCamerasList(cameraFacing: CameraFacing): List<Camera> {
        val facingCameras = internalCameras.filter {
            it.cameraFacing == cameraFacing
        }

        if (facingCameras.isEmpty()) {
            return listOf()
        }

        val mainCamera = facingCameras.first()

        if (!overlayConfiguration.enableAuxCameras) {
            // Return only the main camera
            return listOf(mainCamera)
        }

        // Get the list of aux cameras
        val auxCameras = facingCameras
            .drop(1)
            .filter { !overlayConfiguration.ignoreLogicalAuxCameras || !it.isLogical }

        return listOf(mainCamera) + auxCameras
    }
}
