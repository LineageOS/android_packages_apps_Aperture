/*
 * SPDX-FileCopyrightText: 2022-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.animation.ValueAnimator
import android.app.KeyguardManager
import android.content.ClipData
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.icu.text.DecimalFormat
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
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.ExperimentalZeroShutterLag
import androidx.camera.core.ImageCapture
import androidx.camera.core.MirrorMode
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.extensions.ExtensionMode
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.ScreenFlashView
import androidx.camera.viewfinder.core.ZoomGestureDetector
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.children
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.fallback
import coil3.size.Scale
import coil3.video.VideoFrameDecoder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lineageos.aperture.ext.camera2CameraControl
import org.lineageos.aperture.ext.flashMode
import org.lineageos.aperture.ext.mapToRange
import org.lineageos.aperture.ext.px
import org.lineageos.aperture.ext.scale
import org.lineageos.aperture.ext.setColorCorrectionAberrationMode
import org.lineageos.aperture.ext.setDistortionCorrectionMode
import org.lineageos.aperture.ext.setEdgeMode
import org.lineageos.aperture.ext.setFrameRate
import org.lineageos.aperture.ext.setHotPixelMode
import org.lineageos.aperture.ext.setNoiseReductionMode
import org.lineageos.aperture.ext.setPadding
import org.lineageos.aperture.ext.setShadingMode
import org.lineageos.aperture.ext.setVideoStabilizationMode
import org.lineageos.aperture.ext.slide
import org.lineageos.aperture.ext.slideDown
import org.lineageos.aperture.ext.smoothRotate
import org.lineageos.aperture.ext.transform
import org.lineageos.aperture.ext.updateBarsVisibility
import org.lineageos.aperture.models.AssistantIntent
import org.lineageos.aperture.models.CameraConfiguration
import org.lineageos.aperture.models.CameraFacing
import org.lineageos.aperture.models.CameraMode
import org.lineageos.aperture.models.CameraState
import org.lineageos.aperture.models.Event
import org.lineageos.aperture.models.FlashMode
import org.lineageos.aperture.models.GestureAction
import org.lineageos.aperture.models.GridMode
import org.lineageos.aperture.models.HardwareKey
import org.lineageos.aperture.models.MediaType
import org.lineageos.aperture.models.Permission
import org.lineageos.aperture.models.PermissionState
import org.lineageos.aperture.models.PhotoOutputFormat
import org.lineageos.aperture.models.Rotation
import org.lineageos.aperture.models.ThermalStatus
import org.lineageos.aperture.models.TimerMode
import org.lineageos.aperture.models.VideoDynamicRange
import org.lineageos.aperture.models.VideoMirrorMode
import org.lineageos.aperture.models.VideoStabilizationMode
import org.lineageos.aperture.ui.dialogs.LocationPermissionsDialog
import org.lineageos.aperture.ui.dialogs.QrBottomSheetDialog
import org.lineageos.aperture.ui.views.CameraModeSelectorLayout
import org.lineageos.aperture.ui.views.CapturePreviewLayout
import org.lineageos.aperture.ui.views.CountDownView
import org.lineageos.aperture.ui.views.GridView
import org.lineageos.aperture.ui.views.HorizontalSlider
import org.lineageos.aperture.ui.views.IslandView
import org.lineageos.aperture.ui.views.LensSelectorLayout
import org.lineageos.aperture.ui.views.LevelerView
import org.lineageos.aperture.ui.views.PreviewBlurView
import org.lineageos.aperture.ui.views.VerticalSlider
import org.lineageos.aperture.utils.ExifUtils
import org.lineageos.aperture.utils.GoogleLensUtils
import org.lineageos.aperture.utils.PermissionsManager
import org.lineageos.aperture.utils.ShortcutsUtils
import org.lineageos.aperture.viewmodels.CameraViewModel
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import kotlin.math.abs
import kotlin.reflect.safeCast
import androidx.camera.core.CameraState as CameraXCameraState

@androidx.annotation.OptIn(ExperimentalCamera2Interop::class, ExperimentalZeroShutterLag::class)
open class CameraActivity : AppCompatActivity(R.layout.activity_camera) {
    // View models
    private val viewModel by viewModels<CameraViewModel>()

    // Views
    private val aspectRatioButton by lazy { findViewById<Button>(R.id.aspectRatioButton) }
    private val cameraModeSelectorLayout by lazy { findViewById<CameraModeSelectorLayout>(R.id.cameraModeSelectorLayout) }
    private val capturePreviewLayout by lazy { findViewById<CapturePreviewLayout>(R.id.capturePreviewLayout) }
    private val countDownView by lazy { findViewById<CountDownView>(R.id.countDownView) }
    private val effectButton by lazy { findViewById<Button>(R.id.effectButton) }
    private val exposureLevel by lazy { findViewById<VerticalSlider>(R.id.exposureLevel) }
    private val flashButton by lazy { findViewById<ImageButton>(R.id.flashButton) }
    private val flipCameraButton by lazy { findViewById<ImageButton>(R.id.flipCameraButton) }
    private val galleryButtonCardView by lazy { findViewById<CardView>(R.id.galleryButtonCardView) }
    private val galleryButtonIconImageView by lazy { findViewById<ImageView>(R.id.galleryButtonIconImageView) }
    private val galleryButtonPreviewImageView by lazy { findViewById<ImageView>(R.id.galleryButtonPreviewImageView) }
    private val googleLensButton by lazy { findViewById<ImageButton>(R.id.googleLensButton) }
    private val gridButton by lazy { findViewById<Button>(R.id.gridButton) }
    private val gridView by lazy { findViewById<GridView>(R.id.gridView) }
    private val islandView by lazy { findViewById<IslandView>(R.id.islandView) }
    private val lensSelectorLayout by lazy { findViewById<LensSelectorLayout>(R.id.lensSelectorLayout) }
    private val levelerView by lazy { findViewById<LevelerView>(R.id.levelerView) }
    private val mainLayout by lazy { findViewById<ConstraintLayout>(R.id.mainLayout) }
    private val micButton by lazy { findViewById<Button>(R.id.micButton) }
    private val previewBlurView by lazy { findViewById<PreviewBlurView>(R.id.previewBlurView) }
    private val proButton by lazy { findViewById<ImageButton>(R.id.proButton) }
    private val screenFlashView by lazy { findViewById<ScreenFlashView>(R.id.screenFlashView) }
    private val secondaryBottomBarLayout by lazy { findViewById<ConstraintLayout>(R.id.secondaryBottomBarLayout) }
    private val secondaryTopBarLayout by lazy { findViewById<HorizontalScrollView>(R.id.secondaryTopBarLayout) }
    private val settingsButton by lazy { findViewById<Button>(R.id.settingsButton) }
    private val shutterButton by lazy { findViewById<ImageButton>(R.id.shutterButton) }
    private val timerButton by lazy { findViewById<Button>(R.id.timerButton) }
    private val videoFrameRateButton by lazy { findViewById<Button>(R.id.videoFrameRateButton) }
    private val videoQualityButton by lazy { findViewById<Button>(R.id.videoQualityButton) }
    private val videoRecordingStateButton by lazy { findViewById<ImageButton>(R.id.videoRecordingStateButton) }
    private val videoDynamicRangeButton by lazy { findViewById<Button>(R.id.videoDynamicRangeButton) }
    private val viewFinder by lazy { findViewById<PreviewView>(R.id.viewFinder) }
    private val viewFinderFocus by lazy { findViewById<ImageView>(R.id.viewFinderFocus) }
    private val zoomLevel by lazy { findViewById<HorizontalSlider>(R.id.zoomLevel) }

    // System services
    private val keyguardManager by lazy { getSystemService(KeyguardManager::class.java) }

    /**
     * The currently shown URI.
     */
    private var galleryButtonUri: Uri? = null

    /**
     * Medias captured from secure activity will be stored here
     * NOTE: Order is important, the first element is the newest one.
     */
    private val secureMediaUris = ArrayDeque<Uri>()

    // QR
    private val qrBottomSheetDialog by lazy { QrBottomSheetDialog(this) }
    private val isGoogleLensAvailable by lazy { GoogleLensUtils.isGoogleLensAvailable(this) }

    private var viewFinderTouchEvent: MotionEvent? = null
    private val gestureDetector by lazy {
        GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                viewFinderTouchEvent = e
                return false
            }

            override fun onFling(
                e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
            ): Boolean {
                return e1?.let {
                    if (!handler.hasMessages(MSG_ON_PINCH_TO_ZOOM) &&
                        abs(it.x - e2.x) > 75 * resources.displayMetrics.density
                    ) {
                        if (e2.x > it.x) {
                            // Left to right
                            viewModel.previousCameraMode()
                        } else {
                            // Right to left
                            viewModel.nextCameraMode()
                        }
                    }
                    true
                } ?: false
            }
        })
    }
    private val zoomGestureDetector by lazy {
        ZoomGestureDetector(this) {
            when (it) {
                is ZoomGestureDetector.ZoomEvent.Begin -> {
                    zoomGestureDetectorIsInProgress = true
                }

                is ZoomGestureDetector.ZoomEvent.Move -> {
                    viewModel.cameraController.onPinchToZoom(it.incrementalScaleFactor)
                    handler.removeMessages(MSG_ON_PINCH_TO_ZOOM)
                    handler.sendMessageDelayed(handler.obtainMessage(MSG_ON_PINCH_TO_ZOOM), 500)
                }

                is ZoomGestureDetector.ZoomEvent.End -> {
                    zoomGestureDetectorIsInProgress = false
                }
            }
            true
        }
    }
    private var zoomGestureDetectorIsInProgress = false

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                MSG_HIDE_ZOOM_SLIDER -> {
                    zoomLevel.isVisible = false
                }

                MSG_HIDE_FOCUS_RING -> {
                    viewFinderFocus.isVisible = false
                }

                MSG_HIDE_EXPOSURE_SLIDER -> {
                    exposureLevel.isVisible = false
                }
            }
        }
    }

    // Permissions
    private val permissionsManager = PermissionsManager(this)

    private val locationPermissionsDialog by lazy {
        LocationPermissionsDialog(this).also {
            it.onResultCallback = { result ->
                if (result) {
                    lifecycleScope.launch {
                        val permissionState = permissionsManager.requestPermission(
                            Permission.LOCATION
                        )
                        viewModel.setSaveLocation(permissionState == PermissionState.GRANTED)
                    }
                } else {
                    viewModel.setSaveLocation(false)
                }
            }
        }
    }

    private val forceTorchSnackbar by lazy {
        Snackbar.make(
            secondaryBottomBarLayout, R.string.force_torch_help, Snackbar.LENGTH_INDEFINITE
        )
            .setAnchorView(secondaryBottomBarLayout)
            .setAction(android.R.string.ok) {
                viewModel.setForceModeHelpShown(true)
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
            viewModel.initialCameraMode = CameraMode.PHOTO
            viewModel.inSingleCaptureMode.value = true
        },
        MediaStore.ACTION_IMAGE_CAPTURE_SECURE to {
            viewModel.initialCameraMode = CameraMode.PHOTO
            viewModel.inSingleCaptureMode.value = true
        },
        MediaStore.ACTION_VIDEO_CAPTURE to {
            viewModel.initialCameraMode = CameraMode.VIDEO
            viewModel.inSingleCaptureMode.value = true
        },
        MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA to {
            viewModel.initialCameraMode = CameraMode.PHOTO
        },
        MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE to {
            viewModel.initialCameraMode = CameraMode.PHOTO
        },
        MediaStore.INTENT_ACTION_VIDEO_CAMERA to {
            viewModel.initialCameraMode = CameraMode.VIDEO
        },

        // Shortcuts
        ShortcutsUtils.SHORTCUT_ID_SELFIE to {
            viewModel.initialCameraMode = CameraMode.PHOTO
            viewModel.initialCameraFacing = CameraFacing.FRONT
        },
        ShortcutsUtils.SHORTCUT_ID_VIDEO to {
            viewModel.initialCameraMode = CameraMode.VIDEO
            viewModel.initialCameraFacing = CameraFacing.BACK
        },
        ShortcutsUtils.SHORTCUT_ID_QR to {
            viewModel.initialCameraMode = CameraMode.QR
        },
    )
    private val assistantIntent
        get() = AssistantIntent.fromIntent(intent)
    private val launchedViaVoiceIntent
        get() = isVoiceInteractionRoot && intent.hasCategory(Intent.CATEGORY_VOICE)

    @Suppress("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge
        enableEdgeToEdge()

        // Hide the status bars
        window.updateBarsVisibility(
            WindowInsetsControllerCompat.BEHAVIOR_DEFAULT,
            statusBars = false,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
            && keyguardManager.isKeyguardLocked
        ) {
            setShowWhenLocked(true)

            @Suppress("SourceLockedOrientationActivity")
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        // Register shortcuts
        ShortcutsUtils.registerShortcuts(this)

        // Initialize initial camera mode
        overrideInitialCameraMode()?.let {
            viewModel.initialCameraMode = it
        }

        // Handle intent
        intent.action?.let {
            intentActions[it]?.invoke()
        }

        // Handle assistant intent
        assistantIntent?.useFrontCamera?.let {
            viewModel.initialCameraFacing = if (it) {
                CameraFacing.FRONT
            } else {
                CameraFacing.BACK
            }
        }

        if (viewModel.initialCameraMode == CameraMode.VIDEO && !viewModel.isVideoRecordingAvailable.value) {
            // If an app asked for a video we have to bail out
            if (viewModel.inSingleCaptureMode.value) {
                Toast.makeText(
                    this, getString(R.string.camcorder_unsupported_toast), Toast.LENGTH_LONG
                ).show()
                finish()
                return
            }
            // Fallback to photo mode
            viewModel.initialCameraMode = CameraMode.PHOTO
        }

        // Initialize the camera configuration
        if (!viewModel.initializeCameraConfiguration()) {
            noCamera()
            return
        }

        // Setup window insets
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            cameraModeSelectorLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom
                leftMargin = insets.left
                rightMargin = insets.right
            }

            capturePreviewLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom
                leftMargin = insets.left
                rightMargin = insets.right
            }

            windowInsets
        }

        // Set secondary top bar button callbacks
        aspectRatioButton.setOnClickListener { viewModel.cyclePhotoAspectRatio() }
        videoQualityButton.setOnClickListener { viewModel.cycleVideoQuality() }
        videoFrameRateButton.setOnClickListener { viewModel.cycleVideoFrameRate() }
        videoDynamicRangeButton.setOnClickListener { viewModel.cycleVideoDynamicRange() }
        effectButton.setOnClickListener { viewModel.cycleExtensionMode() }
        gridButton.setOnClickListener { viewModel.cycleGridMode() }
        timerButton.setOnClickListener { viewModel.toggleTimerMode() }
        micButton.setOnClickListener { viewModel.toggleVideoMicrophoneEnabled() }
        settingsButton.setOnClickListener { openSettings() }

        // Set secondary bottom bar button callbacks
        proButton.setOnClickListener {
            secondaryTopBarLayout.slide()
        }
        googleLensButton.setOnClickListener {
            dismissKeyguardAndRun {
                GoogleLensUtils.launchGoogleLens(this)
            }
        }
        flashButton.setOnClickListener { viewModel.cycleFlashMode(false) }
        flashButton.setOnLongClickListener { viewModel.cycleFlashMode(true) }

        // Attach CameraController to PreviewView
        viewFinder.controller = viewModel.cameraController

        // Attach CameraController to ScreenFlashView
        screenFlashView.setController(viewModel.cameraController)
        screenFlashView.setScreenFlashWindow(window)

        // Observe manual focus
        viewFinder.setOnTouchListener { _, event ->
            if (zoomGestureDetector.onTouchEvent(event) && zoomGestureDetectorIsInProgress) {
                return@setOnTouchListener true
            }
            return@setOnTouchListener gestureDetector.onTouchEvent(event)
        }
        viewFinder.setOnClickListener {
            // Reset exposure level to 0 EV
            viewModel.setExposureCompensationLevel(0.5f)

            exposureLevel.isVisible = true
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

        zoomLevel.onProgressChangedByUser = {
            viewModel.cameraController.setLinearZoom(it)
        }
        zoomLevel.textFormatter = {
            "%.1fx".format(viewModel.zoomState.value?.zoomRatio)
        }

        // Set expose level callback & text formatter
        exposureLevel.onProgressChangedByUser = {
            viewModel.setExposureCompensationLevel(it)

            handler.removeMessages(MSG_HIDE_EXPOSURE_SLIDER)
            handler.sendMessageDelayed(handler.obtainMessage(MSG_HIDE_EXPOSURE_SLIDER), 2000)
        }

        // Set primary bar button callbacks
        flipCameraButton.setOnClickListener { viewModel.flipCamera() }

        videoRecordingStateButton.setOnClickListener {
            viewModel.onVideoRecordingStateButtonPress()
        }

        shutterButton.setOnClickListener {
            // Shutter animation
            when (viewModel.cameraMode.value) {
                CameraMode.PHOTO -> startShutterAnimation(ShutterAnimation.PhotoCapture)
                CameraMode.VIDEO -> {
                    if (countDownView.cancelCountDown()) {
                        viewModel.cameraState.value = CameraState.IDLE
                        startShutterAnimation(ShutterAnimation.VideoEnd)
                        return@setOnClickListener
                    }
                    if (viewModel.cameraState.value == CameraState.IDLE) {
                        startShutterAnimation(ShutterAnimation.VideoStart)
                    }
                }

                else -> {}
            }

            startTimerAndRun {
                when (viewModel.cameraMode.value) {
                    CameraMode.PHOTO -> viewModel.takePhoto()
                    CameraMode.VIDEO -> viewModel.captureVideo()
                    else -> {}
                }
            }
        }

        galleryButtonCardView.setOnClickListener { openGallery() }

        // Set lens switching callback
        lensSelectorLayout.onCameraChangeCallback = {
            viewModel.selectCamera(it)
        }
        lensSelectorLayout.onZoomRatioChangeCallback = {
            viewModel.smoothZoom(it)
        }
        lensSelectorLayout.onResetZoomRatioCallback = {
            viewModel.resetZoom()
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

        // Set mode selector callback
        cameraModeSelectorLayout.onModeSelectedCallback = {
            viewModel.setCameraMode(it)
        }

        // Bind viewfinder and preview blur view
        previewBlurView.previewView = viewFinder

        // Observe QR dialog dismiss
        qrBottomSheetDialog.setOnDismissListener {
            viewModel.onQrDialogDismissed()
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                permissionsManager.withPermissionGranted(Permission.CAMERA) {
                    loadData()
                }
            }
        }

        // Also collect location
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                permissionsManager.withPermissionGranted(Permission.LOCATION) {
                    viewModel.location.collect()
                }
            }
        }

        // Check for permissions
        lifecycleScope.launch {
            permissionsManager.permissionStateFlow(
                Permission.CAMERA
            ).collectLatest { permissionState ->
                if (permissionState != PermissionState.GRANTED) {
                    when (permissionsManager.requestPermission(Permission.CAMERA)) {
                        PermissionState.GRANTED -> {
                            // This is a good time to ask the user for location permissions
                            if (viewModel.saveLocation.value == null) {
                                locationPermissionsDialog.show()
                            }
                        }

                        else -> {
                            Toast.makeText(
                                this@CameraActivity,
                                R.string.app_permissions_toast,
                                Toast.LENGTH_LONG,
                            ).show()
                            finish()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        // Detach CameraController from ScreenFlashView
        screenFlashView.setController(null)

        // Detach CameraController from PreviewView
        viewFinder.controller = null

        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?) = when (capturePreviewLayout.isVisible) {
        true -> super.onKeyDown(keyCode, event)
        false -> handleHardwareKeyDown(keyCode, event) ?: super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?) = when (capturePreviewLayout.isVisible) {
        true -> super.onKeyUp(keyCode, event)
        false -> handleHardwareKeyUp(keyCode, event) ?: super.onKeyUp(keyCode, event)
    }

    /**
     * This is a method that can be overridden to set the initial camera mode and facing.
     * It's gonna have priority over shared preferences and intents.
     */
    protected open fun overrideInitialCameraMode(): CameraMode? = null

    private fun CoroutineScope.loadData() {
        launch {
            viewModel.event.collect { event ->
                when (event) {
                    is Event.NoCamera -> noCamera()

                    is Event.ShowForceTorchHelp -> if (!forceTorchSnackbar.isShownOrQueued) {
                        forceTorchSnackbar.show()
                    }

                    is Event.FlipCameraAnimation ->
                        (flipCameraButton.drawable as AnimatedVectorDrawable).start()

                    is Event.PhotoCaptureStatus -> when (event) {
                        is Event.PhotoCaptureStatus.CaptureStarted -> {
                            viewFinder.foreground = ColorDrawable(Color.BLACK)
                            ValueAnimator.ofInt(0, 255, 0).apply {
                                addUpdateListener { anim ->
                                    viewFinder.foreground.alpha = anim.animatedValue as Int
                                }
                            }.start()
                        }

                        is Event.PhotoCaptureStatus.ImageSaved -> {
                            if (!viewModel.inSingleCaptureMode.value) {
                                onCapturedMedia(event.output.savedUri)
                            } else {
                                event.output.savedUri?.let {
                                    openCapturePreview(it, MediaType.PHOTO)
                                }
                                event.photoOutputStream?.use {
                                    openCapturePreview(
                                        ByteArrayInputStream(
                                            event.photoOutputStream.toByteArray()
                                        )
                                    )
                                }
                            }
                        }

                        is Event.PhotoCaptureStatus.Error -> {
                            // Do nothing
                        }
                    }
                }
            }
        }

        launch {
            viewModel.cameraConfiguration.collectLatest { cameraConfiguration ->
                bindCameraUseCases(cameraConfiguration)
            }
        }

        launch {
            viewModel.cameraMode.collectLatest { cameraMode ->
                // Hide secondary top bar
                secondaryTopBarLayout.isVisible = false

                // Update secondary top bar buttons
                aspectRatioButton.isVisible = cameraMode != CameraMode.VIDEO
                videoQualityButton.isVisible = cameraMode == CameraMode.VIDEO
                videoFrameRateButton.isVisible = cameraMode == CameraMode.VIDEO
                videoDynamicRangeButton.isVisible = cameraMode == CameraMode.VIDEO
                micButton.isVisible = cameraMode == CameraMode.VIDEO

                // Update secondary bottom bar buttons
                proButton.isVisible = cameraMode != CameraMode.QR
                googleLensButton.isVisible = cameraMode == CameraMode.QR && isGoogleLensAvailable

                // Update primary bar buttons
                shutterButton.isInvisible = cameraMode == CameraMode.QR

                // Update camera mode selector
                cameraModeSelectorLayout.setCurrentCameraMode(cameraMode)
            }
        }

        launch {
            viewModel.cameraModeTransition.collectLatest { (oldCameraMode, newCameraMode) ->
                when (oldCameraMode) {
                    CameraMode.PHOTO -> when (newCameraMode) {
                        CameraMode.VIDEO -> startShutterAnimation(ShutterAnimation.PhotoToVideo)
                        else -> startShutterAnimation(ShutterAnimation.InitPhoto)
                    }

                    CameraMode.VIDEO -> when (newCameraMode) {
                        CameraMode.PHOTO -> startShutterAnimation(ShutterAnimation.VideoToPhoto)
                        else -> startShutterAnimation(ShutterAnimation.InitVideo)
                    }

                    CameraMode.QR, null -> when (newCameraMode) {
                        CameraMode.PHOTO -> startShutterAnimation(ShutterAnimation.InitPhoto)

                        CameraMode.VIDEO -> startShutterAnimation(ShutterAnimation.InitVideo)

                        CameraMode.QR -> {}
                    }
                }
            }
        }

        launch {
            viewModel.lenses.collectLatest { (activeCamera, availableCameras) ->
                // Update lens selector
                activeCamera?.let {
                    lensSelectorLayout.setCamera(
                        it,
                        availableCameras,
                    )
                }
            }
        }

        launch {
            viewModel.cameraXCameraState.collect { cameraXCameraState ->
                cameraXCameraState.error?.let {
                    // Log the error
                    Log.e(
                        LOG_TAG,
                        "Error: code: ${it.code}, type: ${it.type}",
                        it.cause
                    )

                    val showToast = { stringId: Int ->
                        Toast.makeText(
                            this@CameraActivity,
                            stringId,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    when (it.code) {
                        CameraXCameraState.ERROR_MAX_CAMERAS_IN_USE -> {
                            // No way to fix it without user action, bail out
                            showToast(R.string.error_max_cameras_in_use)
                            finish()
                        }

                        CameraXCameraState.ERROR_CAMERA_IN_USE -> {
                            // No way to fix it without user action, bail out
                            showToast(R.string.error_camera_in_use)
                            finish()
                        }

                        CameraXCameraState.ERROR_OTHER_RECOVERABLE_ERROR -> {
                            // Warn the user and don't do anything
                            showToast(R.string.error_other_recoverable_error)
                        }

                        CameraXCameraState.ERROR_STREAM_CONFIG -> {
                            // CameraX use case misconfiguration, no way to recover
                            showToast(R.string.error_stream_config)
                            finish()
                        }

                        CameraXCameraState.ERROR_CAMERA_DISABLED -> {
                            // No way to fix it without user action, bail out
                            showToast(R.string.error_camera_disabled)
                            finish()
                        }

                        CameraXCameraState.ERROR_CAMERA_FATAL_ERROR -> {
                            // No way to fix it without user action, bail out
                            showToast(R.string.error_camera_fatal_error)
                            finish()
                        }

                        CameraXCameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED -> {
                            // No way to fix it without user action, bail out
                            showToast(R.string.error_do_not_disturb_mode_enabled)
                            finish()
                        }

                        else -> {
                            // We know anything about it, just check if it's recoverable or
                            // critical
                            when (it.type) {
                                CameraXCameraState.ErrorType.RECOVERABLE -> {
                                    showToast(R.string.error_unknown_recoverable)
                                }

                                CameraXCameraState.ErrorType.CRITICAL -> {
                                    showToast(R.string.error_unknown_critical)
                                    finish()
                                }

                                null -> {
                                    // Do nothing
                                }
                            }
                        }
                    }
                }
            }
        }

        launch {
            viewModel.inSingleCaptureMode.collectLatest { inSingleCaptureMode ->
                // Update primary bar buttons
                galleryButtonCardView.isInvisible = inSingleCaptureMode

                // Update camera mode selector
                cameraModeSelectorLayout.setInSingleCaptureMode(inSingleCaptureMode)
            }
        }

        launch {
            viewModel.cameraState.collectLatest { cameraState ->
                // Update secondary bar buttons
                timerButton.isEnabled = cameraState == CameraState.IDLE
                aspectRatioButton.isEnabled = cameraState == CameraState.IDLE
                effectButton.isEnabled = cameraState == CameraState.IDLE
                settingsButton.isEnabled = cameraState == CameraState.IDLE

                lensSelectorLayout.setCameraState(cameraState)

                // Update primary bar buttons
                galleryButtonCardView.isEnabled = cameraState == CameraState.IDLE
                // Shutter button must stay enabled
                flipCameraButton.isEnabled = cameraState == CameraState.IDLE
                videoRecordingStateButton.isVisible = cameraState.isRecordingVideo

                // Update camera mode selector
                cameraModeSelectorLayout.setCameraState(cameraState)
            }
        }

        launch {
            viewModel.screenRotation.collectLatest { screenRotation ->
                val compensationValue = screenRotation.compensationValue.toFloat()

                // Rotate sliders
                exposureLevel.screenRotation = screenRotation
                zoomLevel.screenRotation = screenRotation

                // Rotate info chip
                islandView.setScreenRotation(screenRotation)

                // Rotate secondary top bar buttons
                ConstraintLayout::class.safeCast(
                    secondaryTopBarLayout.getChildAt(0)
                )?.let { layout ->
                    for (child in layout.children) {
                        Button::class.safeCast(child)?.let {
                            it.smoothRotate(compensationValue)
                            ValueAnimator.ofFloat(
                                (it.layoutParams as ConstraintLayout.LayoutParams).verticalBias,
                                when (screenRotation) {
                                    Rotation.ROTATION_0 -> 0.0f
                                    Rotation.ROTATION_180 -> 1.0f
                                    Rotation.ROTATION_90,
                                    Rotation.ROTATION_270 -> 0.5f
                                }
                            ).apply {
                                addUpdateListener { anim ->
                                    it.updateLayoutParams<ConstraintLayout.LayoutParams> {
                                        verticalBias = anim.animatedValue as Float
                                    }
                                }
                            }.start()
                        }
                    }
                }

                // Rotate secondary bottom bar buttons
                proButton.smoothRotate(compensationValue)
                lensSelectorLayout.setScreenRotation(screenRotation)
                flashButton.smoothRotate(compensationValue)

                // Rotate primary bar buttons
                galleryButtonCardView.smoothRotate(compensationValue)
                shutterButton.smoothRotate(compensationValue)
                flipCameraButton.smoothRotate(compensationValue)

                // Rotate capture preview
                capturePreviewLayout.setScreenRotation(screenRotation)

                // Rotate count down view
                countDownView.setScreenRotation(screenRotation)
            }
        }

        launch {
            viewModel.capturedMedia.collectLatest { capturedMedia ->
                updateGalleryButton(capturedMedia.firstOrNull(), false)
            }
        }

        launch {
            viewModel.supportedFlashModes.collectLatest { supportedFlashModes ->
                // Hide the button if the only available mode is off,
                // we want the user to know if any other mode is being used
                flashButton.isVisible = supportedFlashModes.size != 1
                        || supportedFlashModes.first() != FlashMode.OFF
            }
        }

        launch {
            viewModel.flashMode.collectLatest { flashMode ->
                // Update secondary bar buttons
                flashButton.setImageResource(
                    when (flashMode) {
                        FlashMode.OFF -> R.drawable.ic_flash_off
                        FlashMode.AUTO -> R.drawable.ic_flash_auto
                        FlashMode.ON -> R.drawable.ic_flash_on
                        FlashMode.TORCH -> R.drawable.ic_flashlight_on
                        FlashMode.SCREEN -> R.drawable.ic_flash_screen
                    }
                )
            }
        }

        launch {
            viewModel.isFlashButtonEnabled.collectLatest { isFlashButtonEnabled ->
                flashButton.isEnabled = isFlashButtonEnabled
            }
        }

        launch {
            viewModel.gridMode.collectLatest { gridMode ->
                gridView.mode = gridMode

                // Update secondary bar buttons
                gridButton.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    when (gridMode) {
                        GridMode.OFF -> R.drawable.ic_grid_3x3_off
                        GridMode.ON_3 -> R.drawable.ic_grid_3x3
                        GridMode.ON_4 -> R.drawable.ic_grid_4x4
                        GridMode.ON_GOLDEN_RATIO -> R.drawable.ic_grid_goldenratio
                    },
                    0,
                    0
                )
                gridButton.setText(
                    when (gridMode) {
                        GridMode.OFF -> R.string.grid_off
                        GridMode.ON_3 -> R.string.grid_on_3
                        GridMode.ON_4 -> R.string.grid_on_4
                        GridMode.ON_GOLDEN_RATIO -> R.string.grid_on_goldenratio
                    }
                )
            }
        }

        launch {
            viewModel.timerMode.collectLatest { timerMode ->
                // Update secondary bar buttons
                timerButton.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    when (timerMode) {
                        TimerMode.OFF -> R.drawable.ic_timer_off
                        TimerMode.ON_3S -> R.drawable.ic_timer_3_alt_1
                        TimerMode.ON_10S -> R.drawable.ic_timer_10_alt_1
                    },
                    0,
                    0
                )
                timerButton.setText(
                    when (timerMode) {
                        TimerMode.OFF -> R.string.timer_off
                        TimerMode.ON_3S -> R.string.timer_3
                        TimerMode.ON_10S -> R.string.timer_10
                    }
                )
            }
        }

        launch {
            viewModel.levelerEnabled.collectLatest { levelerEnabled ->
                levelerView.isVisible = levelerEnabled
            }
        }

        launch {
            viewModel.fullScreenBrightness.collectLatest { fullScreenBrightness ->
                window.attributes = window.attributes.apply {
                    screenBrightness = when (fullScreenBrightness) {
                        true -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                        false -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    }
                }
            }
        }

        launch {
            viewModel.thermalStatus.collectLatest { thermalStatus ->
                val showSnackBar = { stringId: Int ->
                    Snackbar.make(
                        secondaryBottomBarLayout,
                        stringId,
                        Snackbar.LENGTH_INDEFINITE
                    )
                        .setAnchorView(secondaryBottomBarLayout)
                        .setAction(android.R.string.ok) {
                            // Do nothing
                        }
                        .show()
                }

                when (thermalStatus) {
                    ThermalStatus.NONE,
                    ThermalStatus.LIGHT -> Unit

                    ThermalStatus.MODERATE -> {
                        showSnackBar(R.string.thermal_status_moderate)
                    }

                    ThermalStatus.SEVERE -> {
                        showSnackBar(R.string.thermal_status_severe)
                    }

                    ThermalStatus.CRITICAL -> {
                        showSnackBar(R.string.thermal_status_critical)
                    }

                    ThermalStatus.EMERGENCY -> {
                        showSnackBar(R.string.thermal_status_emergency)
                        finish()
                    }

                    ThermalStatus.SHUTDOWN -> {
                        showSnackBar(R.string.thermal_status_shutdown)
                        finish()
                    }
                }
            }
        }

        launch {
            viewModel.zoomState.collectLatest { zoomState ->
                zoomState?.takeIf { it.minZoomRatio != it.maxZoomRatio }?.let {
                    zoomLevel.progress = it.linearZoom
                    zoomLevel.isVisible = true

                    handler.removeMessages(MSG_HIDE_ZOOM_SLIDER)
                    handler.sendMessageDelayed(handler.obtainMessage(MSG_HIDE_ZOOM_SLIDER), 2000)

                    lensSelectorLayout.onZoomRatioChanged(it.zoomRatio)
                }
            }
        }

        launch {
            viewModel.tapToFocusInfoState.collectLatest { tapToFocusInfoState ->
                when (tapToFocusInfoState.focusState) {
                    CameraController.TAP_TO_FOCUS_STARTED -> {
                        viewFinderFocus.x =
                            tapToFocusInfoState.tapPoint!!.x - (viewFinderFocus.width / 2)
                        viewFinderFocus.y =
                            tapToFocusInfoState.tapPoint!!.y - (viewFinderFocus.height / 2)
                        viewFinderFocus.isVisible = true
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
        }

        launch {
            viewModel.exposureCompensationRangeToLevel.collectLatest { (range, level) ->
                exposureLevel.steps = range.endInclusive - range.start
                exposureLevel.progress = level
                exposureLevel.textFormatter = {
                    val ev = Int.mapToRange(range, it)
                    when (ev == 0) {
                        true -> "0"
                        false -> EXPOSURE_LEVEL_FORMATTER.format(ev).toString()
                    }
                }
            }
        }

        launch {
            viewModel.isShutterButtonEnabled.collectLatest { isShutterButtonEnabled ->
                shutterButton.isEnabled = isShutterButtonEnabled
            }
        }

        launch {
            viewModel.photoAspectRatio.collectLatest { photoAspectRatio ->
                // Update secondary bar buttons
                aspectRatioButton.setText(
                    when (photoAspectRatio) {
                        AspectRatio.RATIO_4_3 -> R.string.aspect_ratio_4_3
                        AspectRatio.RATIO_16_9 -> R.string.aspect_ratio_16_9
                        else -> throw Exception("Unknown aspect ratio $photoAspectRatio")
                    }
                )
            }
        }

        launch {
            viewModel.photoEffect.collectLatest { photoEffect ->
                // Update secondary bar buttons
                effectButton.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    when (photoEffect) {
                        ExtensionMode.NONE -> R.drawable.ic_blur_off
                        ExtensionMode.BOKEH -> R.drawable.ic_effect_bokeh
                        ExtensionMode.HDR -> R.drawable.ic_hdr_on
                        ExtensionMode.NIGHT -> R.drawable.ic_clear_night
                        ExtensionMode.FACE_RETOUCH -> R.drawable.ic_face_retouching_natural
                        ExtensionMode.AUTO -> R.drawable.ic_hdr_auto
                        else -> R.drawable.ic_blur_off
                    },
                    0,
                    0
                )
                effectButton.setText(
                    when (photoEffect) {
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

        launch {
            viewModel.isPhotoEffectButtonVisible.collectLatest { isPhotoEffectButtonVisible ->
                effectButton.isVisible = isPhotoEffectButtonVisible
            }
        }

        launch {
            viewModel.videoQuality.collectLatest { videoQuality ->
                // Update secondary bar buttons
                videoQualityButton.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    when (videoQuality) {
                        Quality.SD -> R.drawable.ic_sd
                        Quality.HD -> R.drawable.ic_hd
                        Quality.FHD -> R.drawable.ic_full_hd
                        Quality.UHD -> R.drawable.ic_4k
                        else -> throw Exception("Unknown video quality $videoQuality")
                    },
                    0,
                    0
                )
                videoQualityButton.setText(
                    when (videoQuality) {
                        Quality.SD -> R.string.video_quality_sd
                        Quality.HD -> R.string.video_quality_hd
                        Quality.FHD -> R.string.video_quality_fhd
                        Quality.UHD -> R.string.video_quality_uhd
                        else -> throw Exception("Unknown video quality $videoQuality")
                    }
                )
            }
        }

        launch {
            viewModel.isVideoQualityButtonEnabled.collectLatest { isVideoQualityButtonEnabled ->
                videoQualityButton.isEnabled = isVideoQualityButtonEnabled
            }
        }

        launch {
            viewModel.videoFrameRate.collectLatest { videoFrameRate ->
                // Update secondary bar buttons
                videoFrameRateButton.text = videoFrameRate?.let {
                    resources.getString(R.string.video_framerate_value, it.value)
                } ?: resources.getString(R.string.video_framerate_auto)
            }
        }

        launch {
            viewModel.isVideoFrameRateButtonEnabled.collectLatest { isVideoFrameRateButtonEnabled ->
                videoFrameRateButton.isEnabled = isVideoFrameRateButtonEnabled
            }
        }

        launch {
            viewModel.videoDynamicRange.collectLatest { videoDynamicRange ->
                // Update secondary bar buttons
                videoDynamicRangeButton.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    when (videoDynamicRange) {
                        VideoDynamicRange.SDR -> R.drawable.ic_hdr_off
                        VideoDynamicRange.HLG_10_BIT -> R.drawable.ic_hdr_on
                        VideoDynamicRange.HDR10_10_BIT -> R.drawable.ic_hdr_on
                        VideoDynamicRange.HDR10_PLUS_10_BIT -> R.drawable.ic_hdr_on
                        VideoDynamicRange.DOLBY_VISION_10_BIT -> R.drawable.ic_dolby
                        VideoDynamicRange.DOLBY_VISION_8_BIT -> R.drawable.ic_dolby
                    },
                    0,
                    0,
                )
                videoDynamicRangeButton.setText(
                    when (videoDynamicRange) {
                        VideoDynamicRange.SDR -> R.string.video_dynamic_range_sdr
                        VideoDynamicRange.HLG_10_BIT -> R.string.video_dynamic_range_hlg_10_bit
                        VideoDynamicRange.HDR10_10_BIT -> R.string.video_dynamic_range_hdr10_10_bit
                        VideoDynamicRange.HDR10_PLUS_10_BIT -> R.string.video_dynamic_range_hdr10_plus_10_bit
                        VideoDynamicRange.DOLBY_VISION_10_BIT -> R.string.video_dynamic_range_dolby_vision_10_bit
                        VideoDynamicRange.DOLBY_VISION_8_BIT -> R.string.video_dynamic_range_dolby_vision_8_bit
                    }
                )
            }
        }

        launch {
            viewModel.isVideoDynamicRangeButtonEnabled.collectLatest { isVideoDynamicRangeButtonEnabled ->
                videoDynamicRangeButton.isEnabled = isVideoDynamicRangeButtonEnabled
            }
        }

        launch {
            viewModel.videoMicMode.collectLatest { videoMicMode ->
                micButton.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    when (videoMicMode) {
                        true -> R.drawable.ic_mic_on
                        false -> R.drawable.ic_mic_off
                    },
                    0,
                    0
                )
                micButton.setText(
                    when (videoMicMode) {
                        true -> R.string.mic_on
                        false -> R.string.mic_off
                    }
                )
            }
        }

        launch {
            viewModel.isVideoMicButtonEnabled.collectLatest { isVideoMicButtonEnabled ->
                micButton.isEnabled = isVideoMicButtonEnabled
            }
        }

        launch {
            viewModel.videoRecordingDuration.collectLatest { videoRecordingDuration ->
                cameraModeSelectorLayout.setVideoRecordingDuration(videoRecordingDuration)
            }
        }

        launch {
            viewModel.videoRecordEvent.collect { videoRecordEvent ->
                when (videoRecordEvent) {
                    is VideoRecordEvent.Start -> runOnUiThread {
                        startVideoRecordingStateAnimation(
                            VideoRecordingStateAnimation.Init
                        )
                    }

                    is VideoRecordEvent.Pause -> runOnUiThread {
                        startVideoRecordingStateAnimation(
                            VideoRecordingStateAnimation.ResumeToPause
                        )
                    }

                    is VideoRecordEvent.Resume -> runOnUiThread {
                        startVideoRecordingStateAnimation(
                            VideoRecordingStateAnimation.PauseToResume
                        )
                    }

                    is VideoRecordEvent.Status -> {
                        // Do nothing
                    }

                    is VideoRecordEvent.Finalize -> {
                        runOnUiThread {
                            startShutterAnimation(ShutterAnimation.VideoEnd)
                        }
                        if (videoRecordEvent.error != VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA) {
                            if (!viewModel.inSingleCaptureMode.value) {
                                onCapturedMedia(videoRecordEvent.outputResults.outputUri)
                            } else {
                                openCapturePreview(
                                    videoRecordEvent.outputResults.outputUri,
                                    MediaType.VIDEO,
                                )
                            }
                        }
                    }
                }
            }
        }

        launch {
            viewModel.qrResult.collectLatest { qrResult ->
                qrBottomSheetDialog.setQrResult(qrResult)
            }
        }

        launch {
            viewModel.canFlipCamera.collectLatest { canFlipCamera ->
                flipCameraButton.isInvisible = !canFlipCamera
            }
        }

        launch {
            viewModel.islandItems.collectLatest { islandItems ->
                islandView.setItems(islandItems)
            }
        }
    }

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

    /**
     * Rebind cameraProvider use cases
     */
    private fun bindCameraUseCases(cameraConfiguration: CameraConfiguration) {
        require(viewModel.cameraState.value == CameraState.IDLE) {
            "Trying to rebind camera while camera is not in IDLE state"
        }

        // Show blurred preview
        previewBlurView.freeze()
        previewBlurView.isVisible = true

        // Unbind previous use cases
        viewModel.cameraController.unbind()

        // Hide grid until preview is ready
        gridView.alpha = 0f

        // If the current camera doesn't support the selected camera mode, give up
        require(cameraConfiguration.camera.supportsCameraMode(cameraConfiguration.cameraMode)) {
            "Selected camera mode (${
                cameraConfiguration.cameraMode
            }) not supported by camera ${cameraConfiguration.camera.cameraId}"
        }

        // Make sure the extension mode is supported
        require(
            cameraConfiguration.camera.supportsExtensionMode(
                cameraConfiguration.extensionMode
            )
        ) {
            "Selected extension mode (${
                cameraConfiguration.extensionMode
            }) not supported by camera ${cameraConfiguration.camera.cameraId}"
        }

        // Initialize the use case we want and set its properties
        val cameraUseCases = when (cameraConfiguration) {
            is CameraConfiguration.Photo -> {
                require(
                    cameraConfiguration.photoCaptureMode != ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG
                            || cameraConfiguration.camera.supportsZsl
                ) {
                    "Requested ZSL capture mode when the camera doesn't support it"
                }

                require(
                    cameraConfiguration.camera.supportedPhotoOutputFormats.contains(
                        cameraConfiguration.photoOutputFormat
                    )
                ) {
                    "Selected photo output format (${
                        cameraConfiguration.photoOutputFormat
                    } not supported by camera ${cameraConfiguration.camera.cameraId})"
                }

                viewModel.cameraController.imageCaptureMode = cameraConfiguration.photoCaptureMode

                viewModel.cameraController.imageOutputFormat = when (
                    cameraConfiguration.photoOutputFormat
                ) {
                    PhotoOutputFormat.JPEG -> ImageCapture.OUTPUT_FORMAT_JPEG
                    PhotoOutputFormat.JPEG_ULTRA_HDR -> ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR
                    PhotoOutputFormat.RAW -> ImageCapture.OUTPUT_FORMAT_RAW
                    PhotoOutputFormat.RAW_JPEG -> ImageCapture.OUTPUT_FORMAT_RAW_JPEG
                }

                viewModel.cameraController.imageCaptureResolutionSelector =
                    ResolutionSelector.Builder()
                        .setAspectRatioStrategy(
                            AspectRatioStrategy(
                                cameraConfiguration.photoAspectRatio,
                                AspectRatioStrategy.FALLBACK_RULE_AUTO,
                            )
                        )
                        .setAllowedResolutionMode(
                            if (cameraConfiguration.enableHighResolution) {
                                ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE
                            } else {
                                ResolutionSelector.PREFER_CAPTURE_RATE_OVER_HIGHER_RESOLUTION
                            }
                        )
                        .build()

                CameraController.IMAGE_CAPTURE
            }

            is CameraConfiguration.Video -> {
                // Check whether or not the video quality is supported
                val videoQualityInfo = cameraConfiguration.camera.supportedVideoQualities[
                    cameraConfiguration.videoQuality
                ] ?: error("Video quality not supported by the camera")

                require(
                    videoQualityInfo.supportedFrameRates.contains(
                        cameraConfiguration.videoFrameRate
                    )
                ) {
                    "Video frame rate not supported with the requested video quality"
                }

                require(
                    videoQualityInfo.supportedDynamicRanges.contains(
                        cameraConfiguration.videoDynamicRange
                    )
                ) {
                    "Video dynamic range not supported with the requested video quality"
                }

                // Set the quality
                viewModel.cameraController.videoCaptureQualitySelector =
                    QualitySelector.from(cameraConfiguration.videoQuality)

                // Set the dynamic range
                viewModel.cameraController.videoCaptureDynamicRange =
                    cameraConfiguration.videoDynamicRange.dynamicRange

                // Set video mirror mode
                viewModel.cameraController.videoCaptureMirrorMode =
                    when (cameraConfiguration.videoMirrorMode) {
                        VideoMirrorMode.OFF -> MirrorMode.MIRROR_MODE_OFF
                        VideoMirrorMode.ON -> MirrorMode.MIRROR_MODE_ON
                        VideoMirrorMode.ON_FFC_ONLY -> when (
                            cameraConfiguration.camera.cameraFacing
                        ) {
                            CameraFacing.FRONT -> MirrorMode.MIRROR_MODE_ON
                            else -> MirrorMode.MIRROR_MODE_OFF
                        }
                    }

                CameraController.VIDEO_CAPTURE
            }

            is CameraConfiguration.Qr -> {
                viewModel.cameraController.setImageAnalysisAnalyzer(
                    viewModel.cameraExecutor, viewModel.qrImageAnalyzer
                )

                CameraController.IMAGE_ANALYSIS
            }
        }

        // Get the camera selector
        val cameraSelector = viewModel.getExtensionEnabledCameraSelector(
            cameraConfiguration.camera, cameraConfiguration.extensionMode
        )

        // Workaround: We cannot set flash mode to screen with a non front facing camera.
        // VM will set the correct value later on
        if (cameraConfiguration.camera.cameraFacing != CameraFacing.FRONT
            && viewModel.cameraController.flashMode == FlashMode.SCREEN
        ) {
            viewModel.cameraController.flashMode = FlashMode.OFF
        }

        // Bind use cases to camera
        viewModel.cameraController.cameraSelector = cameraSelector
        viewModel.cameraController.setEnabledUseCases(cameraUseCases)

        // Bind camera controller to lifecycle
        viewModel.cameraController.bindToLifecycle(this)

        // Wait for camera to be ready
        lifecycleScope.launch {
            viewModel.cameraController.initializationFuture.await()

            val camera2CameraControl = viewModel.cameraController.camera2CameraControl ?: run {
                Log.wtf(LOG_TAG, "Camera2CameraControl not available even with camera ready?")
                return@launch
            }

            val camera2Options = cameraConfiguration.camera2Options

            // Set Camera2 CaptureRequest options
            camera2CameraControl.captureRequestOptions = CaptureRequestOptions.Builder()
                .setFrameRate(
                    when (cameraConfiguration) {
                        is CameraConfiguration.Video -> cameraConfiguration.videoFrameRate
                        else -> null
                    }
                )
                .setVideoStabilizationMode(
                    when (cameraConfiguration) {
                        is CameraConfiguration.Video -> when (
                            cameraConfiguration.enableVideoStabilization
                        ) {
                            true -> VideoStabilizationMode.getMode(cameraConfiguration.camera)
                            false -> null
                        }

                        else -> null
                    } ?: VideoStabilizationMode.OFF
                )
                .setEdgeMode(camera2Options.edgeMode)
                .setNoiseReductionMode(camera2Options.noiseReductionMode)
                .setShadingMode(camera2Options.shadingMode)
                .setColorCorrectionAberrationMode(camera2Options.colorCorrectionAberrationMode)
                .setDistortionCorrectionMode(camera2Options.distortionCorrectionMode)
                .setHotPixelMode(camera2Options.hotPixelMode)
                .build()
        }

        // Restore settings that can be set on the fly
        viewModel.setVideoMicrophoneEnabled(viewModel.videoMicMode.value)

        // Reset exposure level
        viewModel.setExposureCompensationLevel(0.5f)
    }

    private fun updateGalleryButton(uri: Uri?, fromCapture: Boolean) {
        runOnUiThread {
            val keyguardLocked = keyguardManager.isKeyguardLocked

            galleryButtonIconImageView.setImageResource(
                when (keyguardLocked) {
                    true -> R.drawable.ic_lock
                    false -> R.drawable.ic_image
                }
            )

            if (keyguardLocked != fromCapture) {
                return@runOnUiThread
            }

            galleryButtonUri = uri

            // When keyguard is unlocked, we want media from MediaStore, else we only trust the
            // ones coming from the capture
            uri?.also {
                galleryButtonPreviewImageView.load(uri) {
                    decoderFactory(VideoFrameDecoder.Factory())
                    crossfade(true)
                    scale(Scale.FILL)
                    size(75.px)
                    error(R.drawable.ic_image)
                    fallback(R.drawable.ic_image)
                    listener(
                        onCancel = {
                            galleryButtonPreviewImageView.isVisible = false
                            galleryButtonIconImageView.isVisible = true
                        },
                        onError = { _, _ ->
                            galleryButtonPreviewImageView.isVisible = false
                            galleryButtonIconImageView.isVisible = true
                        },
                        onSuccess = { _, _ ->
                            galleryButtonPreviewImageView.isVisible = true
                            galleryButtonIconImageView.isVisible = false
                        }
                    )
                }
            } ?: run {
                galleryButtonIconImageView.isVisible = true
                galleryButtonPreviewImageView.isVisible = false
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
        lifecycleScope.launch {
            // secureMediaUris will be cleared if keyguard is unlocked
            // or none of the items were found.
            withContext(Dispatchers.IO) {
                updateSecureMediaUris(keyguardManager.isKeyguardLocked)
            }

            if (keyguardManager.isKeyguardLocked) {
                // In this state the only thing we can do is launch a secure review intent
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    && secureMediaUris.isNotEmpty()
                ) {
                    val intent = Intent(
                        MediaStore.ACTION_REVIEW_SECURE, secureMediaUris.first()
                    ).apply {
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        if (secureMediaUris.size > 1) {
                            clipData = ClipData.newUri(
                                contentResolver, null, secureMediaUris[1]
                            ).apply {
                                for (i in 2 until secureMediaUris.size) {
                                    addItem(contentResolver, ClipData.Item(secureMediaUris[i]))
                                }
                            }
                        }
                    }
                    runCatching {
                        startActivity(intent)
                        return@launch
                    }
                }
            }

            galleryButtonUri?.also { uri ->
                // Try to open the Uri in the non secure gallery
                dismissKeyguardAndRun {
                    mutableListOf<String>().apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            add(MediaStore.ACTION_REVIEW)
                        }
                        add(Intent.ACTION_VIEW)
                    }.forEach {
                        val intent = Intent(it, uri).apply {
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        runCatching {
                            startActivity(intent)
                            return@dismissKeyguardAndRun
                        }
                    }
                }
            } ?: run {
                // If the Uri is null, attempt to launch non secure-gallery
                dismissKeyguardAndRun {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        type = "image/*"
                    }
                    runCatching {
                        startActivity(intent)
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

    private fun startTimerAndRun(runnable: () -> Unit) {
        // Allow forcing timer if requested by the assistant
        val timerModeSeconds =
            assistantIntent?.timerDurationSeconds ?: viewModel.timerMode.value.seconds

        if (timerModeSeconds <= 0 || !viewModel.canRestartCamera()) {
            runnable()
            return
        }

        val previousCameraState = viewModel.cameraState.value
        viewModel.cameraState.value = CameraState.COUNTDOWN

        countDownView.onPreviewAreaChanged(Rect().apply {
            viewFinder.getGlobalVisibleRect(this)
        })
        countDownView.startCountDown(timerModeSeconds) {
            viewModel.cameraState.value = previousCameraState
            runnable()
        }
    }

    /**
     * Show a toast warning the user that no camera is available and close the activity.
     */
    private fun noCamera() {
        Toast.makeText(
            this, R.string.error_no_cameras_available, Toast.LENGTH_LONG
        ).show()
        finish()
    }

    /**
     * Method called when a new media have been successfully captured and saved.
     * Keep track of media items captured while in a secure lockscreen state so that they
     * can be passed to the gallery for viewing without unlocking the device. However, if the
     * keyguard is no longer locked, clear any existing URIs, and do not add this one.
     */
    private fun onCapturedMedia(item: Uri?) {
        updateGalleryButton(item, true)

        if (!keyguardManager.isKeyguardLocked) {
            secureMediaUris.clear()
        } else {
            item?.let {
                secureMediaUris.addFirst(it)
            }
        }
    }

    /**
     * If keyguard is not locked, remove any URIs that were stored while in the secure camera state.
     * Otherwise, remove any URIs that no longer exist.
     */
    private fun updateSecureMediaUris(keyguardLocked: Boolean) {
        if (!keyguardLocked) {
            secureMediaUris.clear()
        } else {
            secureMediaUris.removeIf { !viewModel.fileExists(it) }
        }
    }

    private fun handleHardwareKeyDown(
        keyCode: Int, event: KeyEvent?
    ) = HardwareKey.match(keyCode)?.let { (hardwareKey, tempIncrease) ->
        val increase = when (viewModel.getHardwareKeyInvert(hardwareKey)) {
            true -> !tempIncrease
            false -> tempIncrease
        }

        val gestureAction = viewModel.getHardwareKeyAction(hardwareKey)

        if (gestureAction.isTwoWayAction && !hardwareKey.isTwoWayKey) {
            Log.wtf(
                LOG_TAG,
                "${gestureAction.name} requires two-way key but ${hardwareKey.name} is not"
            )
            return@let true
        }

        when (gestureAction) {
            GestureAction.SHUTTER -> {
                if (viewModel.cameraMode.value == CameraMode.VIDEO
                    && viewModel.isShutterButtonEnabled.value
                    && event?.repeatCount == 0
                ) {
                    shutterButton.performClick()
                }
                true
            }

            GestureAction.FOCUS -> {
                if (event?.repeatCount == 0) {
                    viewFinderTouchEvent = null
                    viewFinder.performClick()
                }
                true
            }

            GestureAction.MIC_MUTE -> {
                if (viewModel.cameraMode.value == CameraMode.VIDEO && micButton.isEnabled &&
                    event?.repeatCount == 0
                ) {
                    viewModel.toggleVideoMicrophoneEnabled()
                }
                true
            }

            GestureAction.ZOOM -> {
                when (increase) {
                    true -> viewModel.zoomIn()
                    false -> viewModel.zoomOut()
                }
                true
            }

            GestureAction.DEFAULT -> {
                if (hardwareKey.supportsDefault) {
                    super.onKeyDown(keyCode, event)
                } else {
                    Log.wtf(
                        LOG_TAG,
                        "Got GestureAction.DEFAULT for ${hardwareKey.name} which doesn't support it"
                    )
                    true
                }
            }

            GestureAction.NOTHING -> {
                // Do nothing
                true
            }
        }
    }

    private fun handleHardwareKeyUp(
        keyCode: Int, event: KeyEvent?
    ) = HardwareKey.match(keyCode)?.let { (hardwareKey, _) ->
        val gestureAction = viewModel.getHardwareKeyAction(hardwareKey)

        if (gestureAction.isTwoWayAction && !hardwareKey.isTwoWayKey) {
            Log.wtf(
                LOG_TAG,
                "${gestureAction.name} requires two-way key but ${hardwareKey.name} is not"
            )
            return@let true
        }

        when (gestureAction) {
            GestureAction.SHUTTER -> {
                if (viewModel.cameraMode.value != CameraMode.QR
                    && viewModel.isShutterButtonEnabled.value
                ) {
                    shutterButton.performClick()
                }
                true
            }

            GestureAction.FOCUS -> {
                true
            }

            GestureAction.MIC_MUTE -> {
                true
            }

            GestureAction.ZOOM -> {
                true
            }

            GestureAction.DEFAULT -> {
                if (hardwareKey.supportsDefault) {
                    super.onKeyDown(keyCode, event)
                } else {
                    Log.wtf(
                        LOG_TAG,
                        "Got GestureAction.DEFAULT for ${hardwareKey.name} which doesn't support it"
                    )
                    true
                }
            }

            GestureAction.NOTHING -> {
                // Do nothing
                true
            }
        }
    }

    companion object {
        private val LOG_TAG = CameraActivity::class.simpleName!!

        private const val MSG_HIDE_ZOOM_SLIDER = 0
        private const val MSG_HIDE_FOCUS_RING = 1
        private const val MSG_HIDE_EXPOSURE_SLIDER = 2
        private const val MSG_ON_PINCH_TO_ZOOM = 3

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
