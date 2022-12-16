/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.content.Context
import android.icu.text.DecimalFormat
import android.icu.text.DecimalFormatSymbols
import android.hardware.camera2.CameraManager as Camera2CameraManager
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.view.LifecycleCameraController
import org.lineageos.aperture.R
import org.lineageos.aperture.getBoolean
import org.lineageos.aperture.getStringArray
import java.math.RoundingMode
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Class managing an app camera session
 */
@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class CameraManager(context: Context) {
    val camera2CameraManager: Camera2CameraManager =
        context.getSystemService(Camera2CameraManager::class.java)

    private val cameraProvider = ProcessCameraProvider.getInstance(context).get()
    val extensionsManager = ExtensionsManager.getInstanceAsync(context, cameraProvider).get()!!
    val cameraController = LifecycleCameraController(context)
    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val additionalVideoConfigurations by lazy {
        mutableMapOf<String, MutableMap<Quality, MutableList<Framerate>>>().apply {
            context.resources.getStringArray(context, R.array.config_additionalVideoConfigurations)
                .let {
                    if (it.size % 3 != 0) {
                        // Invalid configuration
                        return@apply
                    }

                    for (i in it.indices step 3) {
                        val cameraId = it[i]
                        val framerates = it[i + 2].split("|").mapNotNull {
                            Framerate.fromValue(it.toInt())
                        }

                        it[i + 1].split("|").mapNotNull {
                            when (it) {
                                "sd" -> Quality.SD
                                "hd" -> Quality.HD
                                "fhd" -> Quality.FHD
                                "uhd" -> Quality.UHD
                                else -> null
                            }
                        }.distinct().forEach {
                            if (!this.containsKey(cameraId)) {
                                this[cameraId] = mutableMapOf()
                            }
                            if (!this[cameraId]!!.containsKey(it)) {
                                this[cameraId]!![it] = mutableListOf()
                            }
                            this[cameraId]!![it]!!.addAll(framerates)
                        }
                    }
                }
        }.map { a ->
            a.key to a.value.map { b ->
                b.key to b.value.toList()
            }.toMap()
        }.toMap()
    }
    private val enableAuxCameras by lazy {
        context.resources.getBoolean(context, R.bool.config_enableAuxCameras)
    }
    private val ignoredAuxCameraIds by lazy {
        context.resources.getStringArray(context, R.array.config_ignoredAuxCameraIds)
    }
    private val ignoreLogicalAuxCameras by lazy {
        context.resources.getBoolean(context, R.bool.config_ignoreLogicalAuxCameras)
    }
    private val logicalZoomRatios by lazy {
        mutableMapOf<String, MutableMap<Float, Float>>().apply {
            context.resources.getStringArray(context, R.array.config_logicalZoomRatios).let {
                if (it.size % 3 != 0) {
                    // Invalid configuration
                    return@apply
                }

                for (i in it.indices step 3) {
                    val cameraId = it[i]
                    val approximateZoomRatio = it[i + 1].toFloat()
                    val exactZoomRatio = it[i + 2].toFloat()

                    if (!this.containsKey(cameraId)) {
                        this[cameraId] = mutableMapOf()
                    }
                    this[cameraId]!![approximateZoomRatio] = exactZoomRatio
                }
            }
        }.map { a ->
            a.key to a.value.toMap()
        }.toMap()
    }

    private val cameras: Map<String, Camera>
        get() = cameraProvider.availableCameraInfos.associate {
            val camera = Camera(it, this)
            camera.cameraId to camera
        }

    // We expect device cameras to never change
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

    val internalCamerasSupportingVideoRecoding =
        backCamerasSupportingVideoRecording + frontCamerasSupportingVideoRecording

    private val externalCameras: List<Camera>
        get() = cameras.values.filter {
            it.cameraFacing == CameraFacing.EXTERNAL
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

    fun getAdditionalVideoFramerates(cameraId: String, quality: Quality) =
        additionalVideoConfigurations[cameraId]?.get(quality) ?: listOf()

    fun getLogicalZoomRatios(cameraId: String) = mutableMapOf(1.0f to 1.0f).apply {
        logicalZoomRatios[cameraId]?.let {
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

        if (!enableAuxCameras) {
            // Return only the main camera
            return listOf(mainCamera)
        }

        // Get the list of aux cameras
        val auxCameras = facingCameras
            .drop(1)
            .filter { !ignoreLogicalAuxCameras || !it.isLogical }

        // Setup zoom ratio for aux cameras if main cam is a physical camera device
        if (mainCamera.sensors.size == 1) {
            val mainSensorViewAngleDegrees = mainCamera.sensors[0].viewAngleDegrees.toFloat()

            for (camera in auxCameras) {
                // Setup zoom ratio only for physical camera devices
                if (camera.sensors.size == 1) {
                    val auxSensor = camera.sensors[0]
                    camera.intrinsicZoomRatio = roundOffZoomRatio(
                        mainSensorViewAngleDegrees / auxSensor.viewAngleDegrees
                    )
                }
            }
        }

        return listOf(mainCamera) + auxCameras
    }

    private fun roundOffZoomRatio(number: Float): Float {
        val symbols = DecimalFormatSymbols(Locale.US)
        return DecimalFormat("#.#", symbols).apply {
            roundingMode = RoundingMode.CEILING.ordinal
        }.format(number).toFloat()
    }
}
