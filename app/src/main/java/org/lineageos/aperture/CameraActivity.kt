/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.icu.text.DecimalFormat
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.MediaStore
import android.util.Log
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.extensions.ExtensionMode
import androidx.camera.video.Quality
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.onPinchToZoom
import androidx.camera.view.video.AudioConfig
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
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
import org.lineageos.aperture.ui.CapturePreviewLayout
import org.lineageos.aperture.ui.CountDownView
import org.lineageos.aperture.ui.GridView
import org.lineageos.aperture.ui.HorizontalSlider
import org.lineageos.aperture.ui.LensSelectorLayout
import org.lineageos.aperture.ui.LevelerView
import org.lineageos.aperture.ui.PreviewBlurView
import org.lineageos.aperture.ui.VerticalSlider
import org.lineageos.aperture.utils.Camera
import org.lineageos.aperture.utils.CameraFacing
import org.lineageos.aperture.utils.CameraManager
import org.lineageos.aperture.utils.CameraMode
import org.lineageos.aperture.utils.CameraSoundsUtils
import org.lineageos.aperture.utils.CameraState
import org.lineageos.aperture.utils.FlashMode
import org.lineageos.aperture.utils.Framerate
import org.lineageos.aperture.utils.GridMode
import org.lineageos.aperture.utils.MediaType
import org.lineageos.aperture.utils.PermissionsUtils
import org.lineageos.aperture.utils.ShortcutsUtils
import org.lineageos.aperture.utils.StabilizationMode
import org.lineageos.aperture.utils.StorageUtils
import org.lineageos.aperture.utils.TimeUtils
import org.lineageos.aperture.utils.TimerMode
import java.io.FileNotFoundException
import java.util.concurrent.ExecutorService
import kotlin.math.abs

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
@androidx.camera.core.ExperimentalZeroShutterLag
@androidx.camera.view.video.ExperimentalVideo
@androidx.media3.common.util.UnstableApi
open class CameraActivity : AppCompatActivity() {
    // Views
    private val aspectRatioButton by lazy { findViewById<Button>(R.id.aspectRatioButton) }
    private val cameraModeHighlight by lazy { findViewById<MaterialButton>(R.id.cameraModeHighlight) }
    private val capturePreviewLayout by lazy { findViewById<CapturePreviewLayout>(R.id.capturePreviewLayout) }
    private val countDownView by lazy { findViewById<CountDownView>(R.id.countDownView) }
    private val effectButton by lazy { findViewById<Button>(R.id.effectButton) }
    private val exposureLevel by lazy { findViewById<VerticalSlider>(R.id.exposureLevel) }
    private val flashButton by lazy { findViewById<ImageButton>(R.id.flashButton) }
    private val flipCameraButton by lazy { findViewById<ImageButton>(R.id.flipCameraButton) }
    private val galleryButton by lazy { findViewById<ImageView>(R.id.galleryButton) }
    private val galleryButtonCardView by lazy { findViewById<CardView>(R.id.galleryButtonCardView) }
    private val gridButton by lazy { findViewById<Button>(R.id.gridButton) }
    private val gridView by lazy { findViewById<GridView>(R.id.gridView) }
    private val lensSelectorLayout by lazy { findViewById<LensSelectorLayout>(R.id.lensSelectorLayout) }
    private val levelerView by lazy { findViewById<LevelerView>(R.id.levelerView) }
    private val micButton by lazy { findViewById<Button>(R.id.micButton) }
    private val photoModeButton by lazy { findViewById<MaterialButton>(R.id.photoModeButton) }
    private val previewBlurView by lazy { findViewById<PreviewBlurView>(R.id.previewBlurView) }
    private val primaryBarLayout by lazy { findViewById<ConstraintLayout>(R.id.primaryBarLayout) }
    private val proButton by lazy { findViewById<ImageButton>(R.id.proButton) }
    private val qrModeButton by lazy { findViewById<MaterialButton>(R.id.qrModeButton) }
    private val secondaryBottomBarLayout by lazy { findViewById<ConstraintLayout>(R.id.secondaryBottomBarLayout) }
    private val secondaryTopBarLayout by lazy { findViewById<HorizontalScrollView>(R.id.secondaryTopBarLayout) }
    private val settingsButton by lazy { findViewById<Button>(R.id.settingsButton) }
    private val shutterButton by lazy { findViewById<ImageButton>(R.id.shutterButton) }
    private val timerButton by lazy { findViewById<Button>(R.id.timerButton) }
    private val videoDuration by lazy { findViewById<MaterialButton>(R.id.videoDuration) }
    private val videoFramerateButton by lazy { findViewById<Button>(R.id.videoFramerateButton) }
    private val videoModeButton by lazy { findViewById<MaterialButton>(R.id.videoModeButton) }
    private val videoQualityButton by lazy { findViewById<Button>(R.id.videoQualityButton) }
    private val videoRecordingStateButton by lazy { findViewById<ImageButton>(R.id.videoRecordingStateButton) }
    private val viewFinder by lazy { findViewById<PreviewView>(R.id.viewFinder) }
    private val viewFinderFocus by lazy { findViewById<ImageView>(R.id.viewFinderFocus) }
    private val zoomLevel by lazy { findViewById<HorizontalSlider>(R.id.zoomLevel) }

    // System services
    private val keyguardManager by lazy { getSystemService(KeyguardManager::class.java) }
    private val locationManager by lazy { getSystemService(LocationManager::class.java) }

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
    private lateinit var camera: Camera
    private lateinit var cameraMode: CameraMode
    private lateinit var initialCameraFacing: CameraFacing
    private var singleCaptureMode = false
        set(value) {
            field = value
            updateSecondaryBarButtons()
            updatePrimaryBarButtons()
            updateCameraModeButtons()
        }
    private var cameraState = CameraState.IDLE
        set(value) {
            field = value
            updateSecondaryBarButtons()
            updatePrimaryBarButtons()
        }
    private var tookSomething: Boolean = false
        set(value) {
            field = value
            updateGalleryButton()
        }

    // Video
    private val supportedVideoQualities: List<Quality>
        get() = camera.supportedVideoQualities.keys.toList()
    private val supportedVideoFramerates: List<Framerate>
        get() = camera.supportedVideoQualities.getOrDefault(
            sharedPreferences.videoQuality, listOf()
        )
    private lateinit var audioConfig: AudioConfig
    private var recording: Recording? = null

    // QR
    private val imageAnalyzer by lazy { QrImageAnalyzer(this) }

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
    private val locationListener = object : LocationListener {
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
                    locationManager.requestLocationUpdates(it, 1000, 1f, this)
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

    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (it.isNotEmpty()) {
            if (!permissionsUtils.mainPermissionsGranted()) {
                Toast.makeText(
                    this, getString(R.string.app_permissions_toast), Toast.LENGTH_SHORT
                ).show()
                finish()
            }
            sharedPreferences.saveLocation = permissionsUtils.locationPermissionsGranted()
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideStatusBars()

        setContentView(R.layout.activity_camera)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
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

        // Handle intent
        intent.action?.let {
            intentActions[it]?.invoke()
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
        aspectRatioButton.setOnClickListener { cycleAspectRatio() }
        videoQualityButton.setOnClickListener { cycleVideoQuality() }
        videoFramerateButton.setOnClickListener { cycleVideoFramerate() }
        effectButton.setOnClickListener { cyclePhotoEffects() }
        gridButton.setOnClickListener { cycleGridMode() }
        timerButton.setOnClickListener { toggleTimerMode() }
        micButton.setOnClickListener { toggleMicrophoneMode() }
        settingsButton.setOnClickListener { openSettings() }

        // Set secondary bottom bar button callbacks
        proButton.setOnClickListener {
            secondaryTopBarLayout.slide()
        }
        flashButton.setOnClickListener { cycleFlashMode() }

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
            updateFlashModeIcon()
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

            secondaryTopBarLayout.slideDown()
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

            lensSelectorLayout.onZoomRatioChanged(it.zoomRatio)
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

        videoRecordingStateButton.setOnClickListener {
            when (cameraState) {
                CameraState.RECORDING_VIDEO -> recording?.pause()
                CameraState.RECORDING_VIDEO_PAUSED -> recording?.resume()
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
        lensSelectorLayout.onCameraChangeCallback = {
            if (canRestartCamera()) {
                camera = it
                bindCameraUseCases()
            }
        }
        lensSelectorLayout.onZoomRatioChangeCallback = {
            cameraController.setZoomRatio(it)
        }

        // Set capture preview callback
        capturePreviewLayout.onChoiceCallback = { uri ->
            uri?.let {
                sendIntentResultAndExit(it)
            } ?: run {
                capturePreviewLayout.isVisible = false
            }
        }

        // Bind viewfinder and preview blur view
        previewBlurView.previewView = viewFinder
    }

    override fun onResume() {
        super.onResume()

        // Request camera permissions
        if (!permissionsUtils.mainPermissionsGranted() || sharedPreferences.saveLocation == null) {
            requestMultiplePermissions.launch(PermissionsUtils.allPermissions)
        }

        // Set bright screen
        setBrightScreen(sharedPreferences.brightScreen)

        // Set leveler
        setLeveler(sharedPreferences.leveler)

        // Reset tookSomething state
        tookSomething = false

        // Register location updates
        locationListener.register()

        // Re-bind the use cases
        bindCameraUseCases()
    }

    override fun onPause() {
        // Remove location and location updates
        locationListener.unregister()

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
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (cameraMode == CameraMode.VIDEO && shutterButton.isEnabled && event?.repeatCount == 1) {
                    shutterButton.performClick()
                }
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return if (capturePreviewLayout.isVisible) {
            super.onKeyUp(keyCode, event)
        } else when (keyCode) {
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (cameraMode != CameraMode.QR && shutterButton.isEnabled) {
                    shutterButton.performClick()
                }
                true
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

        // Create output options object which contains file + metadata
        val outputOptions = StorageUtils.getPhotoMediaStoreOutputOptions(
            contentResolver,
            ImageCapture.Metadata().apply {
                location = this@CameraActivity.location
            }
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
                    } else {
                        output.savedUri?.let {
                            openCapturePreview(it, MediaType.PHOTO)
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
                recording?.stop()
            }
            return
        }

        // Disallow state changes while we are about to prepare for recording video
        cameraState = CameraState.PRE_RECORDING_VIDEO

        // Create output options object which contains file + metadata
        val outputOptions = StorageUtils.getVideoMediaStoreOutputOptions(contentResolver, location)

        // Play shutter sound
        val delayTime = if (cameraSoundsUtils.playStartVideoRecording()) 500L else 0L

        handler.postDelayed({
            // Start recording
            recording = cameraController.startRecording(
                outputOptions,
                audioConfig,
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
                            } else {
                                openCapturePreview(it.outputResults.outputUri, MediaType.VIDEO)
                            }
                        }
                        cameraState = CameraState.IDLE
                        recording = null
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
        if (!camera.supportsExtensionMode(sharedPreferences.photoEffect)) {
            sharedPreferences.photoEffect = ExtensionMode.NONE
        }

        // Initialize the use case we want and set its properties
        val cameraUseCases = when (cameraMode) {
            CameraMode.QR -> {
                cameraController.setImageAnalysisAnalyzer(cameraExecutor, imageAnalyzer)
                CameraController.IMAGE_ANALYSIS
            }
            CameraMode.PHOTO -> {
                cameraController.imageCaptureTargetSize = CameraController.OutputSize(
                    sharedPreferences.aspectRatio
                )
                CameraController.IMAGE_CAPTURE
            }
            CameraMode.VIDEO -> {
                // Fallback to highest supported video quality
                if (!supportedVideoQualities.contains(sharedPreferences.videoQuality)) {
                    sharedPreferences.videoQuality = supportedVideoQualities.first()
                }
                cameraController.videoCaptureTargetQuality = sharedPreferences.videoQuality

                // Set proper video framerate
                sharedPreferences.videoFramerate = (Framerate::getLowerOrHigher)(
                    sharedPreferences.videoFramerate ?: Framerate.FPS_30, supportedVideoFramerates
                )

                CameraController.VIDEO_CAPTURE
            }
        }

        // Only photo mode supports vendor extensions for now
        val cameraSelector = if (cameraMode == CameraMode.PHOTO) {
            cameraManager.extensionsManager.getExtensionEnabledCameraSelector(
                camera.cameraSelector, sharedPreferences.photoEffect
            )
        } else {
            camera.cameraSelector
        }

        // Setup UI depending on camera mode
        when (cameraMode) {
            CameraMode.QR -> {
                timerButton.isVisible = false
                secondaryBottomBarLayout.isVisible = false
                primaryBarLayout.isVisible = false
            }
            CameraMode.PHOTO -> {
                timerButton.isVisible = true
                secondaryBottomBarLayout.isVisible = true
                primaryBarLayout.isVisible = true
            }
            CameraMode.VIDEO -> {
                timerButton.isVisible = true
                secondaryBottomBarLayout.isVisible = true
                primaryBarLayout.isVisible = true
            }
        }

        // Bind use cases to camera
        cameraController.cameraSelector = cameraSelector
        cameraController.setEnabledUseCases(cameraUseCases)

        // Restore settings that needs a rebind
        cameraController.imageCaptureMode = sharedPreferences.photoCaptureMode

        // Bind camera controller to lifecycle
        cameraController.bindToLifecycle(this)

        // Wait for camera to be ready
        cameraController.initializationFuture.addListener({
            // Set Camera2 CaptureRequest options
            cameraController.camera2CameraControl?.apply {
                captureRequestOptions = CaptureRequestOptions.Builder()
                    .apply {
                        setFramerate(
                            if (cameraMode == CameraMode.VIDEO) {
                                sharedPreferences.videoFramerate
                            } else {
                                null
                            }
                        )
                        setStabilizationMode(
                            (StabilizationMode::getClosestMode)(
                                when (cameraMode) {
                                    CameraMode.PHOTO -> sharedPreferences.imageStabilizationMode
                                    CameraMode.VIDEO -> sharedPreferences.videoStabilizationMode
                                    CameraMode.QR -> StabilizationMode.OFF
                                },
                                camera, cameraMode
                            )
                        )
                    }
                    .build()
            } ?: Log.wtf(LOG_TAG, "Camera2CameraControl not available even with camera ready?")
        }, ContextCompat.getMainExecutor(this))

        // Restore settings that can be set on the fly
        setGridMode(
            if (cameraMode != CameraMode.QR) sharedPreferences.lastGridMode else GridMode.OFF
        )
        setFlashMode(
            when (cameraMode) {
                CameraMode.PHOTO -> sharedPreferences.photoFlashMode
                CameraMode.VIDEO -> sharedPreferences.videoFlashMode
                CameraMode.QR -> FlashMode.OFF
            }
        )
        setMicrophoneMode(sharedPreferences.lastMicMode)

        // Reset exposure level
        exposureLevel.progress = 0.5f
        exposureLevel.steps =
            camera.exposureCompensationRange.upper - camera.exposureCompensationRange.lower

        // Update icons from last state
        updateCameraModeButtons()
        updateTimerModeIcon()
        updateAspectRatioIcon()
        updateVideoQualityIcon()
        updateVideoFramerateIcon()
        updatePhotoEffectIcon()
        updateGridIcon()
        updateFlashModeIcon()
        updateMicrophoneModeIcon()

        // Update lens selector
        lensSelectorLayout.setCamera(
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

        // Hide secondary top bar
        secondaryTopBarLayout.slideDown()

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

    /**
     * Update the camera mode buttons reflecting the current mode
     */
    private fun updateCameraModeButtons() {
        runOnUiThread {
            cameraModeHighlight.isInvisible = singleCaptureMode
            photoModeButton.isInvisible = singleCaptureMode
            videoModeButton.isInvisible = singleCaptureMode
            qrModeButton.isInvisible = singleCaptureMode

            cameraMode.let {
                qrModeButton.isEnabled = it != CameraMode.QR
                photoModeButton.isEnabled = it != CameraMode.PHOTO
                videoModeButton.isEnabled = it != CameraMode.VIDEO
            }

            // Animate camera mode change
            (cameraModeHighlight.parent as View).doOnLayout {
                ValueAnimator.ofFloat(
                    cameraModeHighlight.x, when (cameraMode) {
                        CameraMode.QR -> qrModeButton.x
                        CameraMode.PHOTO -> photoModeButton.x
                        CameraMode.VIDEO -> videoModeButton.x
                    }
                ).apply {
                    addUpdateListener {
                        cameraModeHighlight.x = it.animatedValue as Float
                    }
                }.start()
            }
        }
    }

    /**
     * Enable or disable secondary bar buttons
     */
    private fun updateSecondaryBarButtons() {
        runOnUiThread {
            timerButton.isEnabled = cameraState == CameraState.IDLE
            aspectRatioButton.isEnabled = cameraState == CameraState.IDLE
            videoQualityButton.isEnabled = cameraState == CameraState.IDLE
            videoFramerateButton.isEnabled = cameraState == CameraState.IDLE
            effectButton.isEnabled = cameraState == CameraState.IDLE
            // Grid mode can be toggled at any time
            // Torch mode can be toggled at any time
            flashButton.isEnabled =
                cameraMode != CameraMode.PHOTO || cameraState == CameraState.IDLE
            micButton.isEnabled = cameraState == CameraState.IDLE
            settingsButton.isEnabled = cameraState == CameraState.IDLE
        }
    }

    /**
     * Enable or disable primary bar buttons
     */
    private fun updatePrimaryBarButtons() {
        runOnUiThread {
            galleryButtonCardView.isInvisible = singleCaptureMode
            galleryButton.isEnabled = cameraState == CameraState.IDLE
            // Shutter button must stay enabled
            flipCameraButton.isEnabled = cameraState == CameraState.IDLE
        }
    }

    private fun cycleAspectRatio() {
        if (!canRestartCamera()) {
            return
        }

        sharedPreferences.aspectRatio = when (sharedPreferences.aspectRatio) {
            AspectRatio.RATIO_4_3 -> AspectRatio.RATIO_16_9
            AspectRatio.RATIO_16_9 -> AspectRatio.RATIO_4_3
            else -> AspectRatio.RATIO_4_3
        }

        bindCameraUseCases()
    }

    private fun cycleVideoQuality() {
        if (!canRestartCamera()) {
            return
        }

        val currentVideoQuality = sharedPreferences.videoQuality
        val newVideoQuality = supportedVideoQualities.next(currentVideoQuality)

        if (newVideoQuality == currentVideoQuality) {
            return
        }

        sharedPreferences.videoQuality = newVideoQuality

        bindCameraUseCases()
    }

    private fun updateVideoFramerateIcon() {
        videoFramerateButton.isEnabled = supportedVideoFramerates.size > 1
        videoFramerateButton.isVisible = cameraMode == CameraMode.VIDEO

        videoFramerateButton.text = sharedPreferences.videoFramerate?.let {
            resources.getString(R.string.video_framerate_value, it.value)
        } ?: resources.getString(R.string.video_framerate_auto)
    }

    private fun cycleVideoFramerate() {
        if (!canRestartCamera()) {
            return
        }

        val currentVideoFramerate = sharedPreferences.videoFramerate
        val newVideoFramerate = supportedVideoFramerates.next(currentVideoFramerate)

        if (newVideoFramerate == currentVideoFramerate) {
            return
        }

        sharedPreferences.videoFramerate = newVideoFramerate
        bindCameraUseCases()
    }

    /**
     * Update the grid button icon based on the value set in grid view
     */
    private fun updateGridIcon() {
        sharedPreferences.lastGridMode.let {
            gridButton.setCompoundDrawablesWithIntrinsicBounds(
                0,
                when (it) {
                    GridMode.OFF -> R.drawable.ic_grid_off
                    GridMode.ON_3 -> R.drawable.ic_grid_on_3
                    GridMode.ON_4 -> R.drawable.ic_grid_on_4
                    GridMode.ON_GOLDENRATIO -> R.drawable.ic_grid_on_goldenratio
                },
                0,
                0
            )
            gridButton.text = resources.getText(
                when (it) {
                    GridMode.OFF -> R.string.grid_off
                    GridMode.ON_3 -> R.string.grid_on_3
                    GridMode.ON_4 -> R.string.grid_on_4
                    GridMode.ON_GOLDENRATIO -> R.string.grid_on_goldenratio
                }
            )
        }
    }

    /**
     * Set the specified grid mode, also updating the icon
     */
    private fun cycleGridMode() {
        sharedPreferences.lastGridMode = sharedPreferences.lastGridMode.next()
        setGridMode(sharedPreferences.lastGridMode)
    }

    private fun setGridMode(gridMode: GridMode) {
        gridView.mode = gridMode
        updateGridIcon()
    }

    /**
     * Update the timer mode button icon based on the value set in settings
     */
    private fun updateTimerModeIcon() {
        sharedPreferences.timerMode.let {
            timerButton.setCompoundDrawablesWithIntrinsicBounds(
                0,
                when (it) {
                    TimerMode.OFF -> R.drawable.ic_timer_off
                    TimerMode.ON_3S -> R.drawable.ic_timer_3
                    TimerMode.ON_10S -> R.drawable.ic_timer_10
                },
                0,
                0
            )
            timerButton.text = resources.getText(
                when (it) {
                    TimerMode.OFF -> R.string.timer_off
                    TimerMode.ON_3S -> R.string.timer_3
                    TimerMode.ON_10S -> R.string.timer_10
                }
            )
        }
    }

    /**
     * Toggle timer mode
     */
    private fun toggleTimerMode() {
        sharedPreferences.timerMode = sharedPreferences.timerMode.next()
        updateTimerModeIcon()
    }

    private fun updateAspectRatioIcon() {
        aspectRatioButton.isVisible = cameraMode != CameraMode.VIDEO

        sharedPreferences.aspectRatio.let {
            aspectRatioButton.setCompoundDrawablesWithIntrinsicBounds(
                0,
                when (it) {
                    AspectRatio.RATIO_4_3 -> R.drawable.ic_aspect_ratio_4_3
                    AspectRatio.RATIO_16_9 -> R.drawable.ic_aspect_ratio_16_9
                    else -> throw Exception("Unknown aspect ratio $it")
                },
                0,
                0
            )
            aspectRatioButton.text = resources.getText(
                when (it) {
                    AspectRatio.RATIO_4_3 -> R.string.aspect_ratio_4_3
                    AspectRatio.RATIO_16_9 -> R.string.aspect_ratio_16_9
                    else -> throw Exception("Unknown aspect ratio $it")
                }
            )
        }
    }

    private fun updateVideoQualityIcon() {
        videoQualityButton.isVisible = cameraMode == CameraMode.VIDEO

        sharedPreferences.videoQuality.let {
            videoQualityButton.setCompoundDrawablesWithIntrinsicBounds(
                0,
                when (it) {
                    Quality.SD -> R.drawable.ic_video_quality_sd
                    Quality.HD -> R.drawable.ic_video_quality_hd
                    Quality.FHD -> R.drawable.ic_video_quality_hd
                    Quality.UHD -> R.drawable.ic_video_quality_uhd
                    else -> throw Exception("Unknown video quality $it")
                },
                0,
                0
            )
            videoQualityButton.text = resources.getText(
                when (it) {
                    Quality.SD -> R.string.video_quality_sd
                    Quality.HD -> R.string.video_quality_hd
                    Quality.FHD -> R.string.video_quality_fhd
                    Quality.UHD -> R.string.video_quality_uhd
                    else -> throw Exception("Unknown video quality $it")
                }
            )
        }
    }

    /**
     * Update the flash mode button icon based on the value set in imageCapture
     */
    private fun updateFlashModeIcon() {
        flashButton.isVisible = camera.hasFlashUnit

        cameraController.flashMode.let {
            flashButton.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    when (it) {
                        FlashMode.OFF -> R.drawable.ic_flash_off
                        FlashMode.AUTO -> R.drawable.ic_flash_auto
                        FlashMode.ON -> R.drawable.ic_flash_on
                        FlashMode.TORCH -> R.drawable.ic_flash_torch
                    }
                )
            )
        }
    }

    /**
     * Set the specified flash mode, saving the value to shared prefs and updating the icon
     */
    private fun setFlashMode(flashMode: FlashMode) {
        cameraController.flashMode = flashMode
        updateFlashModeIcon()

        when (cameraMode) {
            CameraMode.PHOTO -> sharedPreferences.photoFlashMode = flashMode
            CameraMode.VIDEO -> sharedPreferences.videoFlashMode = flashMode
            else -> {}
        }
    }

    /**
     * Cycle flash mode
     */
    private fun cycleFlashMode() {
        setFlashMode(
            when (cameraMode) {
                CameraMode.PHOTO -> cameraController.flashMode.next()
                CameraMode.VIDEO ->
                    if (cameraController.flashMode != FlashMode.OFF) {
                        FlashMode.OFF
                    } else {
                        FlashMode.TORCH
                    }
                else -> FlashMode.OFF
            }
        )
    }

    /**
     * Update the microphone mode button icon based on the value set in audioConfig
     */
    private fun updateMicrophoneModeIcon() {
        micButton.isVisible = cameraMode == CameraMode.VIDEO

        audioConfig.audioEnabled.let {
            micButton.setCompoundDrawablesWithIntrinsicBounds(
                0,
                if (it) R.drawable.ic_mic_on else R.drawable.ic_mic_off,
                0,
                0
            )
            micButton.text = resources.getText(if (it) R.string.mic_on else R.string.mic_off)
        }
    }

    /**
     * Toggles microphone during video recording
     */
    private fun toggleMicrophoneMode() {
        setMicrophoneMode(!audioConfig.audioEnabled)
    }

    /**
     * Set the specified microphone mode, saving the value to shared prefs and updating the icon
     */
    @SuppressLint("MissingPermission")
    private fun setMicrophoneMode(microphoneMode: Boolean) {
        audioConfig = AudioConfig.create(microphoneMode)
        updateMicrophoneModeIcon()

        sharedPreferences.lastMicMode = microphoneMode
    }

    /**
     * Update the photo effect icon based on the current value of extensionMode
     */
    private fun updatePhotoEffectIcon() {
        effectButton.isVisible =
            cameraMode == CameraMode.PHOTO && camera.supportedExtensionModes.size > 1

        sharedPreferences.photoEffect.let {
            effectButton.setCompoundDrawablesWithIntrinsicBounds(
                0,
                when (it) {
                    ExtensionMode.NONE -> R.drawable.ic_effect_none
                    ExtensionMode.BOKEH -> R.drawable.ic_effect_bokeh
                    ExtensionMode.HDR -> R.drawable.ic_effect_hdr
                    ExtensionMode.NIGHT -> R.drawable.ic_effect_night
                    ExtensionMode.FACE_RETOUCH -> R.drawable.ic_effect_face_retouch
                    ExtensionMode.AUTO -> R.drawable.ic_effect_auto
                    else -> R.drawable.ic_effect_none
                },
                0,
                0
            )
            effectButton.text = resources.getText(
                when (it) {
                    ExtensionMode.NONE -> R.string.effect_none
                    ExtensionMode.BOKEH -> R.string.effect_bokeh
                    ExtensionMode.HDR -> R.string.effect_hdr
                    ExtensionMode.NIGHT -> R.string.effect_night
                    ExtensionMode.FACE_RETOUCH -> R.string.effect_face_retouch
                    ExtensionMode.AUTO -> R.string.effect_auto
                    else -> R.string.effect_none
                }
            )
        }
    }

    /**
     * Cycle between supported photo camera effects
     */
    private fun cyclePhotoEffects() {
        if (!canRestartCamera()) {
            return
        }

        val currentExtensionMode = sharedPreferences.photoEffect
        val newExtensionMode = camera.supportedExtensionModes.next(currentExtensionMode)

        if (newExtensionMode == currentExtensionMode) {
            return
        }

        sharedPreferences.photoEffect = newExtensionMode

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
            val uri = sharedPreferences.lastSavedUri
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
            capturePreviewLayout.updateUri(uri, mediaType)
            capturePreviewLayout.isVisible = true
        }
    }

    /**
     * When the user took a photo or a video and confirmed it, its URI gets sent back to the
     * app that sent the intent and closes the camera.
     */
    private fun sendIntentResultAndExit(uri: Uri) {
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
                contentResolver.openInputStream(uri).use { inputStream ->
                    contentResolver.openOutputStream(it).use { outputStream ->
                        inputStream!!.copyTo(outputStream!!)
                    }
                }

                setResult(RESULT_OK)
            } catch (exc: FileNotFoundException) {
                Log.e(LOG_TAG, "Failed to open URI")
                setResult(RESULT_CANCELED)
            }
        } ?: setResult(RESULT_OK, Intent().apply {
            data = uri
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
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
        if (sharedPreferences.timerMode == TimerMode.OFF || !canRestartCamera()) {
            runnable()
            return
        }

        shutterButton.isEnabled = cameraMode == CameraMode.VIDEO

        countDownView.onPreviewAreaChanged(Rect().apply {
            viewFinder.getGlobalVisibleRect(this)
        })
        countDownView.startCountDown(sharedPreferences.timerMode.seconds) {
            shutterButton.isEnabled = true
            runnable()
        }
    }

    fun preventClicks(@Suppress("UNUSED_PARAMETER") view: View) {}

    companion object {
        private const val LOG_TAG = "Aperture"

        private const val MSG_HIDE_ZOOM_SLIDER = 0
        private const val MSG_HIDE_FOCUS_RING = 1
        private const val MSG_HIDE_EXPOSURE_SLIDER = 2
        private const val MSG_ON_PINCH_TO_ZOOM = 3

        private val EXPOSURE_LEVEL_FORMATTER = DecimalFormat("+#;-#")
    }
}
