/*
 * SPDX-FileCopyrightText: 2022-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.icu.text.DecimalFormat
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.PowerManager
import android.os.PowerManager.OnThermalStatusChangedListener
import android.provider.MediaStore
import android.util.Log
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.extensions.ExtensionMode
import androidx.camera.video.Quality
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.muted
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.onPinchToZoom
import androidx.camera.view.video.AudioConfig
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.Group
import androidx.core.animation.addListener
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import androidx.core.view.WindowCompat.getInsetsController
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import coil.decode.VideoFrameDecoder
import coil.load
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.sync.Semaphore
import org.lineageos.aperture.camera.CameraFacing
import org.lineageos.aperture.camera.CameraManager
import org.lineageos.aperture.camera.CameraMode
import org.lineageos.aperture.camera.CameraState
import org.lineageos.aperture.camera.CameraViewModel
import org.lineageos.aperture.camera.FlashMode
import org.lineageos.aperture.camera.FrameRate
import org.lineageos.aperture.camera.VideoStabilizationMode
import org.lineageos.aperture.ext.*
import org.lineageos.aperture.qr.QrImageAnalyzer
import org.lineageos.aperture.ui.CapturePreviewLayout
import org.lineageos.aperture.ui.CountDownView
import org.lineageos.aperture.ui.GridView
import org.lineageos.aperture.ui.HorizontalSlider
import org.lineageos.aperture.ui.LevelerView
import org.lineageos.aperture.ui.LocationPermissionsDialog
import org.lineageos.aperture.ui.PreviewBlurView
import org.lineageos.aperture.ui.SecondaryBarLayout
import org.lineageos.aperture.ui.VerticalSlider
import org.lineageos.aperture.utils.AssistantIntent
import org.lineageos.aperture.utils.BroadcastUtils
import org.lineageos.aperture.utils.CameraSoundsUtils
import org.lineageos.aperture.utils.ExifUtils
import org.lineageos.aperture.utils.GestureActions
import org.lineageos.aperture.utils.GoogleLensUtils
import org.lineageos.aperture.utils.GridMode
import org.lineageos.aperture.utils.MediaStoreUtils
import org.lineageos.aperture.utils.MediaType
import org.lineageos.aperture.utils.PermissionsUtils
import org.lineageos.aperture.utils.Rotation
import org.lineageos.aperture.utils.ShortcutsUtils
import org.lineageos.aperture.utils.StorageUtils
import org.lineageos.aperture.utils.TimeUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.concurrent.ExecutorService
import kotlin.math.abs

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
@androidx.camera.core.ExperimentalZeroShutterLag
@androidx.camera.view.video.ExperimentalVideo
open class CameraActivity : AppCompatActivity() {
    // Views
    private val cameraModeHighlight by lazy { findViewById<MaterialButton>(R.id.cameraModeHighlight) }
    private val capturePreviewLayout by lazy { findViewById<CapturePreviewLayout>(R.id.capturePreviewLayout) }
    private val countDownView by lazy { findViewById<CountDownView>(R.id.countDownView) }
    private val exposureLevel by lazy { findViewById<VerticalSlider>(R.id.exposureLevel) }
    private val flipCameraButton by lazy { findViewById<ImageButton>(R.id.flipCameraButton) }
    private val galleryButton by lazy { findViewById<ImageView>(R.id.galleryButton) }
    private val galleryButtonCardView by lazy { findViewById<CardView>(R.id.galleryButtonCardView) }
    private val googleLensButton by lazy { findViewById<ImageButton>(R.id.googleLensButton) }
    private val gridView by lazy { findViewById<GridView>(R.id.gridView) }
    private val levelerView by lazy { findViewById<LevelerView>(R.id.levelerView) }
    private val photoModeButton by lazy { findViewById<MaterialButton>(R.id.photoModeButton) }
    private val previewBlurView by lazy { findViewById<PreviewBlurView>(R.id.previewBlurView) }
    private val primaryBarLayoutGroupPhoto by lazy { findViewById<Group>(R.id.primaryBarLayoutGroupPhoto) }
    private val qrModeButton by lazy { findViewById<MaterialButton>(R.id.qrModeButton) }
    private val secondaryBarHalf by lazy { findViewById<LinearLayout>(R.id.secondaryBarHalf) }
    private val secondaryBarLayout by lazy { findViewById<SecondaryBarLayout>(R.id.secondaryBarLayout) }
    private val shutterButton by lazy { findViewById<ImageButton>(R.id.shutterButton) }
    private val videoDuration by lazy { findViewById<MaterialButton>(R.id.videoDuration) }
    private val videoModeButton by lazy { findViewById<MaterialButton>(R.id.videoModeButton) }
    private val videoRecordingStateButton by lazy { findViewById<ImageButton>(R.id.videoRecordingStateButton) }
    private val viewFinder by lazy { findViewById<PreviewView>(R.id.viewFinder) }
    private val viewFinderFocus by lazy { findViewById<ImageView>(R.id.viewFinderFocus) }
    private val zoomLevel by lazy { findViewById<HorizontalSlider>(R.id.zoomLevel) }

    // System services
    private val keyguardManager by lazy { getSystemService(KeyguardManager::class.java) }
    private val locationManager by lazy { getSystemService(LocationManager::class.java) }
    private val powerManager by lazy { getSystemService(PowerManager::class.java) }

    // Core camera utils
    private lateinit var cameraManager: CameraManager
    private val cameraController: LifecycleCameraController
        get() = cameraManager.cameraController
    private val cameraExecutor: ExecutorService
        get() = cameraManager.cameraExecutor
    private lateinit var cameraSoundsUtils: CameraSoundsUtils
    private val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }
    private val permissionsUtils by lazy { PermissionsUtils(this) }

    // Current camera state
    private val cameraViewModel: CameraViewModel by viewModels()

    private var camera
        get() = cameraViewModel.camera.value!!
        set(value) {
            if (Looper.getMainLooper().isCurrentThread) {
                cameraViewModel.camera.value = value
            } else {
                cameraViewModel.camera.postValue(value)
            }
        }
    private var cameraMode
        get() = cameraViewModel.cameraMode.value!!
        set(value) {
            if (Looper.getMainLooper().isCurrentThread) {
                cameraViewModel.cameraMode.value = value
            } else {
                cameraViewModel.cameraMode.postValue(value)
            }
        }
    private var singleCaptureMode
        get() = cameraViewModel.inSingleCaptureMode.value!!
        set(value) {
            if (Looper.getMainLooper().isCurrentThread) {
                cameraViewModel.inSingleCaptureMode.value = value
            } else {
                cameraViewModel.inSingleCaptureMode.postValue(value)
            }
        }
    private var cameraState
        get() = cameraViewModel.cameraState.value!!
        set(value) {
            if (Looper.getMainLooper().isCurrentThread) {
                cameraViewModel.cameraState.value = value
            } else {
                cameraViewModel.cameraState.postValue(value)
            }
        }
    private val screenRotation
        get() = cameraViewModel.screenRotation
    private var gridMode
        get() = cameraViewModel.gridMode.value!!
        set(value) {
            if (Looper.getMainLooper().isCurrentThread) {
                cameraViewModel.gridMode.value = value
            } else {
                cameraViewModel.gridMode.postValue(value)
            }
        }
    private var flashMode
        get() = cameraViewModel.flashMode.value!!
        set(value) {
            if (Looper.getMainLooper().isCurrentThread) {
                cameraViewModel.flashMode.value = value
            } else {
                cameraViewModel.flashMode.postValue(value)
            }
        }
    private var timerMode
        get() = cameraViewModel.timerMode.value!!
        set(value) {
            if (Looper.getMainLooper().isCurrentThread) {
                cameraViewModel.timerMode.value = value
            } else {
                cameraViewModel.timerMode.postValue(value)
            }
        }
    private var photoCaptureMode
        get() = cameraViewModel.photoCaptureMode.value!!
        set(value) {
            if (Looper.getMainLooper().isCurrentThread) {
                cameraViewModel.photoCaptureMode.value = value
            } else {
                cameraViewModel.photoCaptureMode.postValue(value)
            }
        }
    private var photoAspectRatio
        get() = cameraViewModel.photoAspectRatio.value!!
        set(value) {
            if (Looper.getMainLooper().isCurrentThread) {
                cameraViewModel.photoAspectRatio.value = value
            } else {
                cameraViewModel.photoAspectRatio.postValue(value)
            }
        }
    private var photoEffect
        get() = cameraViewModel.photoEffect.value!!
        set(value) {
            if (Looper.getMainLooper().isCurrentThread) {
                cameraViewModel.photoEffect.value = value
            } else {
                cameraViewModel.photoEffect.postValue(value)
            }
        }
    private var videoQuality
        get() = cameraViewModel.videoQuality.value!!
        set(value) {
            if (Looper.getMainLooper().isCurrentThread) {
                cameraViewModel.videoQuality.value = value
            } else {
                cameraViewModel.videoQuality.postValue(value)
            }
        }
    private var videoFrameRate
        get() = cameraViewModel.videoFrameRate.value
        set(value) {
            if (Looper.getMainLooper().isCurrentThread) {
                cameraViewModel.videoFrameRate.value = value
            } else {
                cameraViewModel.videoFrameRate.postValue(value)
            }
        }
    private var videoMicMode
        get() = cameraViewModel.videoMicMode.value!!
        set(value) {
            if (Looper.getMainLooper().isCurrentThread) {
                cameraViewModel.videoMicMode.value = value
            } else {
                cameraViewModel.videoMicMode.postValue(value)
            }
        }
    private var videoAudioConfig
        get() = cameraViewModel.videoAudioConfig.value!!
        set(value) {
            if (Looper.getMainLooper().isCurrentThread) {
                cameraViewModel.videoAudioConfig.value = value
            } else {
                cameraViewModel.videoAudioConfig.postValue(value)
            }
        }
    private var videoRecording
        get() = cameraViewModel.videoRecording.value
        set(value) {
            if (Looper.getMainLooper().isCurrentThread) {
                cameraViewModel.videoRecording.value = value
            } else {
                cameraViewModel.videoRecording.postValue(value)
            }
        }

    private lateinit var initialCameraFacing: CameraFacing

    private var tookSomething: Boolean = false
        set(value) {
            field = value
            updateGalleryButton()
        }

    private var zoomGestureSemaphore = Semaphore(1)

    // Video
    private val supportedVideoQualities: Set<Quality>
        get() = camera.supportedVideoQualities.keys
    private val supportedVideoFrameRates: Set<FrameRate>
        get() = camera.supportedVideoQualities.getOrDefault(
            videoQuality, setOf()
        )

    // QR
    private val imageAnalyzer by lazy { QrImageAnalyzer(this) }
    private val isGoogleLensAvailable by lazy { GoogleLensUtils.isGoogleLensAvailable(this) }

    private var viewFinderTouchEvent: MotionEvent? = null
    private val gestureDetector by lazy {
        GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                viewFinderTouchEvent = e
                return false
            }

            override fun onFling(
                e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float
            ): Boolean {
                if (!handler.hasMessages(MSG_ON_PINCH_TO_ZOOM) &&
                    abs(e1.x - e2.x) > 75 * resources.displayMetrics.density
                ) {
                    if (e2.x > e1.x) {
                        // Left to right
                        when (cameraMode) {
                            CameraMode.PHOTO -> changeCameraMode(CameraMode.QR)
                            CameraMode.VIDEO -> changeCameraMode(CameraMode.PHOTO)
                            CameraMode.QR -> changeCameraMode(CameraMode.VIDEO)
                        }
                    } else {
                        // Right to left
                        when (cameraMode) {
                            CameraMode.PHOTO -> changeCameraMode(CameraMode.VIDEO)
                            CameraMode.VIDEO -> changeCameraMode(CameraMode.QR)
                            CameraMode.QR -> changeCameraMode(CameraMode.PHOTO)
                        }
                    }
                }
                return true
            }
        })
    }
    private val scaleGestureDetector by lazy {
        ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                cameraController.onPinchToZoom(detector.scaleFactor)

                handler.removeMessages(MSG_ON_PINCH_TO_ZOOM)
                handler.sendMessageDelayed(handler.obtainMessage(MSG_ON_PINCH_TO_ZOOM), 500)

                return true
            }
        })
    }

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                MSG_HIDE_ZOOM_SLIDER -> {
                    zoomLevel.visibility = View.GONE
                }
                MSG_HIDE_FOCUS_RING -> {
                    viewFinderFocus.visibility = View.GONE
                }
                MSG_HIDE_EXPOSURE_SLIDER -> {
                    exposureLevel.visibility = View.GONE
                }
            }
        }
    }

    private var location: Location? = null
    private val locationListener = object : LocationListenerCompat {
        override fun onLocationChanged(location: Location) {
            val cameraActivity = this@CameraActivity
            cameraActivity.location = cameraActivity.location?.let {
                if (it.accuracy >= location.accuracy) {
                    location
                } else {
                    cameraActivity.location
                }
            } ?: location
        }

        @SuppressLint("MissingPermission")
        fun register() {
            // Reset cached location
            location = null

            if (permissionsUtils.locationPermissionsGranted()
                && sharedPreferences.saveLocation == true
            ) {
                // Request location updates
                locationManager.allProviders.forEach {
                    LocationManagerCompat.requestLocationUpdates(
                        locationManager,
                        it,
                        LocationRequestCompat.Builder(1000).apply {
                            setMinUpdateDistanceMeters(1f)
                            setQuality(LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY)
                        }.build(),
                        this,
                        Looper.getMainLooper()
                    )
                }
            }
        }

        fun unregister() {
            // Remove updates
            locationManager.removeUpdates(this)

            // Reset cached location
            location = null
        }
    }

    private val mainPermissionsRequestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (it.isNotEmpty()) {
            if (!permissionsUtils.mainPermissionsGranted()) {
                Toast.makeText(
                    this, getString(R.string.app_permissions_toast), Toast.LENGTH_SHORT
                ).show()
                finish()
            }

            // This is a good time to ask the user for location permissions
            if (sharedPreferences.saveLocation == null) {
                locationPermissionsDialog.show()
            }
        }
    }
    private val locationPermissionsRequestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        sharedPreferences.saveLocation = permissionsUtils.locationPermissionsGranted()
    }

    private val locationPermissionsDialog by lazy {
        LocationPermissionsDialog(this).also {
            it.onResultCallback = { result ->
                if (result) {
                    locationPermissionsRequestLauncher.launch(PermissionsUtils.locationPermissions)
                } else {
                    sharedPreferences.saveLocation = false
                }
            }
        }
    }

    private val orientationEventListener by lazy {
        object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    return
                }

                val rotation = Rotation.fromDegreesInAperture(orientation)

                if (screenRotation.value != rotation) {
                    screenRotation.value = rotation
                }
            }
        }
    }

    @get:RequiresApi(Build.VERSION_CODES.Q)
    private val onThermalStatusChangedListener by lazy {
        OnThermalStatusChangedListener {
            val showSnackBar = { s: Int ->
                Snackbar.make(secondaryBarHalf, s, Snackbar.LENGTH_INDEFINITE)
                    .setAnchorView(secondaryBarHalf)
                    .setAction(android.R.string.ok) {
                        // Do nothing
                    }
                    .show()
            }

            when (it) {
                PowerManager.THERMAL_STATUS_MODERATE -> {
                    showSnackBar(R.string.thermal_status_moderate)
                }
                PowerManager.THERMAL_STATUS_SEVERE -> {
                    showSnackBar(R.string.thermal_status_severe)
                }
                PowerManager.THERMAL_STATUS_CRITICAL -> {
                    showSnackBar(R.string.thermal_status_critical)
                }
                PowerManager.THERMAL_STATUS_EMERGENCY -> {
                    showSnackBar(R.string.thermal_status_emergency)
                    emergencyClose()
                }
                PowerManager.THERMAL_STATUS_SHUTDOWN -> {
                    showSnackBar(R.string.thermal_status_shutdown)
                    emergencyClose()
                }
            }
        }
    }

    enum class ShutterAnimation(val resourceId: Int) {
        InitPhoto(R.drawable.avd_photo_capture),
        InitVideo(R.drawable.avd_mode_video_photo),

        PhotoCapture(R.drawable.avd_photo_capture),
        PhotoToVideo(R.drawable.avd_mode_photo_video),

        VideoToPhoto(R.drawable.avd_mode_video_photo),
        VideoStart(R.drawable.avd_video_start),
        VideoEnd(R.drawable.avd_video_end),
    }

    enum class VideoRecordingStateAnimation(val resourceId: Int) {
        Init(R.drawable.avd_video_recording_pause),
        ResumeToPause(R.drawable.avd_video_recording_pause),
        PauseToResume(R.drawable.avd_video_recording_resume),
    }

    private val intentActions = mapOf(
        // Intents
        MediaStore.ACTION_IMAGE_CAPTURE to {
            cameraMode = CameraMode.PHOTO
            singleCaptureMode = true
        },
        MediaStore.ACTION_IMAGE_CAPTURE_SECURE to {
            cameraMode = CameraMode.PHOTO
            singleCaptureMode = true
        },
        MediaStore.ACTION_VIDEO_CAPTURE to {
            cameraMode = CameraMode.VIDEO
            singleCaptureMode = true
        },
        MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA to {
            cameraMode = CameraMode.PHOTO
        },
        MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE to {
            cameraMode = CameraMode.PHOTO
        },
        MediaStore.INTENT_ACTION_VIDEO_CAMERA to {
            cameraMode = CameraMode.VIDEO
        },

        // Shortcuts
        ShortcutsUtils.SHORTCUT_ID_SELFIE to {
            cameraMode = CameraMode.PHOTO
            initialCameraFacing = CameraFacing.FRONT
        },
        ShortcutsUtils.SHORTCUT_ID_VIDEO to {
            cameraMode = CameraMode.VIDEO
            initialCameraFacing = CameraFacing.BACK
        },
        ShortcutsUtils.SHORTCUT_ID_QR to {
            cameraMode = CameraMode.QR
        },
    )
    private val assistantIntent
        get() = AssistantIntent.fromIntent(intent)
    private val launchedViaVoiceIntent
        get() = isVoiceInteractionRoot && intent.hasCategory(Intent.CATEGORY_VOICE)

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideStatusBars()

        setContentView(R.layout.activity_camera)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
            && keyguardManager.isKeyguardLocked
        ) {
            setShowWhenLocked(true)

            @SuppressLint("SourceLockedOrientationActivity")
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        // Register shortcuts
        ShortcutsUtils.registerShortcuts(this)

        // Initialize camera manager
        cameraManager = CameraManager(this)

        // Initialize sounds utils
        cameraSoundsUtils = CameraSoundsUtils(sharedPreferences)

        // Initialize camera mode and facing
        cameraMode = overrideInitialCameraMode() ?: sharedPreferences.lastCameraMode
        initialCameraFacing = sharedPreferences.lastCameraFacing

        // Pass the view model to the views
        capturePreviewLayout.cameraViewModel = cameraViewModel
        countDownView.cameraViewModel = cameraViewModel
        secondaryBarLayout.cameraViewModel = cameraViewModel

        // Restore settings from shared preferences
        gridMode = sharedPreferences.lastGridMode
        timerMode = sharedPreferences.timerMode
        photoAspectRatio = sharedPreferences.aspectRatio
        photoEffect = sharedPreferences.photoEffect
        videoQuality = sharedPreferences.videoQuality
        videoFrameRate = sharedPreferences.videoFrameRate
        videoMicMode = sharedPreferences.lastMicMode

        // Handle intent
        intent.action?.let {
            intentActions[it]?.invoke()
        }

        // Handle assistant intent
        assistantIntent?.useFrontCamera?.let {
            initialCameraFacing = if (it) {
                CameraFacing.FRONT
            } else {
                CameraFacing.BACK
            }
        }

        if (cameraManager.internalCamerasSupportingVideoRecoding.isEmpty()) {
            // Hide video mode button if no internal camera supports video recoding
            videoModeButton.isVisible = false
            if (cameraMode == CameraMode.VIDEO) {
                // If an app asked for a video we have to bail out
                if (singleCaptureMode) {
                    Toast.makeText(
                        this, getString(R.string.camcorder_unsupported_toast), Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
                // Fallback to photo mode
                cameraMode = CameraMode.PHOTO
            }
        }

        // Select a camera
        camera = cameraManager.getCameraOfFacingOrFirstAvailable(initialCameraFacing, cameraMode)

        // Set secondary top bar button callbacks
        secondaryBarLayout.aspectRatioButton.setOnClickListener { cycleAspectRatio() }
        secondaryBarLayout.videoQualityButton.setOnClickListener { cycleVideoQuality() }
        secondaryBarLayout.videoFrameRateButton.setOnClickListener { cycleVideoFrameRate() }
        secondaryBarLayout.effectButton.setOnClickListener { cyclePhotoEffects() }
        secondaryBarLayout.gridButton.setOnClickListener { cycleGridMode() }
        secondaryBarLayout.timerButton.setOnClickListener { toggleTimerMode() }
        secondaryBarLayout.micButton.setOnClickListener { toggleMicrophoneMode() }
        secondaryBarLayout.settingsButton.setOnClickListener { openSettings() }

        // Set secondary bottom bar button callbacks
        secondaryBarLayout.flashButton.setOnClickListener { cycleFlashMode() }

        // Initialize camera mode highlight position
        (cameraModeHighlight.parent as View).doOnLayout {
            cameraModeHighlight.x = when (cameraMode) {
                CameraMode.QR -> qrModeButton.x
                CameraMode.PHOTO -> photoModeButton.x
                CameraMode.VIDEO -> videoModeButton.x
            }
        }

        // Attach CameraController to PreviewView
        viewFinder.controller = cameraController

        // Observe torch state
        cameraController.torchState.observe(this) {
            flashMode = cameraController.flashMode
        }

        // Observe focus state
        cameraController.tapToFocusState.observe(this) {
            when (it) {
                CameraController.TAP_TO_FOCUS_STARTED -> {
                    viewFinderFocus.visibility = View.VISIBLE
                    handler.removeMessages(MSG_HIDE_FOCUS_RING)
                    ValueAnimator.ofInt(0.px, 8.px).apply {
                        addUpdateListener { anim ->
                            viewFinderFocus.setPadding(anim.animatedValue as Int)
                        }
                    }.start()
                }
                else -> {
                    handler.removeMessages(MSG_HIDE_FOCUS_RING)
                    ValueAnimator.ofInt(8.px, 0.px).apply {
                        addUpdateListener { anim ->
                            viewFinderFocus.setPadding(anim.animatedValue as Int)
                        }
                    }.start()

                    handler.sendMessageDelayed(handler.obtainMessage(MSG_HIDE_FOCUS_RING), 500)
                }
            }
        }

        // Observe manual focus
        viewFinder.setOnTouchListener { _, event ->
            if (scaleGestureDetector.onTouchEvent(event) && scaleGestureDetector.isInProgress) {
                return@setOnTouchListener true
            }
            return@setOnTouchListener gestureDetector.onTouchEvent(event)
        }
        viewFinder.setOnClickListener { view ->
            // Reset exposure level to 0 EV
            cameraController.cameraControl?.setExposureCompensationIndex(0)
            exposureLevel.progress = 0.5f

            exposureLevel.isVisible = true
            viewFinderTouchEvent?.let {
                viewFinderFocus.x = it.x - (viewFinderFocus.width / 2)
                viewFinderFocus.y = it.y - (viewFinderFocus.height / 2)
            } ?: run {
                viewFinderFocus.x = (view.width - viewFinderFocus.width) / 2f
                viewFinderFocus.y = (view.height - viewFinderFocus.height) / 2f
            }
            handler.removeMessages(MSG_HIDE_EXPOSURE_SLIDER)
            handler.sendMessageDelayed(handler.obtainMessage(MSG_HIDE_EXPOSURE_SLIDER), 2000)

            secondaryBarLayout.slideDown()
        }

        // Observe preview stream state
        viewFinder.previewStreamState.observe(this) {
            when (it) {
                PreviewView.StreamState.STREAMING -> {
                    // Show grid
                    gridView.alpha = 1f
                    gridView.previewView = viewFinder

                    // Hide preview blur
                    previewBlurView.isVisible = false

                    // Issue capture if requested via assistant
                    if ((launchedViaVoiceIntent || assistantIntent?.cameraOpenOnly != null)
                        && assistantIntent?.cameraOpenOnly != true
                    ) {
                        shutterButton.performClick()
                    }
                }
                else -> {}
            }
        }

        // Observe zoom state
        cameraController.zoomState.observe(this) {
            if (it.minZoomRatio == it.maxZoomRatio) {
                return@observe
            }

            zoomLevel.progress = it.linearZoom
            zoomLevel.visibility = View.VISIBLE

            handler.removeMessages(MSG_HIDE_ZOOM_SLIDER)
            handler.sendMessageDelayed(handler.obtainMessage(MSG_HIDE_ZOOM_SLIDER), 2000)

            secondaryBarLayout.lensSelectorLayout.onZoomRatioChanged(it.zoomRatio)
        }

        zoomLevel.onProgressChangedByUser = {
            cameraController.setLinearZoom(it)
        }
        zoomLevel.textFormatter = {
            "%.1fx".format(cameraController.zoomState.value?.zoomRatio)
        }

        // Set expose level callback & text formatter
        exposureLevel.onProgressChangedByUser = {
            cameraController.cameraControl?.setExposureCompensationIndex(
                Int.mapToRange(camera.exposureCompensationRange, it)
            )

            handler.removeMessages(MSG_HIDE_EXPOSURE_SLIDER)
            handler.sendMessageDelayed(handler.obtainMessage(MSG_HIDE_EXPOSURE_SLIDER), 2000)
        }
        exposureLevel.textFormatter = {
            val ev = Int.mapToRange(camera.exposureCompensationRange, it)
            if (ev == 0) "0" else EXPOSURE_LEVEL_FORMATTER.format(ev).toString()
        }

        // Set primary bar button callbacks
        qrModeButton.setOnClickListener { changeCameraMode(CameraMode.QR) }
        photoModeButton.setOnClickListener { changeCameraMode(CameraMode.PHOTO) }
        videoModeButton.setOnClickListener { changeCameraMode(CameraMode.VIDEO) }

        flipCameraButton.setOnClickListener { flipCamera() }
        googleLensButton.setOnClickListener {
            dismissKeyguardAndRun {
                GoogleLensUtils.launchGoogleLens(this)
            }
        }

        videoRecordingStateButton.setOnClickListener {
            when (cameraState) {
                CameraState.RECORDING_VIDEO -> videoRecording?.pause()
                CameraState.RECORDING_VIDEO_PAUSED -> videoRecording?.resume()
                else -> throw Exception("videoRecordingStateButton clicked while in invalid state: $cameraState")
            }
        }

        // Initialize shutter drawable
        when (cameraMode) {
            CameraMode.PHOTO -> startShutterAnimation(ShutterAnimation.InitPhoto)
            CameraMode.VIDEO -> startShutterAnimation(ShutterAnimation.InitVideo)
            else -> {}
        }

        shutterButton.setOnClickListener {
            // Shutter animation
            when (cameraMode) {
                CameraMode.PHOTO -> startShutterAnimation(ShutterAnimation.PhotoCapture)
                CameraMode.VIDEO -> {
                    if (countDownView.cancelCountDown()) {
                        startShutterAnimation(ShutterAnimation.VideoEnd)
                        return@setOnClickListener
                    }
                    if (cameraState == CameraState.IDLE) {
                        startShutterAnimation(ShutterAnimation.VideoStart)
                    }
                }
                else -> {}
            }

            startTimerAndRun {
                when (cameraMode) {
                    CameraMode.PHOTO -> takePhoto()
                    CameraMode.VIDEO -> captureVideo()
                    else -> {}
                }
            }
        }

        galleryButton.setOnClickListener { openGallery() }

        // Set lens switching callback
        secondaryBarLayout.lensSelectorLayout.onCameraChangeCallback = {
            if (canRestartCamera()) {
                camera = it
                bindCameraUseCases()
            }
        }
        secondaryBarLayout.lensSelectorLayout.onZoomRatioChangeCallback = {
            cameraController.setZoomRatio(it)
        }

        // Set capture preview callback
        capturePreviewLayout.onChoiceCallback = { input ->
            when (input) {
                null -> {
                    capturePreviewLayout.isVisible = false
                }
                is InputStream,
                is Uri -> sendIntentResultAndExit(input)
                else -> throw Exception("Invalid input")
            }
        }

        // Bind viewfinder and preview blur view
        previewBlurView.previewView = viewFinder

        // Observe screen rotation
        screenRotation.observe(this) { rotateViews(it) }

        // Observe camera mode
        cameraViewModel.cameraMode.observe(this) {
            val cameraMode = it ?: return@observe

            // Update camera mode buttons
            qrModeButton.isEnabled = cameraMode != CameraMode.QR
            photoModeButton.isEnabled = cameraMode != CameraMode.PHOTO
            videoModeButton.isEnabled = cameraMode != CameraMode.VIDEO

            // Animate camera mode change
            (cameraModeHighlight.parent as View).doOnLayout {
                ValueAnimator.ofFloat(
                    cameraModeHighlight.x, when (cameraMode) {
                        CameraMode.QR -> qrModeButton.x
                        CameraMode.PHOTO -> photoModeButton.x
                        CameraMode.VIDEO -> videoModeButton.x
                    }
                ).apply {
                    addUpdateListener { valueAnimator ->
                        cameraModeHighlight.x = valueAnimator.animatedValue as Float
                    }
                }.start()
            }
        }

        // Observe single capture mode
        cameraViewModel.inSingleCaptureMode.observe(this) {
            val inSingleCaptureMode = it ?: return@observe

            // Update primary bar buttons
            galleryButtonCardView.isInvisible = inSingleCaptureMode

            // Update camera mode buttons
            cameraModeHighlight.isInvisible = inSingleCaptureMode
            photoModeButton.isInvisible = inSingleCaptureMode
            videoModeButton.isInvisible = inSingleCaptureMode
            qrModeButton.isInvisible = inSingleCaptureMode
        }

        // Observe camera state
        cameraViewModel.cameraState.observe(this) {
            val cameraState = it ?: return@observe

            // Update primary bar buttons
            galleryButton.isEnabled = cameraState == CameraState.IDLE
            // Shutter button must stay enabled
            flipCameraButton.isEnabled = cameraState == CameraState.IDLE
        }

        // Request camera permissions
        if (!permissionsUtils.mainPermissionsGranted()) {
            mainPermissionsRequestLauncher.launch(PermissionsUtils.mainPermissions)
        } else if (sharedPreferences.saveLocation == null) {
            locationPermissionsDialog.show()
        }
    }

    override fun onResume() {
        super.onResume()

        // Re-request camera permissions in case the user revoked them on app runtime
        if (!permissionsUtils.mainPermissionsGranted()) {
            mainPermissionsRequestLauncher.launch(PermissionsUtils.mainPermissions)
        }

        // Set bright screen
        setBrightScreen(sharedPreferences.brightScreen)

        // Set leveler
        setLeveler(sharedPreferences.leveler)

        // Reset tookSomething state
        tookSomething = false

        // Register location updates
        locationListener.register()

        // Enable orientation listener
        orientationEventListener.enable()

        // Start observing thermal status
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            powerManager.addThermalStatusListener(onThermalStatusChangedListener)
        }

        // Re-bind the use cases
        bindCameraUseCases()
    }

    override fun onPause() {
        // Remove location and location updates
        locationListener.unregister()

        // Disable orientation listener
        orientationEventListener.disable()

        // Remove thermal status observer
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            powerManager.removeThermalStatusListener(onThermalStatusChangedListener)
        }

        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.shutdown()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (capturePreviewLayout.isVisible) {
            super.onKeyDown(keyCode, event)
        } else when (keyCode) {
            KeyEvent.KEYCODE_FOCUS -> {
                if (event?.repeatCount == 1) {
                    viewFinderTouchEvent = null
                    viewFinder.performClick()
                }
                true
            }
            KeyEvent.KEYCODE_CAMERA -> {
                if (cameraMode == CameraMode.VIDEO && shutterButton.isEnabled && event?.repeatCount == 1) {
                    shutterButton.performClick()
                }
                true
            }
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN -> when (sharedPreferences.volumeButtonsAction) {
                GestureActions.SHUTTER -> {
                    if (cameraMode == CameraMode.VIDEO && shutterButton.isEnabled && event?.repeatCount == 1) {
                        shutterButton.performClick()
                    }
                    true
                }
                GestureActions.ZOOM -> {
                    when (keyCode) {
                        KeyEvent.KEYCODE_VOLUME_UP -> zoomIn()
                        KeyEvent.KEYCODE_VOLUME_DOWN -> zoomOut()
                    }
                    true
                }
                GestureActions.VOLUME -> {
                    super.onKeyDown(keyCode, event)
                }
                GestureActions.NOTHING -> {
                    // Do nothing
                    true
                }
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return if (capturePreviewLayout.isVisible) {
            super.onKeyUp(keyCode, event)
        } else when (keyCode) {
            KeyEvent.KEYCODE_CAMERA -> {
                if (cameraMode != CameraMode.QR && shutterButton.isEnabled) {
                    shutterButton.performClick()
                }
                true
            }
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                when (sharedPreferences.volumeButtonsAction) {
                    GestureActions.SHUTTER -> {
                        if (cameraMode != CameraMode.QR && shutterButton.isEnabled) {
                            shutterButton.performClick()
                        }
                        true
                    }
                    GestureActions.ZOOM -> {
                        true
                    }
                    GestureActions.VOLUME -> {
                        super.onKeyDown(keyCode, event)
                    }
                    GestureActions.NOTHING -> {
                        // Do nothing
                        true
                    }
                }
            }
            else -> super.onKeyUp(keyCode, event)
        }
    }

    /**
     * This is a method that can be overridden to set the initial camera mode and facing.
     * It's gonna have priority over shared preferences and intents.
     */
    protected open fun overrideInitialCameraMode(): CameraMode? = null

    private fun startShutterAnimation(shutterAnimation: ShutterAnimation) {
        // Get appropriate drawable
        val drawable = ContextCompat.getDrawable(
            this, shutterAnimation.resourceId
        ) as AnimatedVectorDrawable

        // Update current drawable
        shutterButton.setImageDrawable(drawable)

        // Start or reset animation
        when (shutterAnimation) {
            ShutterAnimation.InitPhoto,
            ShutterAnimation.InitVideo -> drawable.reset()
            else -> drawable.start()
        }
    }

    private fun startVideoRecordingStateAnimation(animation: VideoRecordingStateAnimation) {
        // Get appropriate drawable
        val drawable = ContextCompat.getDrawable(
            this, animation.resourceId
        ) as AnimatedVectorDrawable

        // Update current drawable
        videoRecordingStateButton.setImageDrawable(drawable)

        // Start or reset animation
        when (animation) {
            VideoRecordingStateAnimation.Init -> drawable.reset()
            else -> drawable.start()
        }
    }

    private fun takePhoto() {
        // Bail out if a photo is already being taken
        if (cameraState == CameraState.TAKING_PHOTO) {
            return
        }

        cameraState = CameraState.TAKING_PHOTO
        shutterButton.isEnabled = false

        val photoOutputStream = if (singleCaptureMode) {
            ByteArrayOutputStream(SINGLE_CAPTURE_PHOTO_BUFFER_INITIAL_SIZE_BYTES)
        } else {
            null
        }

        // Create output options object which contains file + metadata
        val outputOptions = StorageUtils.getPhotoMediaStoreOutputOptions(
            contentResolver,
            ImageCapture.Metadata().apply {
                if (!singleCaptureMode) {
                    location = this@CameraActivity.location
                }
                if (camera.cameraFacing == CameraFacing.FRONT) {
                    isReversedHorizontal = sharedPreferences.photoFfcMirror
                }
            },
            photoOutputStream
        )

        // Set up image capture listener, which is triggered after photo has
        // been taken
        cameraController.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(LOG_TAG, "Photo capture failed: ${exc.message}", exc)
                    cameraState = CameraState.IDLE
                    shutterButton.isEnabled = true
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    cameraSoundsUtils.playShutterClick()
                    viewFinder.foreground = ColorDrawable(Color.BLACK)
                    ValueAnimator.ofInt(0, 255, 0).apply {
                        addUpdateListener { anim ->
                            viewFinder.foreground.alpha = anim.animatedValue as Int
                        }
                    }.start()
                    Log.d(LOG_TAG, "Photo capture succeeded: ${output.savedUri}")
                    cameraState = CameraState.IDLE
                    shutterButton.isEnabled = true
                    if (!singleCaptureMode) {
                        sharedPreferences.lastSavedUri = output.savedUri
                        tookSomething = true
                        output.savedUri?.let {
                            BroadcastUtils.broadcastNewPicture(this@CameraActivity, it)
                        }
                    } else {
                        output.savedUri?.let {
                            openCapturePreview(it, MediaType.PHOTO)
                        }
                        photoOutputStream?.use {
                            openCapturePreview(
                                ByteArrayInputStream(photoOutputStream.toByteArray())
                            )
                        }
                    }
                }
            }
        )
    }

    private fun captureVideo() {
        if (cameraState != CameraState.IDLE) {
            if (cameraController.isRecording) {
                // Stop the current recording session.
                videoRecording?.stop()
            }
            return
        }

        // Disallow state changes while we are about to prepare for recording video
        cameraState = CameraState.PRE_RECORDING_VIDEO

        // Create output options object which contains file + metadata
        val outputOptions = StorageUtils.getVideoMediaStoreOutputOptions(
            contentResolver,
            location.takeUnless { singleCaptureMode }
        )

        // Play shutter sound
        val delayTime = if (cameraSoundsUtils.playStartVideoRecording()) 500L else 0L

        handler.postDelayed({
            // Start recording
            videoRecording = cameraController.startRecording(
                outputOptions,
                videoAudioConfig,
                cameraExecutor
            ) {
                val updateRecordingStatus = { enabled: Boolean, duration: Long ->
                    // Hide mode buttons
                    photoModeButton.isInvisible = enabled || singleCaptureMode
                    videoModeButton.isInvisible = enabled || singleCaptureMode
                    qrModeButton.isInvisible = enabled || singleCaptureMode

                    // Update duration text and visibility state
                    videoDuration.text = TimeUtils.convertNanosToString(duration)
                    videoDuration.isVisible = enabled

                    // Update video recording pause/resume button visibility state
                    if (duration == 0L) {
                        flipCameraButton.isInvisible = enabled
                        videoRecordingStateButton.isVisible = enabled
                    }
                }

                when (it) {
                    is VideoRecordEvent.Start -> runOnUiThread {
                        cameraState = CameraState.RECORDING_VIDEO
                        startVideoRecordingStateAnimation(VideoRecordingStateAnimation.Init)
                    }
                    is VideoRecordEvent.Pause -> runOnUiThread {
                        cameraState = CameraState.RECORDING_VIDEO_PAUSED
                        startVideoRecordingStateAnimation(VideoRecordingStateAnimation.ResumeToPause)
                    }
                    is VideoRecordEvent.Resume -> runOnUiThread {
                        cameraState = CameraState.RECORDING_VIDEO
                        startVideoRecordingStateAnimation(VideoRecordingStateAnimation.PauseToResume)
                    }
                    is VideoRecordEvent.Status -> runOnUiThread {
                        updateRecordingStatus(true, it.recordingStats.recordedDurationNanos)
                    }
                    is VideoRecordEvent.Finalize -> {
                        runOnUiThread {
                            startShutterAnimation(ShutterAnimation.VideoEnd)
                            updateRecordingStatus(false, 0)
                        }
                        cameraSoundsUtils.playStopVideoRecording()
                        if (it.error != VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA) {
                            Log.d(LOG_TAG, "Video capture succeeded: ${it.outputResults.outputUri}")
                            if (!singleCaptureMode) {
                                sharedPreferences.lastSavedUri = it.outputResults.outputUri
                                tookSomething = true
                                BroadcastUtils.broadcastNewVideo(this, it.outputResults.outputUri)
                            } else {
                                openCapturePreview(it.outputResults.outputUri, MediaType.VIDEO)
                            }
                        }
                        cameraState = CameraState.IDLE
                        videoRecording = null
                    }
                }
            }
        }, delayTime)
    }

    /**
     * Check if we can reinitialize the camera use cases
     */
    private fun canRestartCamera() = cameraState == CameraState.IDLE && !countDownView.isVisible

    /**
     * Rebind cameraProvider use cases
     */
    private fun bindCameraUseCases() {
        // Show blurred preview
        previewBlurView.freeze()
        previewBlurView.isVisible = true

        // Unbind previous use cases
        cameraController.unbind()

        cameraState = CameraState.IDLE

        // Hide grid until preview is ready
        gridView.alpha = 0f

        // Get the desired camera
        camera = when (cameraMode) {
            CameraMode.QR -> cameraManager.getCameraOfFacingOrFirstAvailable(
                CameraFacing.BACK, cameraMode
            )
            else -> camera
        }

        // If the current camera doesn't support the selected camera mode
        // pick a different one, giving priority to camera facing
        if (!camera.supportsCameraMode(cameraMode)) {
            camera = cameraManager.getCameraOfFacingOrFirstAvailable(
                camera.cameraFacing, cameraMode
            )
        }

        // Fallback to ExtensionMode.NONE if necessary
        if (!camera.supportsExtensionMode(photoEffect)) {
            photoEffect = ExtensionMode.NONE
        }

        // Initialize the use case we want and set its properties
        val cameraUseCases = when (cameraMode) {
            CameraMode.QR -> {
                cameraController.setImageAnalysisAnalyzer(cameraExecutor, imageAnalyzer)
                CameraController.IMAGE_ANALYSIS
            }
            CameraMode.PHOTO -> {
                cameraController.imageCaptureResolutionSelector = ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy(
                        photoAspectRatio, AspectRatioStrategy.FALLBACK_RULE_AUTO
                    ))
                    .setAllowedResolutionMode(if (cameraManager.enableHighResolution) {
                        ResolutionSelector.ALLOWED_RESOLUTIONS_SLOW
                    } else {
                        ResolutionSelector.ALLOWED_RESOLUTIONS_NORMAL
                    })
                    .build()
                CameraController.IMAGE_CAPTURE
            }
            CameraMode.VIDEO -> {
                // Fallback to highest supported video quality
                if (!supportedVideoQualities.contains(videoQuality)) {
                    videoQuality = supportedVideoQualities.first()
                }
                cameraController.videoCaptureTargetQuality = null // FIXME: video preview restart
                cameraController.videoCaptureTargetQuality = videoQuality

                // Set proper video frame rate
                videoFrameRate = (FrameRate::getLowerOrHigher)(
                    videoFrameRate ?: FrameRate.FPS_30, supportedVideoFrameRates
                )

                CameraController.VIDEO_CAPTURE
            }
        }

        photoCaptureMode = sharedPreferences.photoCaptureMode.takeIf {
            it != ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG || camera.supportsZsl
        } ?: ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY

        // Only photo mode supports vendor extensions for now
        val cameraSelector = if (
            cameraMode == CameraMode.PHOTO &&
            photoCaptureMode != ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG
        ) {
            cameraManager.extensionsManager.getExtensionEnabledCameraSelector(
                camera.cameraSelector, photoEffect
            )
        } else {
            camera.cameraSelector
        }

        // Setup UI depending on camera mode
        when (cameraMode) {
            CameraMode.QR -> {
                secondaryBarLayout.isVisible = false
                secondaryBarHalf.isVisible = false
                primaryBarLayoutGroupPhoto.isVisible = false
                googleLensButton.isVisible = isGoogleLensAvailable
            }
            CameraMode.PHOTO -> {
                secondaryBarLayout.isVisible = true
                secondaryBarHalf.isVisible = true
                primaryBarLayoutGroupPhoto.isVisible = true
                googleLensButton.isVisible = false
            }
            CameraMode.VIDEO -> {
                secondaryBarLayout.isVisible = true
                secondaryBarHalf.isVisible = true
                primaryBarLayoutGroupPhoto.isVisible = true
                googleLensButton.isVisible = false
            }
        }

        // Bind use cases to camera
        cameraController.cameraSelector = cameraSelector
        cameraController.setEnabledUseCases(cameraUseCases)

        // Restore settings that needs a rebind
        cameraController.imageCaptureMode = photoCaptureMode

        // Bind camera controller to lifecycle
        cameraController.bindToLifecycle(this)

        // Observe camera state
        camera.cameraState.observe(this) { cameraState ->
            cameraState.error?.let {
                // Log the error
                Log.e(LOG_TAG, "Error: code: ${it.code}, type: ${it.type}", it.cause)

                val showToast = { s: Int ->
                    Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
                }

                when (it.code) {
                    androidx.camera.core.CameraState.ERROR_MAX_CAMERAS_IN_USE -> {
                        // No way to fix it without user action, bail out
                        showToast(R.string.error_max_cameras_in_use)
                        finish()
                    }
                    androidx.camera.core.CameraState.ERROR_CAMERA_IN_USE -> {
                        // No way to fix it without user action, bail out
                        showToast(R.string.error_camera_in_use)
                        finish()
                    }
                    androidx.camera.core.CameraState.ERROR_OTHER_RECOVERABLE_ERROR -> {
                        // Warn the user and don't do anything
                        showToast(R.string.error_other_recoverable_error)
                    }
                    androidx.camera.core.CameraState.ERROR_STREAM_CONFIG -> {
                        // CameraX use case misconfiguration, no way to recover
                        showToast(R.string.error_stream_config)
                        finish()
                    }
                    androidx.camera.core.CameraState.ERROR_CAMERA_DISABLED -> {
                        // No way to fix it without user action, bail out
                        showToast(R.string.error_camera_disabled)
                        finish()
                    }
                    androidx.camera.core.CameraState.ERROR_CAMERA_FATAL_ERROR -> {
                        // No way to fix it without user action, bail out
                        showToast(R.string.error_camera_fatal_error)
                        finish()
                    }
                    androidx.camera.core.CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED -> {
                        // No way to fix it without user action, bail out
                        showToast(R.string.error_do_not_disturb_mode_enabled)
                        finish()
                    }
                    else ->  {
                        // We know anything about it, just check if it's recoverable or critical
                        when (it.type) {
                            androidx.camera.core.CameraState.ErrorType.RECOVERABLE -> {
                                showToast(R.string.error_unknown_recoverable)
                            }
                            androidx.camera.core.CameraState.ErrorType.CRITICAL -> {
                                showToast(R.string.error_unknown_critical)
                                finish()
                            }
                        }
                    }
                }
            }
        }

        // Wait for camera to be ready
        cameraController.initializationFuture.addListener({
            // Set Camera2 CaptureRequest options
            cameraController.camera2CameraControl?.apply {
                captureRequestOptions = CaptureRequestOptions.Builder()
                    .apply {
                        setFrameRate(
                            if (cameraMode == CameraMode.VIDEO) {
                                videoFrameRate
                            } else {
                                null
                            }
                        )
                        setVideoStabilizationMode(
                            if (cameraMode == CameraMode.VIDEO &&
                                sharedPreferences.videoStabilization
                            ) {
                                VideoStabilizationMode.getMode(camera)
                            } else {
                                VideoStabilizationMode.OFF
                            }
                        )
                    }
                    .build()
            } ?: Log.wtf(LOG_TAG, "Camera2CameraControl not available even with camera ready?")
        }, ContextCompat.getMainExecutor(this))

        // Restore settings that can be set on the fly
        changeGridMode(
            if (cameraMode != CameraMode.QR) gridMode else GridMode.OFF
        )
        changeFlashMode(
            when (cameraMode) {
                CameraMode.PHOTO -> sharedPreferences.photoFlashMode
                CameraMode.VIDEO -> sharedPreferences.videoFlashMode
                CameraMode.QR -> FlashMode.OFF
            }
        )
        setMicrophoneMode(videoMicMode)

        // Reset exposure level
        exposureLevel.progress = 0.5f
        exposureLevel.steps =
            camera.exposureCompensationRange.upper - camera.exposureCompensationRange.lower

        // Update lens selector
        secondaryBarLayout.lensSelectorLayout.setCamera(
            camera, cameraManager.getCameras(cameraMode, camera.cameraFacing)
        )
    }

    /**
     * Change the current camera mode and restarts the stream
     */
    private fun changeCameraMode(cameraMode: CameraMode) {
        if (!canRestartCamera()) {
            return
        }

        if (cameraMode == this.cameraMode) {
            return
        }

        when (cameraMode) {
            CameraMode.PHOTO -> {
                if (this.cameraMode == CameraMode.VIDEO) {
                    startShutterAnimation(ShutterAnimation.VideoToPhoto)
                } else {
                    startShutterAnimation(ShutterAnimation.InitPhoto)
                }
            }
            CameraMode.VIDEO -> {
                if (this.cameraMode == CameraMode.PHOTO) {
                    startShutterAnimation(ShutterAnimation.PhotoToVideo)
                } else {
                    startShutterAnimation(ShutterAnimation.InitVideo)
                }
            }
            else -> {}
        }

        this.cameraMode = cameraMode
        sharedPreferences.lastCameraMode = cameraMode

        // Close secondary top bar
        secondaryBarLayout.slideDown()

        bindCameraUseCases()
    }

    /**
     * Cycle between cameras
     */
    private fun flipCamera() {
        if (!canRestartCamera()) {
            return
        }

        (flipCameraButton.drawable as AnimatedVectorDrawable).start()

        camera = cameraManager.getNextCamera(camera, cameraMode)
        sharedPreferences.lastCameraFacing = camera.cameraFacing

        bindCameraUseCases()
    }

    private fun cycleAspectRatio() {
        if (!canRestartCamera()) {
            return
        }

        photoAspectRatio = when (photoAspectRatio) {
            AspectRatio.RATIO_4_3 -> AspectRatio.RATIO_16_9
            AspectRatio.RATIO_16_9 -> AspectRatio.RATIO_4_3
            else -> AspectRatio.RATIO_4_3
        }

        sharedPreferences.aspectRatio = photoAspectRatio

        bindCameraUseCases()
    }

    private fun cycleVideoQuality() {
        if (!canRestartCamera()) {
            return
        }

        val currentVideoQuality = videoQuality
        val newVideoQuality = supportedVideoQualities.toList().sortedWith { a, b ->
            listOf(Quality.SD, Quality.HD, Quality.FHD, Quality.UHD).let {
                it.indexOf(a) - it.indexOf(b)
            }
        }.next(currentVideoQuality)

        if (newVideoQuality == currentVideoQuality) {
            return
        }

        videoQuality = newVideoQuality

        sharedPreferences.videoQuality = videoQuality

        bindCameraUseCases()
    }

    private fun cycleVideoFrameRate() {
        if (!canRestartCamera()) {
            return
        }

        val currentVideoFrameRate = videoFrameRate
        val newVideoFrameRate = supportedVideoFrameRates.toList().sorted()
            .next(currentVideoFrameRate)

        if (newVideoFrameRate == currentVideoFrameRate) {
            return
        }

        videoFrameRate = newVideoFrameRate

        sharedPreferences.videoFrameRate = videoFrameRate

        bindCameraUseCases()
    }

    /**
     * Set the specified grid mode, also updating the icon
     */
    private fun cycleGridMode() {
        gridMode = gridMode.next()

        sharedPreferences.lastGridMode = gridMode

        changeGridMode(gridMode)
    }

    private fun changeGridMode(gridMode: GridMode) {
        gridView.mode = gridMode
    }

    /**
     * Toggle timer mode
     */
    private fun toggleTimerMode() {
        timerMode = timerMode.next()

        sharedPreferences.timerMode = timerMode
    }

    /**
     * Set the specified flash mode, saving the value to shared prefs and updating the icon
     */
    private fun changeFlashMode(flashMode: FlashMode) {
        cameraController.flashMode = flashMode

        this.flashMode = flashMode
    }

    /**
     * Cycle flash mode
     */
    private fun cycleFlashMode() {
        val currentFlashMode = flashMode
        val newFlashMode = when (cameraMode) {
            CameraMode.PHOTO -> FlashMode.PHOTO_ALLOWED_MODES.next(currentFlashMode)
            CameraMode.VIDEO -> FlashMode.VIDEO_ALLOWED_MODES.next(currentFlashMode)
            else -> FlashMode.OFF
        }

        changeFlashMode(newFlashMode)

        when (cameraMode) {
            CameraMode.PHOTO -> sharedPreferences.photoFlashMode = newFlashMode
            CameraMode.VIDEO -> sharedPreferences.videoFlashMode = newFlashMode
            else -> {}
        }
    }

    /**
     * Toggles microphone during video recording
     */
    private fun toggleMicrophoneMode() {
        setMicrophoneMode(!videoMicMode)
    }

    /**
     * Set the specified microphone mode, saving the value to shared prefs and updating the icon
     */
    @SuppressLint("MissingPermission")
    private fun setMicrophoneMode(microphoneMode: Boolean) {
        videoAudioConfig = AudioConfig.create(microphoneMode)
        videoRecording?.muted = !microphoneMode

        videoMicMode = microphoneMode

        sharedPreferences.lastMicMode = videoMicMode
    }

    /**
     * Cycle between supported photo camera effects
     */
    private fun cyclePhotoEffects() {
        if (!canRestartCamera()) {
            return
        }

        val currentExtensionMode = photoEffect
        val newExtensionMode = camera.supportedExtensionModes.next(currentExtensionMode)

        if (newExtensionMode == currentExtensionMode) {
            return
        }

        photoEffect = newExtensionMode

        sharedPreferences.photoEffect = photoEffect

        bindCameraUseCases()
    }

    private fun setBrightScreen(brightScreen: Boolean) {
        window.attributes = window.attributes.apply {
            screenBrightness =
                if (brightScreen) WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }

    private fun setLeveler(enabled: Boolean) {
        levelerView.isVisible = enabled
    }

    private fun updateGalleryButton() {
        runOnUiThread {
            val uri = sharedPreferences.lastSavedUri?.takeIf {
                MediaStoreUtils.fileExists(this, it)
            }
            val keyguardLocked = keyguardManager.isKeyguardLocked
            if (uri != null && (!keyguardLocked || tookSomething)) {
                galleryButton.load(uri) {
                    decoderFactory(VideoFrameDecoder.Factory())
                    crossfade(true)
                    scale(Scale.FILL)
                    size(75.px)
                    error(R.drawable.ic_image)
                    fallback(R.drawable.ic_image)
                    listener(object : ImageRequest.Listener {
                        override fun onSuccess(request: ImageRequest, result: SuccessResult) {
                            galleryButton.setPadding(0)
                            super.onSuccess(request, result)
                        }

                        override fun onError(request: ImageRequest, result: ErrorResult) {
                            galleryButton.setPadding(15.px)
                            super.onError(request, result)
                        }

                        override fun onCancel(request: ImageRequest) {
                            galleryButton.setPadding(15.px)
                            super.onCancel(request)
                        }
                    })
                }
            } else if (keyguardLocked) {
                galleryButton.setPadding(15.px)
                galleryButton.setImageResource(R.drawable.ic_lock)
            } else {
                galleryButton.setPadding(15.px)
                galleryButton.setImageResource(R.drawable.ic_image)
            }
        }
    }

    private fun dismissKeyguardAndRun(runnable: () -> Unit) {
        if (!keyguardManager.isKeyguardLocked) {
            runnable()
            return
        }

        keyguardManager.requestDismissKeyguard(
            this,
            object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    super.onDismissSucceeded()
                    runnable()
                }
            }
        )
    }

    private fun openGallery() {
        sharedPreferences.lastSavedUri.let { uri ->
            // If the Uri is null, attempt to launch non secure-gallery
            if (uri == null) {
                dismissKeyguardAndRun {
                    val intent = Intent().apply {
                        action = Intent.ACTION_VIEW
                        type = "image/*"
                    }
                    runCatching {
                        startActivity(intent)
                        return@dismissKeyguardAndRun
                    }
                }
                return
            }

            // This ensure we took at least one photo/video in the secure use-case
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                tookSomething && keyguardManager.isKeyguardLocked
            ) {
                val intent = Intent().apply {
                    action = MediaStore.ACTION_REVIEW_SECURE
                    data = uri
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                runCatching {
                    startActivity(intent)
                    return
                }
            }

            // Try to open the Uri in the non secure gallery
            dismissKeyguardAndRun {
                mutableListOf<String>().apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        add(MediaStore.ACTION_REVIEW)
                    }
                    add(Intent.ACTION_VIEW)
                }.forEach {
                    val intent = Intent().apply {
                        action = it
                        data = uri
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    runCatching {
                        startActivity(intent)
                        return@dismissKeyguardAndRun
                    }
                }
            }
        }
    }

    private fun openCapturePreview(uri: Uri, mediaType: MediaType) {
        runOnUiThread {
            capturePreviewLayout.updateSource(uri, mediaType)
            capturePreviewLayout.isVisible = true
        }
    }

    private fun openCapturePreview(photoInputStream: InputStream) {
        runOnUiThread {
            capturePreviewLayout.updateSource(photoInputStream)
            capturePreviewLayout.isVisible = true
        }
    }

    /**
     * When the user took a photo or a video and confirmed it, its URI gets sent back to the
     * app that sent the intent and closes the camera.
     */
    private fun sendIntentResultAndExit(input: Any) {
        // The user confirmed the choice
        var outputUri: Uri? = null
        if (intent.extras?.containsKey(MediaStore.EXTRA_OUTPUT) == true) {
            outputUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.extras?.getParcelable(MediaStore.EXTRA_OUTPUT, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.extras?.get(MediaStore.EXTRA_OUTPUT) as Uri
            }
        }

        outputUri?.let {
            try {
                contentResolver.openOutputStream(it, "wt").use { outputStream ->
                    when (input) {
                        is InputStream -> input.use {
                            input.copyTo(outputStream!!)
                        }
                        is Uri -> contentResolver.openInputStream(input).use { inputStream ->
                            inputStream!!.copyTo(outputStream!!)
                        }
                        else -> throw IllegalStateException("Input is not Uri or InputStream")
                    }
                }

                setResult(RESULT_OK)
            } catch (exc: FileNotFoundException) {
                Log.e(LOG_TAG, "Failed to open URI")
                setResult(RESULT_CANCELED)
            }
        } ?: setResult(RESULT_OK, Intent().apply {
            when (input) {
                is InputStream -> {
                    // No output URI provided, so return the photo inline as a downscaled Bitmap.
                    action = "inline-data"
                    val transform = ExifUtils.getTransform(input)
                    val bitmap = input.use { BitmapFactory.decodeStream(input) }
                    val scaledAndRotatedBitmap = bitmap.scale(
                        SINGLE_CAPTURE_INLINE_MAX_SIDE_LEN_PIXELS
                    ).transform(transform)
                    putExtra("data", scaledAndRotatedBitmap)
                }
                is Uri -> {
                    // We saved the media (video), so return the URI that we saved.
                    data = input
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    putExtra(MediaStore.EXTRA_OUTPUT, input)
                }
                else -> throw IllegalStateException("Input is not Uri or InputStream")
            }
        })

        finish()
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun hideStatusBars() {
        val windowInsetsController = getInsetsController(window, window.decorView)
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide the status bar
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
    }

    private fun startTimerAndRun(runnable: () -> Unit) {
        // Allow forcing timer if requested by the assistant
        val timerModeSeconds =
            assistantIntent?.timerDurationSeconds ?: timerMode.seconds

        if (timerModeSeconds <= 0 || !canRestartCamera()) {
            runnable()
            return
        }

        shutterButton.isEnabled = cameraMode == CameraMode.VIDEO

        countDownView.onPreviewAreaChanged(Rect().apply {
            viewFinder.getGlobalVisibleRect(this)
        })
        countDownView.startCountDown(timerModeSeconds) {
            shutterButton.isEnabled = true
            runnable()
        }
    }

    private fun rotateViews(screenRotation: Rotation) {
        val compensationValue = screenRotation.compensationValue.toFloat()

        // Rotate sliders
        exposureLevel.screenRotation = screenRotation
        zoomLevel.screenRotation = screenRotation

        // Rotate primary bar buttons
        galleryButtonCardView.smoothRotate(compensationValue)
        shutterButton.smoothRotate(compensationValue)
        flipCameraButton.smoothRotate(compensationValue)
    }

    /**
     * Zoom in by a power of 2.
     */
    private fun zoomIn() {
        val acquired = zoomGestureSemaphore.tryAcquire()
        if (!acquired) {
            return
        }

        val zoomState = cameraController.zoomState.value ?: return

        ValueAnimator.ofFloat(
            zoomState.zoomRatio,
            zoomState.zoomRatio.nextPowerOfTwo().takeUnless {
                it > zoomState.maxZoomRatio
            } ?: zoomState.maxZoomRatio
        ).apply {
            addUpdateListener {
                cameraController.setZoomRatio(it.animatedValue as Float)
            }
            addListener(onEnd = {
                zoomGestureSemaphore.release()
            })
        }.start()
    }

    /**
     * Zoom out by a power of 2.
     */
    private fun zoomOut() {
        val acquired = zoomGestureSemaphore.tryAcquire()
        if (!acquired) {
            return
        }

        val zoomState = cameraController.zoomState.value ?: return

        ValueAnimator.ofFloat(
            zoomState.zoomRatio,
            zoomState.zoomRatio.previousPowerOfTwo().takeUnless {
                it < zoomState.minZoomRatio
            } ?: zoomState.minZoomRatio
        ).apply {
            addUpdateListener {
                cameraController.setZoomRatio(it.animatedValue as Float)
            }
            addListener(onEnd = {
                zoomGestureSemaphore.release()
            })
        }.start()
    }

    /**
     * Use this function when the app must be closed due to emergency reasons.
     * It will try to save whatever is going on and close the app.
     */
    private fun emergencyClose() {
        // Stop the recording if there's an active one
        if (cameraController.isRecording) {
            videoRecording?.stop()
        }

        // Close the app
        finish()
    }

    fun preventClicks(@Suppress("UNUSED_PARAMETER") view: View) {}

    companion object {
        private const val LOG_TAG = "Aperture"

        private const val MSG_HIDE_ZOOM_SLIDER = 0
        private const val MSG_HIDE_FOCUS_RING = 1
        private const val MSG_HIDE_EXPOSURE_SLIDER = 2
        private const val MSG_ON_PINCH_TO_ZOOM = 3

        private const val SINGLE_CAPTURE_PHOTO_BUFFER_INITIAL_SIZE_BYTES = 8 * 1024 * 1024 // 8 MiB

        // We need to return something small enough so as not to overwhelm Binder. 1MB is the
        // per-process limit across all transactions. Camera2 sets a max pixel count of 51200.
        // We set a max side length of 256, for a max pixel count of 65536. Even at 4 bytes per
        // pixel, this is only 256K, well within the limits. (Note: It's not clear if any modern
        // app expects a photo to be returned inline, rather than providing an output URI.)
        // https://developer.android.com/guide/components/activities/parcelables-and-bundles#sdbp
        private const val SINGLE_CAPTURE_INLINE_MAX_SIDE_LEN_PIXELS = 256

        private val EXPOSURE_LEVEL_FORMATTER = DecimalFormat("+#;-#")
    }
}
