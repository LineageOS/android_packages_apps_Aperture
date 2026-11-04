/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.repositories

import android.Manifest
import android.content.Context
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
import org.lineageos.aperture.models.CameraFacing
import kotlin.math.abs

/**
 * Repository that provides camera devices.
 */
@androidx.annotation.OptIn(ExperimentalLensFacing::class)
class CameraRepository(
    private val context: Context,
    coroutineScope: CoroutineScope,
    private val overlaysRepository: OverlaysRepository,
) {
    /**
     * CameraX's [ProcessCameraProvider].
     */
    val cameraProvider by lazy {
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
        val cameras = cameraProvider.availableCameraInfos
            .filter { cameraXCameraInfo ->
                cameraXCameraInfo.lensFacing != CameraSelector.LENS_FACING_EXTERNAL
            }
            .mapToCamera()

        cameras.forEach { camera ->
            camera.slowMotionCamera = cameras.slowMotionCameraFor(camera)
        }

        cameras.filter { it.isInternalCameraAllowed() }
    }

    val mainBackCamera by lazy {
        internalCameras.firstOrNull { camera ->
            camera.cameraFacing == CameraFacing.BACK
        }
    }

    val mainFrontCamera by lazy {
        internalCameras.firstOrNull { camera ->
            camera.cameraFacing == CameraFacing.FRONT
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

    /**
     * [camera] when it records slow motion video, else the physical camera behind it that
     * does, closest to its 1x lens.
     */
    private fun List<Camera>.slowMotionCameraFor(camera: Camera) =
        camera.takeIf { it.supportsSlowMotion } ?: filter {
            it.cameraId in camera.physicalCameraIds && it.supportsSlowMotion
        }.minByOrNull { abs(it.intrinsicZoomRatio - 1f) } ?: camera

    private fun Camera.isInternalCameraAllowed(): Boolean {
        return when (cameraId) {
            in mainCameraIds -> true

            else -> {
                val isIgnoredAuxCamera = overlaysRepository.ignoredAuxCameraIds.contains(
                    cameraId
                )
                val isIgnoredLogicalCamera = overlaysRepository.ignoreLogicalAuxCameras
                        && physicalCameraIds.size > 1

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
        /**
         * List of main camera IDs. These should never be excluded.
         */
        private val mainCameraIds = setOf(
            "0",
            "1",
        )
    }
}
