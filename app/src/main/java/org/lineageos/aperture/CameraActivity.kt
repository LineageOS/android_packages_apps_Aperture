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
import android.os.PowerManager
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.MirrorMode
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.extensions.ExtensionMode
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.isAudioSourceConfigured
import androidx.camera.video.muted
import androidx.camera.view.CameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.ScreenFlashView
import androidx.camera.view.onPinchToZoom
import androidx.camera.view.video.AudioConfig
import androidx.camera.viewfinder.core.ZoomGestureDetector
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.addListener
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.children
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import coil3.load
import coil3.request.ErrorResult
import coil3.request.ImageRequest
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.lineageos.aperture.ext.camera2CameraControl
import org.lineageos.aperture.ext.colorCorrectionAberrationMode
import org.lineageos.aperture.ext.distortionCorrectionMode
import org.lineageos.aperture.ext.edgeMode
import org.lineageos.aperture.ext.flashMode
import org.lineageos.aperture.ext.forceTorchHelpShown
import org.lineageos.aperture.ext.getHardwareKeyAction
import org.lineageos.aperture.ext.getHardwareKeyInvert
import org.lineageos.aperture.ext.hotPixelMode
import org.lineageos.aperture.ext.lastCameraFacing
import org.lineageos.aperture.ext.lastCameraMode
import org.lineageos.aperture.ext.lastMicMode
import org.lineageos.aperture.ext.mapToRange
import org.lineageos.aperture.ext.next
import org.lineageos.aperture.ext.nextPowerOfTwo
import org.lineageos.aperture.ext.noiseReductionMode
import org.lineageos.aperture.ext.permissionsGranted
import org.lineageos.aperture.ext.photoCaptureMode
import org.lineageos.aperture.ext.photoEffect
import org.lineageos.aperture.ext.photoFfcMirror
import org.lineageos.aperture.ext.photoFlashMode
import org.lineageos.aperture.ext.previous
import org.lineageos.aperture.ext.previousPowerOfTwo
import org.lineageos.aperture.ext.px
import org.lineageos.aperture.ext.saveLocation
import org.lineageos.aperture.ext.scale
import org.lineageos.aperture.ext.setColorCorrectionAberrationMode
import org.lineageos.aperture.ext.setDistortionCorrectionMode
import org.lineageos.aperture.ext.setEdgeMode
import org.lineageos.aperture.ext.setFrameRate
import org.lineageos.aperture.ext.setHotPixel
import org.lineageos.aperture.ext.setNoiseReductionMode
import org.lineageos.aperture.ext.setPadding
import org.lineageos.aperture.ext.setShadingMode
import org.lineageos.aperture.ext.setVideoStabilizationMode
import org.lineageos.aperture.ext.shadingMode
import org.lineageos.aperture.ext.slide
import org.lineageos.aperture.ext.slideDown
import org.lineageos.aperture.ext.smoothRotate
import org.lineageos.aperture.ext.transform
import org.lineageos.aperture.ext.updateBarsVisibility
import org.lineageos.aperture.ext.videoDynamicRange
import org.lineageos.aperture.ext.videoFlashMode
import org.lineageos.aperture.ext.videoFrameRate
import org.lineageos.aperture.ext.videoMirrorMode
import org.lineageos.aperture.ext.videoQuality
import org.lineageos.aperture.ext.videoStabilization
import org.lineageos.aperture.models.AssistantIntent
import org.lineageos.aperture.models.CameraFacing
import org.lineageos.aperture.models.CameraMode
import org.lineageos.aperture.models.CameraState
import org.lineageos.aperture.models.ColorCorrectionAberrationMode
import org.lineageos.aperture.models.DistortionCorrectionMode
import org.lineageos.aperture.models.EdgeMode
import org.lineageos.aperture.models.FlashMode
import org.lineageos.aperture.models.FrameRate
import org.lineageos.aperture.models.GestureAction
import org.lineageos.aperture.models.GridMode
import org.lineageos.aperture.models.HardwareKey
import org.lineageos.aperture.models.HotPixelMode
import org.lineageos.aperture.models.MediaType
import org.lineageos.aperture.models.NoiseReductionMode
import org.lineageos.aperture.models.Rotation
import org.lineageos.aperture.models.ShadingMode
import org.lineageos.aperture.models.TimerMode
import org.lineageos.aperture.models.VideoMirrorMode
import org.lineageos.aperture.models.VideoStabilizationMode
import org.lineageos.aperture.qr.QrImageAnalyzer
import org.lineageos.aperture.ui.CameraModeSelectorLayout
import org.lineageos.aperture.ui.CapturePreviewLayout
import org.lineageos.aperture.ui.CountDownView
import org.lineageos.aperture.ui.GridView
import org.lineageos.aperture.ui.HorizontalSlider
import org.lineageos.aperture.ui.InfoChipView
import org.lineageos.aperture.ui.LensSelectorLayout
import org.lineageos.aperture.ui.LevelerView
import org.lineageos.aperture.ui.LocationPermissionsDialog
import org.lineageos.aperture.ui.PreviewBlurView
import org.lineageos.aperture.ui.VerticalSlider
import org.lineageos.aperture.utils.BroadcastUtils
import org.lineageos.aperture.utils.CameraSoundsUtils
import org.lineageos.aperture.utils.ExifUtils
import org.lineageos.aperture.utils.GoogleLensUtils
import org.lineageos.aperture.utils.MediaStoreUtils
import org.lineageos.aperture.utils.PermissionsChecker
import org.lineageos.aperture.utils.PermissionsUtils
import org.lineageos.aperture.utils.ShortcutsUtils
import org.lineageos.aperture.utils.StorageUtils
import org.lineageos.aperture.viewmodels.CameraViewModel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream
import kotlin.math.abs
import kotlin.reflect.safeCast
import androidx.camera.core.CameraState as CameraXCameraState

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
@androidx.camera.core.ExperimentalZeroShutterLag
open class CameraActivity : AppCompatActivity(R.layout.activity_camera) {
    // View models
    private val viewModel: CameraViewModel by viewModels()

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
    private val infoChipView by lazy { findViewById<InfoChipView>(R.id.infoChipView) }
    private val lensSelectorLayout by lazy { findViewById<LensSelectorLayout>(R.id.lensSelectorLayout) }
    private val levelerView by lazy { findViewById<LevelerView>(R.id.levelerView) }
    private val mainLayout by lazy { findViewById<ConstraintLayout>(R.id.mainLayout) }
    private val micButton by lazy { findViewById<Button>(R.id.micButton) }
    private val previewBlurView by lazy { findViewById<PreviewBlurView>(R.id.previewBlurView) }
    private val primaryBarLayout by lazy { findViewById<ConstraintLayout>(R.id.primaryBarLayout) }
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

    // Core camera utils
    private val cameraSoundsUtils by lazy {
        CameraSoundsUtils(sharedPreferences)
    }
    private val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    private lateinit var initialCameraFacing: CameraFacing

    /**
     * The currently shown URI.
     */
    private var galleryButtonUri: Uri? = null

    /**
     * Medias captured from secure activity will be stored here
     * NOTE: Order is important, the first element is the newest one.
     */
    private val secureMediaUris = ArrayDeque<Uri>()

    private var zoomGestureMutex = Mutex()

    // Video
    private lateinit var videoAudioConfig: AudioConfig

    // QR
    private val imageAnalyzer by lazy { QrImageAnalyzer(this, lifecycleScope) }
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
                            viewModel.cameraMode.value.previous()?.let { cameraMode ->
                                changeCameraMode(cameraMode)
                            }
                        } else {
                            // Right to left
                            viewModel.cameraMode.value.next()?.let { cameraMode ->
                                changeCameraMode(cameraMode)
                            }
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
    private val permissionsChecker = PermissionsChecker(this, PermissionsUtils.mainPermissions)
    private val locationPermissionsChecker = PermissionsChecker(
        this,
        PermissionsUtils.locationPermissions,
        optional = true,
    )

    private val locationPermissionsRequestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        sharedPreferences.saveLocation = permissionsGranted(PermissionsUtils.locationPermissions)
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

    private val forceTorchSnackbar by lazy {
        Snackbar.make(
            secondaryBottomBarLayout, R.string.force_torch_help, Snackbar.LENGTH_INDEFINITE
        )
            .setAnchorView(secondaryBottomBarLayout)
            .setAction(android.R.string.ok) {
                sharedPreferences.forceTorchHelpShown = true
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
            viewModel.cameraMode.value = CameraMode.PHOTO
            viewModel.inSingleCaptureMode.value = true
        },
        MediaStore.ACTION_IMAGE_CAPTURE_SECURE to {
            viewModel.cameraMode.value = CameraMode.PHOTO
            viewModel.inSingleCaptureMode.value = true
        },
        MediaStore.ACTION_VIDEO_CAPTURE to {
            viewModel.cameraMode.value = CameraMode.VIDEO
            viewModel.inSingleCaptureMode.value = true
        },
        MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA to {
            viewModel.cameraMode.value = CameraMode.PHOTO
        },
        MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE to {
            viewModel.cameraMode.value = CameraMode.PHOTO
        },
        MediaStore.INTENT_ACTION_VIDEO_CAMERA to {
            viewModel.cameraMode.value = CameraMode.VIDEO
        },

        // Shortcuts
        ShortcutsUtils.SHORTCUT_ID_SELFIE to {
            viewModel.cameraMode.value = CameraMode.PHOTO
            initialCameraFacing = CameraFacing.FRONT
        },
        ShortcutsUtils.SHORTCUT_ID_VIDEO to {
            viewModel.cameraMode.value = CameraMode.VIDEO
            initialCameraFacing = CameraFacing.BACK
        },
        ShortcutsUtils.SHORTCUT_ID_QR to {
            viewModel.cameraMode.value = CameraMode.QR
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

        // Initialize camera mode and facing
        viewModel.cameraMode.value = overrideInitialCameraMode() ?: sharedPreferences.lastCameraMode
        initialCameraFacing = sharedPreferences.lastCameraFacing

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

        if (viewModel.cameraMode.value == CameraMode.VIDEO
            && !viewModel.videoRecordingAvailable()
        ) {
            // If an app asked for a video we have to bail out
            if (viewModel.inSingleCaptureMode.value) {
                Toast.makeText(
                    this, getString(R.string.camcorder_unsupported_toast), Toast.LENGTH_LONG
                ).show()
                finish()
                return
            }
            // Fallback to photo mode
            viewModel.cameraMode.value = CameraMode.PHOTO
        }

        // Select a camera
        viewModel.camera.value = viewModel.getCameraOfFacingOrFirstAvailable(
            initialCameraFacing
        ) ?: run {
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
        aspectRatioButton.setOnClickListener { cycleAspectRatio() }
        videoQualityButton.setOnClickListener { cycleVideoQuality() }
        videoFrameRateButton.setOnClickListener { cycleVideoFrameRate() }
        videoDynamicRangeButton.setOnClickListener { cycleVideoDynamicRange() }
        effectButton.setOnClickListener { cyclePhotoEffects() }
        gridButton.setOnClickListener { viewModel.cycleGridMode() }
        timerButton.setOnClickListener { viewModel.toggleTimerMode() }
        micButton.setOnClickListener { toggleMicrophoneMode() }
        settingsButton.setOnClickListener { openSettings() }

        // Set secondary bottom bar button callbacks
        proButton.setOnClickListener {
            secondaryTopBarLayout.slide()
        }
        flashButton.setOnClickListener { cycleFlashMode(false) }
        flashButton.setOnLongClickListener { cycleFlashMode(true) }

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
        viewFinder.setOnClickListener { view ->
            // Reset exposure level to 0 EV
            viewModel.cameraController.cameraControl?.setExposureCompensationIndex(0)
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
            "%.1fx".format(viewModel.cameraController.zoomState.value?.zoomRatio)
        }

        // Set expose level callback & text formatter
        exposureLevel.onProgressChangedByUser = {
            viewModel.camera.value?.let { camera ->
                viewModel.cameraController.cameraControl?.setExposureCompensationIndex(
                    Int.mapToRange(camera.exposureCompensationRange, it)
                )
            }

            handler.removeMessages(MSG_HIDE_EXPOSURE_SLIDER)
            handler.sendMessageDelayed(handler.obtainMessage(MSG_HIDE_EXPOSURE_SLIDER), 2000)
        }
        exposureLevel.textFormatter = {
            viewModel.camera.value?.let { camera ->
                val ev = Int.mapToRange(camera.exposureCompensationRange, it)
                if (ev == 0) {
                    "0"
                } else {
                    EXPOSURE_LEVEL_FORMATTER.format(ev).toString()
                }
            } ?: "0"
        }

        // Set primary bar button callbacks
        flipCameraButton.setOnClickListener { flipCamera() }
        googleLensButton.setOnClickListener {
            dismissKeyguardAndRun {
                GoogleLensUtils.launchGoogleLens(this)
            }
        }

        videoRecordingStateButton.setOnClickListener {
            when (viewModel.cameraState.value) {
                CameraState.RECORDING_VIDEO -> viewModel.videoRecording.value?.pause()
                CameraState.RECORDING_VIDEO_PAUSED -> viewModel.videoRecording.value?.resume()
                else -> throw Exception("videoRecordingStateButton clicked while in invalid state: ${viewModel.cameraState.value}")
            }
        }

        // Initialize shutter drawable
        when (viewModel.cameraMode.value) {
            CameraMode.PHOTO -> startShutterAnimation(ShutterAnimation.InitPhoto)
            CameraMode.VIDEO -> startShutterAnimation(ShutterAnimation.InitVideo)
            else -> {}
        }

        shutterButton.setOnClickListener {
            // Shutter animation
            when (viewModel.cameraMode.value) {
                CameraMode.PHOTO -> startShutterAnimation(ShutterAnimation.PhotoCapture)
                CameraMode.VIDEO -> {
                    if (countDownView.cancelCountDown()) {
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
                    CameraMode.PHOTO -> takePhoto()
                    CameraMode.VIDEO -> captureVideo()
                    else -> {}
                }
            }
        }

        galleryButtonCardView.setOnClickListener { openGallery() }

        // Set lens switching callback
        lensSelectorLayout.onCameraChangeCallback = {
            if (canRestartCamera()) {
                viewModel.camera.value = it
                bindCameraUseCases()
            }
        }
        lensSelectorLayout.onZoomRatioChangeCallback = {
            smoothZoom(it)
        }
        lensSelectorLayout.onResetZoomRatioCallback = {
            resetZoom()
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
            changeCameraMode(it)
        }

        // Bind viewfinder and preview blur view
        previewBlurView.previewView = viewFinder

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                permissionsChecker.withPermissionsGranted {
                    loadData()
                }
            }
        }

        // Also collect location
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (permissionsGranted(PermissionsUtils.locationPermissions)) {
                    locationPermissionsChecker.withPermissionsGranted {
                        viewModel.location.collect()
                    }
                }
            }
        }

        // Check again for permissions and re-bind the use cases
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                permissionsChecker.withPermissionsGranted {
                    bindCameraUseCases()

                    // This is a good time to ask the user for location permissions
                    if (sharedPreferences.saveLocation == null) {
                        locationPermissionsDialog.show()
                    }
                }
            }
        }
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
            viewModel.camera.collectLatest { camera ->
                updateSecondaryTopBarButtons()

                launch {
                    // Handle errors
                    camera?.cameraState?.asFlow()?.collectLatest { cameraState ->
                        cameraState?.error?.let {
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
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        launch {
            viewModel.cameraMode.collectLatest { cameraMode ->
                // Update info chip
                infoChipView.setCameraMode(cameraMode)

                // Update secondary top bar buttons
                aspectRatioButton.isVisible = cameraMode != CameraMode.VIDEO
                videoQualityButton.isVisible = cameraMode == CameraMode.VIDEO
                videoFrameRateButton.isVisible = cameraMode == CameraMode.VIDEO
                videoDynamicRangeButton.isVisible = cameraMode == CameraMode.VIDEO
                micButton.isVisible = cameraMode == CameraMode.VIDEO

                updateSecondaryTopBarButtons()

                // Update secondary bottom bar buttons
                secondaryBottomBarLayout.isVisible = cameraMode != CameraMode.QR

                // Update primary bar buttons
                primaryBarLayout.isVisible = cameraMode != CameraMode.QR

                // Update Google Lens button
                googleLensButton.isVisible = cameraMode == CameraMode.QR && isGoogleLensAvailable

                // Update camera mode selector
                cameraModeSelectorLayout.setCurrentCameraMode(cameraMode)
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

                updateSecondaryTopBarButtons()

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
                infoChipView.setScreenRotation(screenRotation)

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
            viewModel.flashMode.collectLatest { flashMode ->
                // Update secondary bar buttons
                flashButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        this@CameraActivity,
                        when (flashMode) {
                            FlashMode.OFF -> R.drawable.ic_flash_off
                            FlashMode.AUTO -> R.drawable.ic_flash_auto
                            FlashMode.ON -> R.drawable.ic_flash_on
                            FlashMode.TORCH -> R.drawable.ic_flashlight_on
                            FlashMode.SCREEN -> R.drawable.ic_flash_screen
                        }
                    )
                )
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
                            finish()
                        }

                        PowerManager.THERMAL_STATUS_SHUTDOWN -> {
                            showSnackBar(R.string.thermal_status_shutdown)
                            finish()
                        }
                    }
                }
            }
        }

        launch {
            viewModel.photoCaptureMode.collectLatest { photoCaptureMode ->
                // Update secondary bar buttons
                updateSecondaryTopBarButtons()
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
            viewModel.tapToFocusState.collectLatest { tapToFocusState ->
                tapToFocusState?.let {
                    when (it) {
                        CameraController.TAP_TO_FOCUS_STARTED -> {
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

                            handler.sendMessageDelayed(
                                handler.obtainMessage(MSG_HIDE_FOCUS_RING),
                                500
                            )
                        }
                    }
                }
            }
        }

        launch {
            viewModel.batteryIntent.collectLatest { batteryIntent ->
                infoChipView.setBatteryIntent(batteryIntent)
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

                updateSecondaryTopBarButtons()
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
            viewModel.videoDynamicRange.collectLatest { videoDynamicRange ->
                // Update secondary bar buttons
                videoDynamicRangeButton.setCompoundDrawablesWithIntrinsicBounds(
                    0, videoDynamicRange.icon, 0, 0
                )
                videoDynamicRangeButton.setText(videoDynamicRange.title)
            }
        }

        launch {
            viewModel.videoMicMode.collectLatest { videoMicMode ->
                // update info chip
                infoChipView.setVideoMicMode(videoMicMode)

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
            viewModel.videoRecording.collectLatest { videoRecording ->
                // Update secondary bar buttons
                updateSecondaryTopBarButtons()
            }
        }

        launch {
            viewModel.videoRecordingDuration.collectLatest { videoRecordingDuration ->
                cameraModeSelectorLayout.setVideoRecordingDuration(videoRecordingDuration)
            }
        }

        launch {
            viewModel.canFlipCamera.collectLatest { canFlipCamera ->
                flipCameraButton.isInvisible = !canFlipCamera
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

    private fun takePhoto() {
        // Bail out if a photo is already being taken
        if (viewModel.cameraState.value == CameraState.TAKING_PHOTO) {
            return
        }

        viewModel.cameraState.value = CameraState.TAKING_PHOTO
        shutterButton.isEnabled = false

        val photoOutputStream = if (viewModel.inSingleCaptureMode.value) {
            ByteArrayOutputStream(SINGLE_CAPTURE_PHOTO_BUFFER_INITIAL_SIZE_BYTES)
        } else {
            null
        }

        // Create output options object which contains file + metadata
        val outputOptions = StorageUtils.getPhotoMediaStoreOutputOptions(
            contentResolver,
            ImageCapture.Metadata().apply {
                if (!viewModel.inSingleCaptureMode.value) {
                    location = viewModel.location.value
                }
                if (viewModel.camera.value?.cameraFacing == CameraFacing.FRONT) {
                    isReversedHorizontal = sharedPreferences.photoFfcMirror
                }
            },
            photoOutputStream
        )

        // Set up image capture listener, which is triggered after photo has
        // been taken
        viewModel.cameraController.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onCaptureStarted() {
                    cameraSoundsUtils.playShutterClick()
                    viewFinder.foreground = ColorDrawable(Color.BLACK)
                    ValueAnimator.ofInt(0, 255, 0).apply {
                        addUpdateListener { anim ->
                            viewFinder.foreground.alpha = anim.animatedValue as Int
                        }
                    }.start()
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(LOG_TAG, "Photo capture failed: ${exc.message}", exc)
                    viewModel.cameraState.value = CameraState.IDLE
                    shutterButton.isEnabled = true
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d(LOG_TAG, "Photo capture succeeded: ${output.savedUri}")
                    viewModel.cameraState.value = CameraState.IDLE
                    shutterButton.isEnabled = true
                    if (!viewModel.inSingleCaptureMode.value) {
                        onCapturedMedia(output.savedUri)
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
        if (viewModel.cameraState.value != CameraState.IDLE) {
            if (viewModel.cameraController.isRecording) {
                // Stop the current recording session.
                viewModel.videoRecording.value?.stop()
            }
            return
        }

        // Disallow state changes while we are about to prepare for recording video
        viewModel.cameraState.value = CameraState.PRE_RECORDING_VIDEO

        // Update duration text
        viewModel.videoRecordingDuration.value = 0L

        // Create output options object which contains file + metadata
        val outputOptions = StorageUtils.getVideoMediaStoreOutputOptions(
            contentResolver,
            viewModel.location.value.takeUnless { viewModel.inSingleCaptureMode.value }
        )

        // Play shutter sound
        val delayTime = if (cameraSoundsUtils.playStartVideoRecording()) 500L else 0L

        handler.postDelayed({
            // Start recording
            viewModel.videoRecording.value = viewModel.cameraController.startRecording(
                outputOptions,
                videoAudioConfig,
                viewModel.cameraExecutor
            ) {
                when (it) {
                    is VideoRecordEvent.Start -> runOnUiThread {
                        viewModel.cameraState.value = CameraState.RECORDING_VIDEO
                        startVideoRecordingStateAnimation(VideoRecordingStateAnimation.Init)
                    }

                    is VideoRecordEvent.Pause -> runOnUiThread {
                        viewModel.cameraState.value = CameraState.RECORDING_VIDEO_PAUSED
                        startVideoRecordingStateAnimation(VideoRecordingStateAnimation.ResumeToPause)
                    }

                    is VideoRecordEvent.Resume -> runOnUiThread {
                        viewModel.cameraState.value = CameraState.RECORDING_VIDEO
                        startVideoRecordingStateAnimation(VideoRecordingStateAnimation.PauseToResume)
                    }

                    is VideoRecordEvent.Status -> runOnUiThread {
                        viewModel.videoRecordingDuration.value =
                            it.recordingStats.recordedDurationNanos
                    }

                    is VideoRecordEvent.Finalize -> {
                        runOnUiThread {
                            startShutterAnimation(ShutterAnimation.VideoEnd)
                        }
                        cameraSoundsUtils.playStopVideoRecording()
                        if (it.error != VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA) {
                            Log.d(LOG_TAG, "Video capture succeeded: ${it.outputResults.outputUri}")
                            if (!viewModel.inSingleCaptureMode.value) {
                                onCapturedMedia(it.outputResults.outputUri)
                                BroadcastUtils.broadcastNewVideo(this, it.outputResults.outputUri)
                            } else {
                                openCapturePreview(it.outputResults.outputUri, MediaType.VIDEO)
                            }
                        }
                        viewModel.cameraState.value = CameraState.IDLE
                        viewModel.videoRecording.value = null
                    }
                }
            }
        }, delayTime)
    }

    /**
     * Check if we can reinitialize the camera use cases
     */
    private fun canRestartCamera() =
        viewModel.cameraState.value == CameraState.IDLE && !countDownView.isVisible

    /**
     * Rebind cameraProvider use cases
     */
    private fun bindCameraUseCases() {
        // Show blurred preview
        previewBlurView.freeze()
        previewBlurView.isVisible = true

        // Unbind previous use cases
        viewModel.cameraController.unbind()

        viewModel.cameraState.value = CameraState.IDLE

        // Hide grid until preview is ready
        gridView.alpha = 0f

        // Get the desired camera
        var camera = when (viewModel.cameraMode.value) {
            CameraMode.QR -> viewModel.getCameraOfFacingOrFirstAvailable(CameraFacing.BACK)
            else -> viewModel.camera.value
        } ?: run {
            noCamera()
            return
        }

        // If the current camera doesn't support the selected camera mode
        // pick a different one, giving priority to camera facing
        if (!camera.supportsCameraMode(viewModel.cameraMode.value)) {
            camera = viewModel.getCameraOfFacingOrFirstAvailable(camera.cameraFacing) ?: run {
                noCamera()
                return
            }
        }

        viewModel.camera.value = camera

        // Fallback to ExtensionMode.NONE if necessary
        viewModel.photoEffect.value = listOf(
            viewModel.photoEffect.value,
            sharedPreferences.photoEffect,
        ).firstOrNull {
            camera.supportsExtensionMode(it)
        } ?: ExtensionMode.NONE

        // Fallback to FlashMode.OFF if necessary
        if (!camera.supportedFlashModes.contains(viewModel.flashMode.value)) {
            changeFlashMode(FlashMode.OFF)
        }

        // Initialize the use case we want and set its properties
        val cameraUseCases = when (viewModel.cameraMode.value) {
            CameraMode.QR -> {
                viewModel.cameraController.setImageAnalysisAnalyzer(
                    viewModel.cameraExecutor, imageAnalyzer
                )
                CameraController.IMAGE_ANALYSIS
            }

            CameraMode.PHOTO -> {
                viewModel.cameraController.imageCaptureResolutionSelector =
                    ResolutionSelector.Builder()
                        .setAspectRatioStrategy(
                            AspectRatioStrategy(
                                viewModel.photoAspectRatio.value,
                                AspectRatioStrategy.FALLBACK_RULE_AUTO
                            )
                        )
                        .setAllowedResolutionMode(
                            if (viewModel.overlayConfiguration.enableHighResolution) {
                                ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE
                            } else {
                                ResolutionSelector.PREFER_CAPTURE_RATE_OVER_HIGHER_RESOLUTION
                            }
                        )
                        .build()
                CameraController.IMAGE_CAPTURE
            }

            CameraMode.VIDEO -> {
                // Fallback to highest supported video quality
                if (!viewModel.supportedVideoQualities.contains(viewModel.videoQuality.value)) {
                    viewModel.videoQuality.value = viewModel.supportedVideoQualities.first()
                }
                viewModel.cameraController.videoCaptureQualitySelector =
                    QualitySelector.from(viewModel.videoQuality.value)

                // Set proper video frame rate
                viewModel.videoFrameRate.value = (FrameRate::getLowerOrHigher)(
                    viewModel.videoFrameRate.value ?: FrameRate.FPS_30,
                    viewModel.supportedVideoFrameRates
                )

                // Set video dynamic range
                viewModel.videoDynamicRange.value = viewModel.videoDynamicRange.value.takeIf {
                    viewModel.supportedVideoDynamicRanges.contains(it)
                } ?: viewModel.supportedVideoDynamicRanges.first()
                viewModel.cameraController.videoCaptureDynamicRange =
                    viewModel.videoDynamicRange.value.dynamicRange

                // Set video mirror mode
                viewModel.cameraController.videoCaptureMirrorMode = when (
                    sharedPreferences.videoMirrorMode
                ) {
                    VideoMirrorMode.OFF -> MirrorMode.MIRROR_MODE_OFF
                    VideoMirrorMode.ON -> MirrorMode.MIRROR_MODE_ON
                    VideoMirrorMode.ON_FFC_ONLY -> when (camera.cameraFacing) {
                        CameraFacing.FRONT -> MirrorMode.MIRROR_MODE_ON
                        else -> MirrorMode.MIRROR_MODE_OFF
                    }
                }

                CameraController.VIDEO_CAPTURE
            }
        }

        viewModel.photoCaptureMode.value = sharedPreferences.photoCaptureMode.takeIf {
            it != ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG || camera.supportsZsl
        } ?: ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY

        // Only photo mode supports vendor extensions for now
        val cameraSelector = if (
            viewModel.cameraMode.value == CameraMode.PHOTO &&
            viewModel.photoCaptureMode.value != ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG
        ) {
            viewModel.extensionsManager.getExtensionEnabledCameraSelector(
                camera.cameraSelector, viewModel.photoEffect.value
            )
        } else {
            camera.cameraSelector
        }

        // Bind use cases to camera
        viewModel.cameraController.cameraSelector = cameraSelector
        viewModel.cameraController.setEnabledUseCases(cameraUseCases)

        // Restore settings that needs a rebind
        viewModel.cameraController.imageCaptureMode = viewModel.photoCaptureMode.value

        // Bind camera controller to lifecycle
        viewModel.cameraController.bindToLifecycle(this)

        // Wait for camera to be ready
        viewModel.cameraController.initializationFuture.addListener({
            // Set Camera2 CaptureRequest options
            viewModel.cameraController.camera2CameraControl?.apply {
                captureRequestOptions = CaptureRequestOptions.Builder()
                    .apply {
                        setFrameRate(
                            if (viewModel.cameraMode.value == CameraMode.VIDEO) {
                                viewModel.videoFrameRate.value
                            } else {
                                null
                            }
                        )
                        setVideoStabilizationMode(
                            if (viewModel.cameraMode.value == CameraMode.VIDEO &&
                                sharedPreferences.videoStabilization
                            ) {
                                VideoStabilizationMode.getMode(camera)
                            } else {
                                VideoStabilizationMode.OFF
                            }
                        )
                        sharedPreferences.edgeMode?.takeIf {
                            camera.supportedEdgeModes.contains(it) && when (
                                viewModel.cameraMode.value
                            ) {
                                CameraMode.PHOTO -> viewModel.photoCaptureMode.value !=
                                        ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG ||
                                        EdgeMode.ALLOWED_MODES_ON_ZSL.contains(it)

                                CameraMode.VIDEO ->
                                    EdgeMode.ALLOWED_MODES_ON_VIDEO_MODE.contains(it)

                                CameraMode.QR -> false
                            }
                        }?.let {
                            setEdgeMode(it)
                        }
                        sharedPreferences.noiseReductionMode?.takeIf {
                            camera.supportedNoiseReductionModes.contains(it) && when (
                                viewModel.cameraMode.value
                            ) {
                                CameraMode.PHOTO -> viewModel.photoCaptureMode.value !=
                                        ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG ||
                                        NoiseReductionMode.ALLOWED_MODES_ON_ZSL.contains(it)

                                CameraMode.VIDEO ->
                                    NoiseReductionMode.ALLOWED_MODES_ON_VIDEO_MODE.contains(it)

                                CameraMode.QR -> false
                            }
                        }?.let {
                            setNoiseReductionMode(it)
                        }
                        sharedPreferences.shadingMode?.takeIf {
                            camera.supportedShadingModes.contains(it) && when (
                                viewModel.cameraMode.value
                            ) {
                                CameraMode.PHOTO -> viewModel.photoCaptureMode.value !=
                                        ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG ||
                                        ShadingMode.ALLOWED_MODES_ON_ZSL.contains(it)

                                CameraMode.VIDEO ->
                                    ShadingMode.ALLOWED_MODES_ON_VIDEO_MODE.contains(it)

                                CameraMode.QR -> false
                            }
                        }?.let {
                            setShadingMode(it)
                        }
                        sharedPreferences.colorCorrectionAberrationMode?.takeIf {
                            camera.supportedColorCorrectionAberrationModes.contains(it) && when (
                                viewModel.cameraMode.value
                            ) {
                                CameraMode.PHOTO -> viewModel.photoCaptureMode.value !=
                                        ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG ||
                                        ColorCorrectionAberrationMode.ALLOWED_MODES_ON_ZSL.contains(
                                            it
                                        )

                                CameraMode.VIDEO ->
                                    ColorCorrectionAberrationMode.ALLOWED_MODES_ON_VIDEO_MODE.contains(
                                        it
                                    )

                                CameraMode.QR -> false
                            }
                        }?.let {
                            setColorCorrectionAberrationMode(it)
                        }
                        sharedPreferences.distortionCorrectionMode?.takeIf {
                            camera.supportedDistortionCorrectionModes.contains(it) && when (
                                viewModel.cameraMode.value
                            ) {
                                CameraMode.PHOTO -> viewModel.photoCaptureMode.value !=
                                        ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG ||
                                        DistortionCorrectionMode.ALLOWED_MODES_ON_ZSL.contains(it)

                                CameraMode.VIDEO ->
                                    DistortionCorrectionMode.ALLOWED_MODES_ON_VIDEO_MODE.contains(it)

                                CameraMode.QR -> false
                            }
                        }?.let {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                setDistortionCorrectionMode(it)
                            }
                        }
                        sharedPreferences.hotPixelMode?.takeIf {
                            camera.supportedHotPixelModes.contains(it) && when (
                                viewModel.cameraMode.value
                            ) {
                                CameraMode.PHOTO -> viewModel.photoCaptureMode.value !=
                                        ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG ||
                                        HotPixelMode.ALLOWED_MODES_ON_ZSL.contains(it)

                                CameraMode.VIDEO ->
                                    HotPixelMode.ALLOWED_MODES_ON_VIDEO_MODE.contains(it)

                                CameraMode.QR -> false
                            }
                        }?.let {
                            setHotPixel(it)
                        }
                    }
                    .build()
            } ?: Log.wtf(LOG_TAG, "Camera2CameraControl not available even with camera ready?")
        }, ContextCompat.getMainExecutor(this))

        // Restore settings that can be set on the fly
        changeFlashMode(
            when (viewModel.cameraMode.value) {
                CameraMode.PHOTO -> sharedPreferences.photoFlashMode
                CameraMode.VIDEO -> sharedPreferences.videoFlashMode
                CameraMode.QR -> FlashMode.OFF
            }.takeIf { viewModel.supportedFlashModes.contains(it) } ?: FlashMode.OFF
        )
        setMicrophoneMode(viewModel.videoMicMode.value)

        // Reset exposure level
        exposureLevel.progress = 0.5f
        exposureLevel.steps =
            camera.exposureCompensationRange.upper - camera.exposureCompensationRange.lower

        // Update lens selector
        lensSelectorLayout.setCamera(
            camera, viewModel.getCameras(viewModel.cameraMode.value, camera.cameraFacing)
        )
    }

    /**
     * Change the current camera mode and restarts the stream
     */
    private fun changeCameraMode(cameraMode: CameraMode) {
        if (!canRestartCamera()) {
            return
        }

        if (cameraMode == viewModel.cameraMode.value) {
            return
        }

        when (cameraMode) {
            CameraMode.PHOTO -> {
                if (viewModel.cameraMode.value == CameraMode.VIDEO) {
                    startShutterAnimation(ShutterAnimation.VideoToPhoto)
                } else {
                    startShutterAnimation(ShutterAnimation.InitPhoto)
                }
            }

            CameraMode.VIDEO -> {
                if (!viewModel.videoRecordingAvailable()) {
                    Snackbar.make(
                        cameraModeSelectorLayout,
                        R.string.camcorder_unsupported_toast,
                        Snackbar.LENGTH_SHORT,
                    ).apply {
                        anchorView = cameraModeSelectorLayout
                        setAction(android.R.string.ok) {
                            // Do nothing
                        }
                    }.show()

                    return
                }

                if (viewModel.cameraMode.value == CameraMode.PHOTO) {
                    startShutterAnimation(ShutterAnimation.PhotoToVideo)
                } else {
                    startShutterAnimation(ShutterAnimation.InitVideo)
                }
            }

            else -> {}
        }

        viewModel.cameraMode.value = cameraMode
        sharedPreferences.lastCameraMode = cameraMode

        // Hide secondary top bar
        secondaryTopBarLayout.isVisible = false

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

        viewModel.camera.value = viewModel.getNextCamera() ?: run {
            noCamera()
            return
        }

        sharedPreferences.lastCameraFacing = viewModel.camera.value!!.cameraFacing

        bindCameraUseCases()
    }

    /**
     * Some UI elements requires checking more than one value, this function will be called
     * when one of these values will change.
     */
    private fun updateSecondaryTopBarButtons() {
        runOnUiThread {
            val camera = viewModel.camera.value ?: return@runOnUiThread
            val cameraMode = viewModel.cameraMode.value
            val cameraState = viewModel.cameraState.value
            val photoCaptureMode = viewModel.photoCaptureMode.value
            val videoQuality = viewModel.videoQuality.value
            val videoRecording = viewModel.videoRecording.value

            val supportedVideoQualities = camera.supportedVideoQualities
            val videoQualityInfo = supportedVideoQualities[videoQuality]
            val supportedVideoFrameRates = videoQualityInfo?.supportedFrameRates ?: setOf()
            val supportedVideoDynamicRanges = videoQualityInfo?.supportedDynamicRanges ?: setOf()

            val supportedFlashModes = cameraMode.supportedFlashModes.intersect(
                camera.supportedFlashModes
            )

            // Hide the button if the only available mode is off,
            // we want the user to know if any other mode is being used
            flashButton.isVisible =
                supportedFlashModes.size != 1 || supportedFlashModes.first() != FlashMode.OFF
            flashButton.isEnabled =
                cameraState == CameraState.IDLE || cameraMode == CameraMode.VIDEO
            effectButton.isVisible = cameraMode == CameraMode.PHOTO &&
                    photoCaptureMode != ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG &&
                    camera.supportedExtensionModes.size > 1
            videoQualityButton.isEnabled =
                cameraState == CameraState.IDLE && supportedVideoQualities.size > 1
            videoFrameRateButton.isEnabled =
                cameraState == CameraState.IDLE && supportedVideoFrameRates.size > 1
            videoDynamicRangeButton.isEnabled =
                cameraState == CameraState.IDLE && supportedVideoDynamicRanges.size > 1
            micButton.isEnabled =
                cameraState == CameraState.IDLE || videoRecording?.isAudioSourceConfigured == true
        }
    }

    private fun cycleAspectRatio() {
        if (!canRestartCamera()) {
            return
        }

        viewModel.cyclePhotoAspectRatio()

        bindCameraUseCases()
    }

    private fun cycleVideoQuality() {
        if (!canRestartCamera()) {
            return
        }

        val currentVideoQuality = viewModel.videoQuality.value

        viewModel.supportedVideoQualities.toList().sortedWith { a, b ->
            listOf(Quality.SD, Quality.HD, Quality.FHD, Quality.UHD).let {
                it.indexOf(a) - it.indexOf(b)
            }
        }.next(currentVideoQuality)?.takeUnless {
            it == currentVideoQuality
        }?.let {
            viewModel.videoQuality.value = it

            sharedPreferences.videoQuality = it

            bindCameraUseCases()
        }
    }

    private fun cycleVideoFrameRate() {
        if (!canRestartCamera()) {
            return
        }

        val currentVideoFrameRate = viewModel.videoFrameRate.value
        val newVideoFrameRate = viewModel.supportedVideoFrameRates.toList().sorted()
            .next(currentVideoFrameRate)

        if (newVideoFrameRate == currentVideoFrameRate) {
            return
        }

        viewModel.videoFrameRate.value = newVideoFrameRate

        sharedPreferences.videoFrameRate = viewModel.videoFrameRate.value

        bindCameraUseCases()
    }

    private fun cycleVideoDynamicRange() {
        if (!canRestartCamera()) {
            return
        }

        val currentVideoDynamicRange = viewModel.videoDynamicRange.value

        viewModel.supportedVideoDynamicRanges.toList().sorted().next(
            currentVideoDynamicRange
        )?.takeUnless {
            it == currentVideoDynamicRange
        }?.let {
            viewModel.videoDynamicRange.value = it

            sharedPreferences.videoDynamicRange = it

            bindCameraUseCases()
        }
    }

    /**
     * Set the specified flash mode, saving the value to shared prefs and updating the icon
     */
    private fun changeFlashMode(flashMode: FlashMode) {
        viewModel.cameraController.flashMode = flashMode

        viewModel.flashMode.value = flashMode
    }

    /**
     * Cycle flash mode
     * @param forceTorch Whether force torch mode should be toggled
     * @return false if called with an unsupported configuration, true otherwise
     */
    private fun cycleFlashMode(forceTorch: Boolean): Boolean {
        // Long-press is supported only on photo mode and if torch mode is available
        val forceTorchAvailable = viewModel.cameraMode.value == CameraMode.PHOTO
                && viewModel.camera.value?.supportedFlashModes.orEmpty().contains(FlashMode.TORCH)
        if (forceTorch && !forceTorchAvailable) {
            return false
        }

        val currentFlashMode = viewModel.flashMode.value

        when (forceTorch) {
            true -> when (currentFlashMode) {
                FlashMode.TORCH -> sharedPreferences.photoFlashMode.takeIf {
                    viewModel.supportedFlashModes.contains(it)
                } ?: FlashMode.OFF

                else -> FlashMode.TORCH
            }

            else -> viewModel.supportedFlashModes.toList().next(currentFlashMode)
        }?.let {
            changeFlashMode(it)

            if (!forceTorch) {
                when (viewModel.cameraMode.value) {
                    CameraMode.PHOTO -> sharedPreferences.photoFlashMode = it
                    CameraMode.VIDEO -> sharedPreferences.videoFlashMode = it
                    else -> {}
                }
            }
        }

        // Check if we should show the force torch suggestion
        if (!sharedPreferences.forceTorchHelpShown) {
            if (forceTorch) {
                // The user figured it out by themself
                sharedPreferences.forceTorchHelpShown = true
            } else if (!forceTorchSnackbar.isShownOrQueued) {
                forceTorchSnackbar.show()
            }
        }

        return true
    }

    /**
     * Toggles microphone during video recording
     */
    private fun toggleMicrophoneMode() {
        setMicrophoneMode(!viewModel.videoMicMode.value)
    }

    /**
     * Set the specified microphone mode, saving the value to shared prefs and updating the icon
     */
    @Suppress("MissingPermission")
    private fun setMicrophoneMode(microphoneMode: Boolean) {
        videoAudioConfig = if (microphoneMode) {
            AudioConfig.create(true)
        } else {
            AudioConfig.AUDIO_DISABLED
        }
        viewModel.videoRecording.value?.apply {
            muted = !microphoneMode
        }

        viewModel.videoMicMode.value = microphoneMode

        sharedPreferences.lastMicMode = viewModel.videoMicMode.value
    }

    /**
     * Cycle between supported photo camera effects
     */
    private fun cyclePhotoEffects() {
        if (!canRestartCamera()) {
            return
        }

        val currentExtensionMode = viewModel.photoEffect.value

        viewModel.camera.value?.supportedExtensionModes?.next(currentExtensionMode)?.takeUnless {
            it == currentExtensionMode
        }?.let {
            sharedPreferences.photoEffect = it

            bindCameraUseCases()
        }
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
                galleryButtonPreviewImageView.isVisible = true
                galleryButtonIconImageView.isVisible = false

                galleryButtonPreviewImageView.load(uri) {
                    decoderFactory(VideoFrameDecoder.Factory())
                    crossfade(true)
                    scale(Scale.FILL)
                    size(75.px)
                    error(R.drawable.ic_image)
                    fallback(R.drawable.ic_image)
                    listener(object : ImageRequest.Listener {
                        override fun onError(request: ImageRequest, result: ErrorResult) {
                            super.onError(request, result)

                            Log.e(LOG_TAG, "Failed to load gallery button icon", result.throwable)

                            galleryButtonPreviewImageView.isVisible = false
                            galleryButtonIconImageView.isVisible = true
                        }

                        override fun onCancel(request: ImageRequest) {
                            galleryButtonPreviewImageView.isVisible = false
                            galleryButtonIconImageView.isVisible = true
                            super.onCancel(request)
                        }
                    })
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

        if (timerModeSeconds <= 0 || !canRestartCamera()) {
            runnable()
            return
        }

        shutterButton.isEnabled = viewModel.cameraMode.value == CameraMode.VIDEO

        countDownView.onPreviewAreaChanged(Rect().apply {
            viewFinder.getGlobalVisibleRect(this)
        })
        countDownView.startCountDown(timerModeSeconds) {
            shutterButton.isEnabled = true
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
     * Apply the specified zoom smoothly. The value will be automatically clamped
     * between min and max.
     * @param zoomRatio The zoom ratio to apply
     */
    private fun smoothZoom(zoomRatio: Float) {
        val acquired = zoomGestureMutex.tryLock()
        if (!acquired) {
            return
        }

        val zoomState = viewModel.zoomState.value ?: return

        ValueAnimator.ofFloat(
            zoomState.zoomRatio,
            zoomRatio.coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
        ).apply {
            addUpdateListener {
                viewModel.cameraController.setZoomRatio(it.animatedValue as Float)
            }
            addListener(
                onEnd = {
                    zoomGestureMutex.unlock()
                }
            )
        }.start()
    }

    /**
     * Reset the zoom to 1.0x (value relative to the lens, not of the FOV).
     */
    private fun resetZoom() = smoothZoom(1f)

    /**
     * Zoom in by a power of 2.
     */
    private fun zoomIn() = viewModel.cameraController.zoomState.value?.zoomRatio?.let {
        smoothZoom(it.nextPowerOfTwo())
    }

    /**
     * Zoom out by a power of 2.
     */
    private fun zoomOut() = viewModel.cameraController.zoomState.value?.zoomRatio?.let {
        smoothZoom(it.previousPowerOfTwo())
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
            secureMediaUris.removeIf { !MediaStoreUtils.fileExists(this, it) }
        }
    }

    private fun handleHardwareKeyDown(
        keyCode: Int, event: KeyEvent?
    ) = HardwareKey.match(keyCode)?.let { (hardwareKey, tempIncrease) ->
        val increase = when (sharedPreferences.getHardwareKeyInvert(hardwareKey)) {
            true -> !tempIncrease
            false -> tempIncrease
        }

        val gestureAction = sharedPreferences.getHardwareKeyAction(hardwareKey)

        if (gestureAction.isTwoWayAction && !hardwareKey.isTwoWayKey) {
            Log.wtf(
                LOG_TAG,
                "${gestureAction.name} requires two-way key but ${hardwareKey.name} is not"
            )
            return@let true
        }

        when (gestureAction) {
            GestureAction.SHUTTER -> {
                if (viewModel.cameraMode.value == CameraMode.VIDEO && shutterButton.isEnabled &&
                    event?.repeatCount == 0
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
                    toggleMicrophoneMode()
                }
                true
            }

            GestureAction.ZOOM -> {
                when (increase) {
                    true -> zoomIn()
                    false -> zoomOut()
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
        val gestureAction = sharedPreferences.getHardwareKeyAction(hardwareKey)

        if (gestureAction.isTwoWayAction && !hardwareKey.isTwoWayKey) {
            Log.wtf(
                LOG_TAG,
                "${gestureAction.name} requires two-way key but ${hardwareKey.name} is not"
            )
            return@let true
        }

        when (sharedPreferences.getHardwareKeyAction(hardwareKey)) {
            GestureAction.SHUTTER -> {
                if (viewModel.cameraMode.value != CameraMode.QR && shutterButton.isEnabled) {
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
