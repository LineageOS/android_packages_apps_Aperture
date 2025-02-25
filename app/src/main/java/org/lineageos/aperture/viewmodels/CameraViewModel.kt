/*
 * SPDX-FileCopyrightText: 2023-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.viewmodels

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.view.OrientationEventListener
import androidx.annotation.RequiresApi
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.Recording
import androidx.camera.view.LifecycleCameraController
import androidx.core.location.LocationRequestCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.lineageos.aperture.camera.Camera
import org.lineageos.aperture.ext.ASPECT_RATIO_KEY
import org.lineageos.aperture.ext.BRIGHT_SCREEN_KEY
import org.lineageos.aperture.ext.LAST_GRID_MODE_KEY
import org.lineageos.aperture.ext.LEVELER_KEY
import org.lineageos.aperture.ext.TIMER_MODE_KEY
import org.lineageos.aperture.ext.applicationContext
import org.lineageos.aperture.ext.aspectRatio
import org.lineageos.aperture.ext.brightScreen
import org.lineageos.aperture.ext.broadcastReceiverFlow
import org.lineageos.aperture.ext.flashMode
import org.lineageos.aperture.ext.lastGridMode
import org.lineageos.aperture.ext.lastMicMode
import org.lineageos.aperture.ext.leveler
import org.lineageos.aperture.ext.locationFlow
import org.lineageos.aperture.ext.next
import org.lineageos.aperture.ext.preferenceFlow
import org.lineageos.aperture.ext.thermalStatusFlow
import org.lineageos.aperture.ext.timerMode
import org.lineageos.aperture.ext.videoDynamicRange
import org.lineageos.aperture.ext.videoFrameRate
import org.lineageos.aperture.ext.videoQuality
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
import org.lineageos.aperture.models.VideoQualityInfo
import org.lineageos.aperture.repository.MediaRepository
import org.lineageos.aperture.utils.OverlayConfiguration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * [ViewModel] representing a camera session. This data is used to receive
 * live data regarding the setting currently enabled.
 */
class CameraViewModel(application: Application) : AndroidViewModel(application) {
    // System services
    private val locationManager = applicationContext.getSystemService(LocationManager::class.java)
    private val powerManager = applicationContext.getSystemService(PowerManager::class.java)

    // Shared preferences
    private val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }

    // Orientation
    private val orientation = callbackFlow {
        val orientationEventListener = object : OrientationEventListener(applicationContext) {
            override fun onOrientationChanged(orientation: Int) {
                trySend(orientation)
            }
        }

        orientationEventListener.enable()

        awaitClose {
            orientationEventListener.disable()
        }
    }
        .flowOn(Dispatchers.IO)
        .shareIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            replay = 1
        )

    /**
     * CameraX's [ProcessCameraProvider].
     */
    private val cameraProvider = ProcessCameraProvider.getInstance(applicationContext).get()

    /**
     * CameraX's [ExtensionsManager].
     */
    val extensionsManager: ExtensionsManager =
        ExtensionsManager.getInstanceAsync(applicationContext, cameraProvider).get()

    /**
     * [ExecutorService] for camera related operations.
     */
    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /**
     * CameraX's [LifecycleCameraController].
     */
    val cameraController = LifecycleCameraController(applicationContext)

    /**
     * Overlay configuration.
     */
    val overlayConfiguration = OverlayConfiguration(applicationContext)

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
    val camera = MutableStateFlow<Camera?>(null)

    /**
     * CameraX's state of the current camera.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val cameraXCameraState = camera
        .flatMapLatest { camera ->
            camera?.cameraState?.asFlow() ?: flowOf(null)
        }
        .flowOn(Dispatchers.IO)
        .shareIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            replay = 1
        )

    /**
     * Current camera mode.
     */
    val cameraMode = MutableStateFlow(CameraMode.PHOTO)

    /**
     * Whether the current session is in single capture mode.
     */
    val inSingleCaptureMode = MutableStateFlow(false)

    /**
     * Current camera state.
     */
    val cameraState = MutableStateFlow(CameraState.IDLE)

    /**
     * Current screen rotation.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val screenRotation = orientation
        .filter { it != OrientationEventListener.ORIENTATION_UNKNOWN }
        .mapLatest { orientation ->
            Rotation.fromDegreesInAperture(orientation)
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = Rotation.ROTATION_0
        )

    /**
     * Captured media [Uri]s
     */
    val capturedMedia = MediaRepository.capturedMedia(applicationContext)
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = listOf(),
        )

    /**
     * Flash mode.
     */
    val flashMode = MutableStateFlow(FlashMode.AUTO)

    /**
     * Grid mode.
     */
    val gridMode = combine(
        sharedPreferences.preferenceFlow(
            LAST_GRID_MODE_KEY, getter = SharedPreferences::lastGridMode
        ),
        cameraMode,
    ) { gridMode, cameraMode ->
        gridMode.takeIf { cameraMode != CameraMode.QR } ?: GridMode.OFF
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = GridMode.OFF
        )

    /**
     * Timer mode.
     */
    val timerMode = sharedPreferences.preferenceFlow(
        TIMER_MODE_KEY, getter = SharedPreferences::timerMode
    )
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = TimerMode.OFF
        )

    /**
     * Whether the leveler is enabled.
     */
    val levelerEnabled = sharedPreferences.preferenceFlow(
        LEVELER_KEY, getter = SharedPreferences::leveler
    )
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false
        )

    /**
     * Whether screen brightness should be forced to full.
     */
    val fullScreenBrightness = sharedPreferences.preferenceFlow(
        BRIGHT_SCREEN_KEY, getter = SharedPreferences::brightScreen
    )
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false
        )

    @RequiresApi(Build.VERSION_CODES.Q)
    val thermalStatus = powerManager.thermalStatusFlow()
        .flowOn(Dispatchers.IO)
        .shareIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            replay = 1
        )

    /**
     * The current zoom state.
     */
    val zoomState = cameraController.zoomState.asFlow()
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    /**
     * The current torch state.
     */
    private val torchState = cameraController.torchState.asFlow()
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    val tapToFocusInfoState = cameraController.tapToFocusInfoState.asFlow()
        .flowOn(Dispatchers.IO)
        .shareIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            replay = 1
        )

    @Suppress("MissingPermission")
    val location = locationManager.locationFlow(
        LocationRequestCompat.Builder(1000)
            .setMinUpdateDistanceMeters(1f)
            .setQuality(LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY)
            .build()
    )
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    val batteryIntent = applicationContext.broadcastReceiverFlow(
        IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    )
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    // Photo

    /**
     * Photo capture mode.
     * @see ImageCapture.CaptureMode
     */
    val photoCaptureMode = MutableStateFlow(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)

    /**
     * Photo aspect ratio.
     * @see AspectRatio.Ratio
     */
    val photoAspectRatio = sharedPreferences.preferenceFlow(
        ASPECT_RATIO_KEY, getter = SharedPreferences::aspectRatio
    )
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = AspectRatio.RATIO_4_3
        )

    /**
     * Photo effect.
     * @see ExtensionMode.Mode
     */
    val photoEffect = MutableStateFlow(ExtensionMode.NONE)

    // Video

    /**
     * Video quality.
     */
    val videoQuality = MutableStateFlow(sharedPreferences.videoQuality)

    /**
     * Video frame rate.
     */
    val videoFrameRate = MutableStateFlow(sharedPreferences.videoFrameRate)

    /**
     * Video dynamic range.
     */
    val videoDynamicRange = MutableStateFlow(sharedPreferences.videoDynamicRange)

    /**
     * Video mic mode.
     */
    val videoMicMode = MutableStateFlow(sharedPreferences.lastMicMode)

    /**
     * Video [Recording].
     */
    val videoRecording = MutableStateFlow<Recording?>(null)

    /**
     * Video recording duration.
     */
    val videoRecordingDuration = MutableStateFlow(0L)

    /**
     * Whether the camera can be flipped.
     */
    val canFlipCamera = combine(cameraMode, cameraState) { cameraMode, cameraState ->
        cameraMode != CameraMode.QR && !cameraState.isRecordingVideo
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false
        )

    val supportedVideoQualities: Set<Quality>
        get() = camera.value?.supportedVideoQualities?.keys.orEmpty()

    private val videoQualityInfo: VideoQualityInfo?
        get() = camera.value?.supportedVideoQualities?.get(videoQuality.value)

    val supportedVideoFrameRates: Set<FrameRate>
        get() = videoQualityInfo?.supportedFrameRates.orEmpty()

    val supportedVideoDynamicRanges: Set<VideoDynamicRange>
        get() = videoQualityInfo?.supportedDynamicRanges.orEmpty()

    val supportedFlashModes: Set<FlashMode>
        get() = cameraMode.value.supportedFlashModes.intersect(
            camera.value?.supportedFlashModes.orEmpty()
        )

    init {
        viewModelScope.launch {
            // Track the torch state to update the flash mode
            torchState.collectLatest {
                flashMode.value = cameraController.flashMode
            }
        }
    }

    override fun onCleared() {
        cameraController.unbind()

        cameraExecutor.shutdown()
    }

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
     * Get a suitable [Camera] for the provided [CameraFacing] and the current [CameraMode].
     * @param cameraFacing The requested [CameraFacing]
     * @return A [Camera] that is compatible with the provided configuration or null
     */
    fun getCameraOfFacingOrFirstAvailable(cameraFacing: CameraFacing) = when (cameraFacing) {
        CameraFacing.BACK -> mainBackCamera
        CameraFacing.FRONT -> mainFrontCamera
        CameraFacing.EXTERNAL -> externalCameras.firstOrNull()
        else -> throw Exception("Unknown facing")
    }?.let {
        if (cameraMode.value == CameraMode.VIDEO && !it.supportsVideoRecording) {
            availableCamerasSupportingVideoRecording.firstOrNull()
        } else {
            it
        }
    } ?: when (cameraMode.value) {
        CameraMode.VIDEO -> availableCamerasSupportingVideoRecording.firstOrNull()
        else -> availableCameras.firstOrNull()
    }

    /**
     * Return the next camera, used for flip camera.
     * @return The next camera, may return null if all the cameras disappeared
     */
    fun getNextCamera(): Camera? {
        val currentCamera = camera.value
        val currentCameraMode = cameraMode.value

        val cameras = when (currentCameraMode) {
            CameraMode.VIDEO -> availableCamerasSupportingVideoRecording
            else -> availableCameras
        }

        // If value is -1 it will just pick the first available camera
        // This should only happen when an external camera is disconnected
        val newCameraIndex = cameras.indexOf(
            when (currentCamera?.cameraFacing) {
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

    /**
     * Cycle to the next grid mode.
     */
    fun cycleGridMode() {
        gridMode.value.next()?.let {
            sharedPreferences.lastGridMode = it
        }
    }

    /**
     * Toggle the timer mode.
     */
    fun toggleTimerMode() {
        timerMode.value.next()?.let {
            sharedPreferences.timerMode = it
        }
    }

    fun cyclePhotoAspectRatio() {
        sharedPreferences.aspectRatio = when (photoAspectRatio.value) {
            AspectRatio.RATIO_4_3 -> AspectRatio.RATIO_16_9
            AspectRatio.RATIO_16_9 -> AspectRatio.RATIO_4_3
            else -> AspectRatio.RATIO_4_3
        }
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
