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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Class managing an app camera session
 */
@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class CameraManager(private val activity: AppCompatActivity) {
    val cameraProvider: ProcessCameraProvider = ProcessCameraProvider.getInstance(activity).get()
    val extensionsManager: ExtensionsManager = ExtensionsManager.getInstanceAsync(
        activity, cameraProvider).get()
    val cameraController = LifecycleCameraController(activity)
    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    val cameras: Map<Int, Camera>
        get() {
            return cameraProvider.availableCameraInfos.associate {
                val camera = Camera(it, this)
                camera.cameraId to camera
            }
        }

    // We expect device cameras to never change
    val backCameras = prepareDeviceCamerasList(CameraFacing.BACK)
    val mainBackCamera = backCameras.firstOrNull()

    val frontCameras = prepareDeviceCamerasList(CameraFacing.FRONT)
    val mainFrontCamera = frontCameras.firstOrNull()

    val externalCameras: List<Camera>
        get() {
            return cameras.values.filter {
                it.cameraFacing == CameraFacing.EXTERNAL
            }
        }

    // Google recommends cycling between all externals, back and front
    // We're gonna do back, front and all externals instead, makes more sense
    val availableCameras: List<Camera>
        get() {
            return mutableListOf<Camera>().apply {
                mainBackCamera?.let {
                    add(it)
                }
                mainFrontCamera?.let {
                    add(it)
                }
                addAll(externalCameras)
            }
        }

    val firstAvailableCamera: Camera
        get() {
            return availableCameras.first()
        }

    fun getCameraOfFacingOrFirstAvailable(cameraFacing: CameraFacing): Camera {
        return when (cameraFacing) {
            CameraFacing.BACK -> mainBackCamera
            CameraFacing.FRONT -> mainFrontCamera
            CameraFacing.EXTERNAL -> externalCameras.firstOrNull()
            else -> throw Exception("Unknown facing")
        } ?: firstAvailableCamera
    }

    fun getNextCamera(camera: Camera): Camera {
        // If value is -1 it will just pick the first available camera
        // This should only happen when an external camera is disconnected
        val newCameraIndex = availableCameras.indexOf(when (camera.cameraFacing) {
            CameraFacing.BACK -> mainBackCamera
            CameraFacing.FRONT -> mainFrontCamera
            CameraFacing.EXTERNAL -> camera
            else -> throw Exception("Unknown facing")
        }) + 1

        return if (newCameraIndex >= availableCameras.size)
            firstAvailableCamera
        else availableCameras[newCameraIndex]
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }

    private fun prepareDeviceCamerasList(cameraFacing: CameraFacing): List<Camera> {
        val facingCameras = cameras.values.filter {
            it.cameraFacing == cameraFacing
        }

        if (facingCameras.isEmpty()) {
            return listOf()
        }

        val mainCamera = facingCameras.first()
        if (mainCamera.isLogical) {
            // If first camera is logical, it's very likely that it merges all sensors and handles
            // them with zoom (e.g. Pixels). Just expose only that
            return listOf(mainCamera)
        }

        // Get rid of logical cameras, we want single sensor cameras for now
        val physicalCameras = facingCameras.filter { !it.isLogical }
        val auxCameras = physicalCameras.drop(1)
        for (camera in auxCameras) {
            camera.zoomRatio = camera.mm35FocalLength / mainCamera.mm35FocalLength
        }

        return physicalCameras
    }
}
