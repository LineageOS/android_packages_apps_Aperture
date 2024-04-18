/*
 * SPDX-FileCopyrightText: 2023-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.net.Uri
import androidx.camera.camera2.internal.CameraIdUtil
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.Recording
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import org.lineageos.aperture.camera.Camera
import org.lineageos.aperture.ext.*
import org.lineageos.aperture.models.CameraFacing
import org.lineageos.aperture.models.CameraMode
import org.lineageos.aperture.models.CameraState
import org.lineageos.aperture.models.CameraType
import org.lineageos.aperture.models.FlashMode
import org.lineageos.aperture.models.FrameRate
import org.lineageos.aperture.models.GridMode
import org.lineageos.aperture.models.Rotation
import org.lineageos.aperture.models.TimerMode
import org.lineageos.aperture.models.VideoDynamicRange
import org.lineageos.aperture.repository.MediaRepository
import org.lineageos.aperture.utils.OverlayConfiguration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * [ViewModel] representing a camera session. This data is used to receive
 * live data regarding the setting currently enabled.
 */
@SuppressLint("RestrictedApi")
@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class CameraViewModel(application: Application) : AndroidViewModel(application) {
    // Base

    /**
     * Overlay configuration.
     */
    val overlayConfiguration = OverlayConfiguration(context)

    init {
        // We need to set the backward compatible camera IDs initializing CameraX
        CameraIdUtil.setBackwardCompatibleCameraIds(
            overlayConfiguration.backwardCompatibleCameraIds
        )
    }

    /**
     * CameraX's [ProcessCameraProvider].
     */
    private val cameraProvider = ProcessCameraProvider.getInstance(context).get()

    /**
     * CameraX's [ExtensionsManager].
     */
    val extensionsManager: ExtensionsManager =
        ExtensionsManager.getInstanceAsync(context, cameraProvider).get()

    /**
     * [ExecutorService] for camera related operations.
     */
    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /**
     * The available [Camera]s.
     */
    private val cameras: List<Camera>
        get() = cameraProvider.availableCameraInfos.map {
            Camera(it, this)
        }.sortedBy { it.cameraId }

    /**
     * List of internal [Camera]s.
     * We expect device cameras to never change.
     */
    private val internalCameras = cameras.filter {
        it.cameraType == CameraType.INTERNAL
                && !overlayConfiguration.ignoredAuxCameraIds.contains(it.cameraId)
    }

    /**
     * The list of internal back [Camera]s.
     */
    private val backCameras = prepareDeviceCamerasList(CameraFacing.BACK)

    /**
     * The main back camera, equals to the first one, usually ID 0.
     */
    private val mainBackCamera = backCameras.firstOrNull()

    /**
     * The list of internal back [Camera]s supporting video recording.
     */
    private val backCamerasSupportingVideoRecording = backCameras.filter {
        it.supportsVideoRecording
    }

    /**
     * The list of internal front [Camera]s.
     */
    private val frontCameras = prepareDeviceCamerasList(CameraFacing.FRONT)

    /**
     * The main front camera, equals to the first one, usually ID 1.
     */
    private val mainFrontCamera = frontCameras.firstOrNull()

    /**
     * The list of internal front [Camera]s supporting video recording.
     */
    private val frontCamerasSupportingVideoRecording = frontCameras.filter {
        it.supportsVideoRecording
    }

    /**
     * The list of external [Camera]s.
     * Expected to change, do not store this anywhere.
     */
    private val externalCameras: List<Camera>
        get() = cameras.filter {
            it.cameraType == CameraType.EXTERNAL
        }

    /**
     * The list of external [Camera]s supporting video recording.
     * Expected to change, do not store this anywhere.
     */
    private val externalCamerasSupportingVideoRecording: List<Camera>
        get() = externalCameras.filter { it.supportsVideoRecording }

    /**
     * The list of [Camera]s to use for cycling.
     * Google recommends cycling between all externals, back and front,
     * we do back, front and all externals instead, makes more sense.
     * Expected to change, do not store this anywhere.
     */
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

    /**
     * The list of [Camera]s that supports video recording to use for cycling.
     * Google recommends cycling between all externals, back and front,
     * we do back, front and all externals instead, makes more sense.
     * Expected to change, do not store this anywhere.
     */
    private val availableCamerasSupportingVideoRecording: List<Camera>
        get() = availableCameras.filter { it.supportsVideoRecording }

    /**
     * The camera currently in use.
     */
    val camera = MutableLiveData<Camera>()

    /**
     * Current camera mode.
     */
    val cameraMode = MutableLiveData<CameraMode>()

    /**
     * Whether the current session is in single capture mode.
     */
    val inSingleCaptureMode = MutableLiveData(false)

    /**
     * Current camera state.
     */
    val cameraState = MutableLiveData<CameraState>()

    /**
     * Current screen rotation.
     */
    val screenRotation = MutableLiveData(Rotation.ROTATION_0)

    /**
     * Captured media [Uri]s
     */
    val capturedMedia = MediaRepository.capturedMedia(context).flowOn(
        Dispatchers.IO
    ).stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = listOf(),
    )

    // General

    /**
     * Flash mode.
     */
    val flashMode = MutableLiveData(FlashMode.AUTO)

    /**
     * Grid mode.
     */
    val gridMode = MutableLiveData<GridMode>()

    /**
     * Timer mode.
     */
    val timerMode = MutableLiveData<TimerMode>()

    // Photo

    /**
     * Photo capture mode.
     */
    val photoCaptureMode = MutableLiveData<Int>()

    /**
     * Photo aspect ratio.
     */
    val photoAspectRatio = MutableLiveData<Int>()

    /**
     * Photo effect.
     */
    val photoEffect = MutableLiveData<Int>()

    // Video

    /**
     * Video quality.
     */
    val videoQuality = MutableLiveData<Quality>()

    /**
     * Video frame rate.
     */
    val videoFrameRate = MutableLiveData<FrameRate?>()

    /**
     * Video dynamic range.
     */
    val videoDynamicRange = MutableLiveData<VideoDynamicRange>()

    /**
     * Video mic mode.
     */
    val videoMicMode = MutableLiveData<Boolean>()

    /**
     * Video [Recording].
     */
    val videoRecording = MutableLiveData<Recording?>()

    /**
     * Video recording duration.
     */
    val videoRecordingDuration = MutableLiveData<Long>()

    fun getAdditionalVideoFrameRates(cameraId: String, quality: Quality) =
        overlayConfiguration.additionalVideoConfigurations[cameraId]?.get(quality) ?: setOf()

    fun getLogicalZoomRatios(cameraId: String) = mutableMapOf(1.0f to 1.0f).apply {
        overlayConfiguration.logicalZoomRatios[cameraId]?.let {
            putAll(it)
        }
    }.toSortedMap()

    fun getCameras(
        cameraMode: CameraMode, cameraFacing: CameraFacing,
    ) = when (cameraMode) {
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

    /**
     * Get a suitable [Camera] for the provided [CameraFacing] and [CameraMode].
     * @param cameraFacing The requested [CameraFacing]
     * @param cameraMode The requested [CameraMode]
     * @return A [Camera] that is compatible with the provided configuration or null
     */
    fun getCameraOfFacingOrFirstAvailable(
        cameraFacing: CameraFacing, cameraMode: CameraMode
    ) = when (cameraFacing) {
        CameraFacing.BACK -> mainBackCamera
        CameraFacing.FRONT -> mainFrontCamera
        CameraFacing.EXTERNAL -> externalCameras.firstOrNull()
        else -> throw Exception("Unknown facing")
    }?.let {
        if (cameraMode == CameraMode.VIDEO && !it.supportsVideoRecording) {
            availableCamerasSupportingVideoRecording.firstOrNull()
        } else {
            it
        }
    } ?: when (cameraMode) {
        CameraMode.VIDEO -> availableCamerasSupportingVideoRecording.firstOrNull()
        else -> availableCameras.firstOrNull()
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
