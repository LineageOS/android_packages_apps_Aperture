/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.repositories

import android.Manifest
import android.content.Context
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalLensFacing
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import org.lineageos.aperture.ext.permissionGranted
import org.lineageos.aperture.models.Camera

/**
 * Repository that provides camera devices.
 */
@androidx.annotation.OptIn(ExperimentalCamera2Interop::class, ExperimentalLensFacing::class)
class CameraRepository(
    private val context: Context,
    coroutineScope: CoroutineScope,
    private val overlaysRepository: OverlaysRepository,
) {
    /**
     * CameraX's [ProcessCameraProvider].
     */
    private val cameraProvider by lazy {
        requireCameraPermission()
        ProcessCameraProvider.getInstance(context).get()
    }

    /**
     * CameraX's [ExtensionsManager].
     */
    private val extensionsManager: ExtensionsManager by lazy {
        ExtensionsManager.getInstanceAsync(context, cameraProvider).get()
    }

    /**
     * List of internal cameras. These should never change.
     */
    private val internalCameras by lazy {
        cameraProvider.availableCameraInfos
            .filter { cameraXCameraInfo ->
                cameraXCameraInfo.lensFacing != CameraSelector.LENS_FACING_EXTERNAL
                        && cameraXCameraInfo.isInternalCameraAllowed()
            }
            .mapToCamera()
    }

    val mainBackCamera by lazy {
        internalCameras.firstOrNull { camera ->
            camera.cameraId == DEFAULT_BACK_CAMERA_ID
        }
    }

    val mainFrontCamera by lazy {
        internalCameras.firstOrNull { camera ->
            camera.cameraId == DEFAULT_FRONT_CAMERA_ID
        }
    }

    /**
     * List of external cameras. These will change once the user connects or disconnects a camera.
     */
    private val externalCameras = suspend {
        cameraProvider.availableCameraInfos
            .filter { cameraXCameraInfo ->
                cameraXCameraInfo.lensFacing == CameraSelector.LENS_FACING_EXTERNAL
            }
            .mapToCamera()
    }.asFlow()
        .flowOn(Dispatchers.IO)
        .shareIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(),
            replay = 1,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val cameras = externalCameras.mapLatest { externalCameras ->
        internalCameras + externalCameras
    }
        .flowOn(Dispatchers.IO)
        .shareIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(),
            replay = 1,
        )

    /**
     * @see ExtensionsManager.getExtensionEnabledCameraSelector
     */
    fun getExtensionEnabledCameraSelector(
        camera: Camera,
        mode: Int,
    ): CameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
        camera.cameraSelector, mode
    )

    private fun CameraInfo.isInternalCameraAllowed(): Boolean {
        val camera2CameraInfo = Camera2CameraInfo.from(this)

        return when (camera2CameraInfo.cameraId) {
            DEFAULT_BACK_CAMERA_ID -> true.also {
                require(lensFacing == CameraSelector.LENS_FACING_BACK) {
                    "Camera with ID ${camera2CameraInfo.cameraId} is not a back camera"
                }
            }

            DEFAULT_FRONT_CAMERA_ID -> true.also {
                require(lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    "Camera with ID ${camera2CameraInfo.cameraId} is not a front camera"
                }
            }

            else -> {
                val isIgnoredAuxCamera = overlaysRepository.ignoredAuxCameraIds.contains(
                    camera2CameraInfo.cameraId
                )
                val isIgnoredLogicalCamera = overlaysRepository.ignoreLogicalAuxCameras
                        && physicalCameraInfos.size > 1

                overlaysRepository.enableAuxCameras
                        && !isIgnoredAuxCamera
                        && !isIgnoredLogicalCamera
            }
        }
    }

    private fun List<CameraInfo>.mapToCamera() = map { it.toCamera() }.sortedBy { it.cameraId }

    private fun CameraInfo.toCamera() = Camera.fromCameraX(
        this,
        extensionsManager,
        overlaysRepository,
    )

    private fun requireCameraPermission() = require(
        context.permissionGranted(Manifest.permission.CAMERA)
    ) { "Camera permission not granted" }

    companion object {
        private const val DEFAULT_BACK_CAMERA_ID = "0"
        private const val DEFAULT_FRONT_CAMERA_ID = "1"
    }
}
