/*
 * Copyright (C) 2022 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import androidx.appcompat.app.AppCompatActivity
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.LifecycleCameraController
import org.lineageos.aperture.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Class managing an app camera session
 */
@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class CameraManager(activity: AppCompatActivity) {
    val cameraProvider: ProcessCameraProvider = ProcessCameraProvider.getInstance(activity).get()
    val extensionsManager: ExtensionsManager = ExtensionsManager.getInstanceAsync(
        activity, cameraProvider
    ).get()
    val cameraController = LifecycleCameraController(activity)
    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    val ignoredAuxCameraIds by lazy {
        activity.resources.getStringArray(R.array.config_ignoredAuxCameraIds)
    }

    val cameras: Map<Int, Camera>
        get() = cameraProvider.availableCameraInfos.associate {
            val camera = Camera(it, this)
            camera.cameraId to camera
        }

    // We expect device cameras to never change
    val backCameras = prepareDeviceCamerasList(CameraFacing.BACK)
    val mainBackCamera = backCameras.firstOrNull()
    val backCamerasSupportingVideoRecording = backCameras.filter { it.supportsVideoRecording }

    val frontCameras = prepareDeviceCamerasList(CameraFacing.FRONT)
    val mainFrontCamera = frontCameras.firstOrNull()
    val frontCamerasSupportingVideoRecording = frontCameras.filter { it.supportsVideoRecording }

    val externalCameras: List<Camera>
        get() = cameras.values.filter {
            it.cameraFacing == CameraFacing.EXTERNAL
        }
    val externalCamerasSupportingVideoRecording: List<Camera>
        get() = externalCameras.filter { it.supportsVideoRecording }

    // Google recommends cycling between all externals, back and front
    // We're gonna do back, front and all externals instead, makes more sense
    val availableCameras: List<Camera>
        get() = mutableListOf<Camera>().apply {
            mainBackCamera?.let {
                add(it)
            }
            mainFrontCamera?.let {
                add(it)
            }
            addAll(externalCameras)
        }
    val availableCamerasSupportingVideoRecording: List<Camera>
        get() = availableCameras.filter { it.supportsVideoRecording }

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

    fun getCameraOfFacingOrFirstAvailable(
        cameraFacing: CameraFacing, cameraMode: CameraMode
    ): Camera {
        val camera = when (cameraFacing) {
            CameraFacing.BACK -> mainBackCamera
            CameraFacing.FRONT -> mainFrontCamera
            CameraFacing.EXTERNAL -> externalCameras.firstOrNull()
            else -> throw Exception("Unknown facing")
        }
        return camera?.let {
            if (cameraMode == CameraMode.VIDEO && !it.supportsVideoRecording) {
                availableCamerasSupportingVideoRecording.first()
            } else {
                it
            }
        } ?: when (cameraMode) {
            CameraMode.VIDEO -> availableCamerasSupportingVideoRecording.first()
            else -> availableCameras.first()
        }
    }

    fun getNextCamera(camera: Camera, cameraMode: CameraMode): Camera {
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
            cameras.first()
        } else {
            cameras[newCameraIndex]
        }
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }

    private fun prepareDeviceCamerasList(cameraFacing: CameraFacing): List<Camera> {
        val facingCameras = cameras.values.filter {
            it.cameraFacing == cameraFacing && !ignoredAuxCameraIds.contains(it.cameraId)
        }

        if (facingCameras.isEmpty()) {
            return listOf()
        }

        val mainCamera = facingCameras.first()
        if (mainCamera.isLogical && mainCamera.focalLengths.size >= 2) {
            // If first camera is logical and it has more focal lengths,
            // it's very likely that it merges all sensors and handles
            // them with zoom (e.g. Pixels). Just expose only that
            return listOf(mainCamera)
        }

        // Get rid of logical cameras, we want single sensor cameras for now
        val auxCameras = facingCameras.drop(1).filter { !it.isLogical }
        // Setup zoom ratio for aux cameras
        mainCamera.mm35FocalLengths?.getOrNull(0)?.let { mainCameraMm35FocalLength ->
            for (camera in auxCameras) {
                camera.mm35FocalLengths?.getOrNull(0)?.let {
                    camera.zoomRatio = it / mainCameraMm35FocalLength
                }
            }
        }

        return listOf(mainCamera) + auxCameras
    }
}
