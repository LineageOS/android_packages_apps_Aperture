/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture
import androidx.camera.extensions.ExtensionMode
import androidx.camera.video.Quality
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.findViewTreeLifecycleOwner
import org.lineageos.aperture.R
import org.lineageos.aperture.camera.CameraMode
import org.lineageos.aperture.camera.CameraState
import org.lineageos.aperture.camera.CameraViewModel
import org.lineageos.aperture.camera.FlashMode
import org.lineageos.aperture.ext.smoothRotate
import org.lineageos.aperture.utils.GridMode
import org.lineageos.aperture.utils.Rotation
import org.lineageos.aperture.utils.TimerMode

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
@androidx.camera.core.ExperimentalZeroShutterLag
@androidx.camera.view.video.ExperimentalVideo
class SecondaryBarLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    // Views
    val aspectRatioButton by lazy { findViewById<Button>(R.id.aspectRatioButton)!! }
    val effectButton by lazy { findViewById<Button>(R.id.effectButton)!! }
    val flashButton by lazy { findViewById<ImageButton>(R.id.flashButton)!! }
    val gridButton by lazy { findViewById<Button>(R.id.gridButton)!! }
    val lensSelectorLayout by lazy { findViewById<LensSelectorLayout>(R.id.lensSelectorLayout)!! }
    val micButton by lazy { findViewById<Button>(R.id.micButton)!! }
    val proButton by lazy { findViewById<ImageButton>(R.id.proButton)!! }
    val settingsButton by lazy { findViewById<Button>(R.id.settingsButton)!! }
    val timerButton by lazy { findViewById<Button>(R.id.timerButton)!! }
    val videoFrameRateButton by lazy { findViewById<Button>(R.id.videoFrameRateButton)!! }
    val videoQualityButton by lazy { findViewById<Button>(R.id.videoQualityButton)!! }

    // System services
    private val layoutInflater = context.getSystemService(LayoutInflater::class.java)

    // Open/close state
    private var isOut = true
    private var toggledAtLeastOnce = false

    internal var cameraViewModel: CameraViewModel? = null
        set(value) {
            val lifecycleOwner = findViewTreeLifecycleOwner() ?: return

            field?.let {
                // Unregister
                it.camera.removeObservers(lifecycleOwner)
                it.cameraMode.removeObservers(lifecycleOwner)
                it.cameraState.removeObservers(lifecycleOwner)
                it.screenRotation.removeObservers(lifecycleOwner)
                it.flashMode.removeObservers(lifecycleOwner)
                it.gridMode.removeObservers(lifecycleOwner)
                it.timerMode.removeObservers(lifecycleOwner)
                it.photoCaptureMode.removeObservers(lifecycleOwner)
                it.photoAspectRatio.removeObservers(lifecycleOwner)
                it.photoEffect.removeObservers(lifecycleOwner)
                it.videoQuality.removeObservers(lifecycleOwner)
                it.videoFrameRate.removeObservers(lifecycleOwner)
                it.videoMicMode.removeObservers(lifecycleOwner)
            }

            field = value

            value?.let { cameraViewModel ->
                // Register
                cameraViewModel.camera.observe(lifecycleOwner) {
                    val camera = it ?: return@observe

                    flashButton.isVisible = camera.hasFlashUnit

                    updateSecondaryBarButtons()
                }

                cameraViewModel.cameraMode.observe(lifecycleOwner) {
                    val cameraMode = it ?: return@observe

                    aspectRatioButton.isVisible = cameraMode != CameraMode.VIDEO
                    videoQualityButton.isVisible = cameraMode == CameraMode.VIDEO
                    videoFrameRateButton.isVisible = cameraMode == CameraMode.VIDEO
                    micButton.isVisible = cameraMode == CameraMode.VIDEO

                    updateSecondaryBarButtons()
                }

                cameraViewModel.cameraState.observe(lifecycleOwner) {
                    val cameraState = it ?: return@observe

                    timerButton.isEnabled = cameraState == CameraState.IDLE
                    aspectRatioButton.isEnabled = cameraState == CameraState.IDLE
                    effectButton.isEnabled = cameraState == CameraState.IDLE
                    settingsButton.isEnabled = cameraState == CameraState.IDLE

                    updateSecondaryBarButtons()
                }

                cameraViewModel.screenRotation.observe(lifecycleOwner) {
                    val screenRotation = it ?: return@observe

                    updateViewsRotation(screenRotation)
                }

                cameraViewModel.flashMode.observe(lifecycleOwner) {
                    val flashMode = it ?: return@observe

                    flashButton.setImageDrawable(
                        ContextCompat.getDrawable(
                            context,
                            when (flashMode) {
                                FlashMode.OFF -> R.drawable.ic_flash_off
                                FlashMode.AUTO -> R.drawable.ic_flash_auto
                                FlashMode.ON -> R.drawable.ic_flash_on
                                FlashMode.TORCH -> R.drawable.ic_flash_torch
                            }
                        )
                    )
                }

                cameraViewModel.gridMode.observe(lifecycleOwner) {
                    val gridMode = it ?: return@observe

                    gridButton.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        when (gridMode) {
                            GridMode.OFF -> R.drawable.ic_grid_off
                            GridMode.ON_3 -> R.drawable.ic_grid_on_3
                            GridMode.ON_4 -> R.drawable.ic_grid_on_4
                            GridMode.ON_GOLDENRATIO -> R.drawable.ic_grid_on_goldenratio
                        },
                        0,
                        0
                    )
                    gridButton.text = resources.getText(
                        when (gridMode) {
                            GridMode.OFF -> R.string.grid_off
                            GridMode.ON_3 -> R.string.grid_on_3
                            GridMode.ON_4 -> R.string.grid_on_4
                            GridMode.ON_GOLDENRATIO -> R.string.grid_on_goldenratio
                        }
                    )
                }

                cameraViewModel.timerMode.observe(lifecycleOwner) {
                    val timerMode = it ?: return@observe

                    timerButton.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        when (timerMode) {
                            TimerMode.OFF -> R.drawable.ic_timer_off
                            TimerMode.ON_3S -> R.drawable.ic_timer_3
                            TimerMode.ON_10S -> R.drawable.ic_timer_10
                        },
                        0,
                        0
                    )
                    timerButton.text = resources.getText(
                        when (timerMode) {
                            TimerMode.OFF -> R.string.timer_off
                            TimerMode.ON_3S -> R.string.timer_3
                            TimerMode.ON_10S -> R.string.timer_10
                        }
                    )
                }

                cameraViewModel.photoCaptureMode.observe(lifecycleOwner) {
                    updateSecondaryBarButtons()
                }

                cameraViewModel.photoAspectRatio.observe(lifecycleOwner) {
                    val photoAspectRatio = it ?: return@observe

                    aspectRatioButton.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        when (photoAspectRatio) {
                            AspectRatio.RATIO_4_3 -> R.drawable.ic_aspect_ratio_4_3
                            AspectRatio.RATIO_16_9 -> R.drawable.ic_aspect_ratio_16_9
                            else -> throw Exception("Unknown aspect ratio $it")
                        },
                        0,
                        0
                    )
                    aspectRatioButton.text = resources.getText(
                        when (photoAspectRatio) {
                            AspectRatio.RATIO_4_3 -> R.string.aspect_ratio_4_3
                            AspectRatio.RATIO_16_9 -> R.string.aspect_ratio_16_9
                            else -> throw Exception("Unknown aspect ratio $it")
                        }
                    )
                }

                cameraViewModel.photoEffect.observe(lifecycleOwner) {
                    val photoEffect = it ?: return@observe

                    effectButton.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        when (photoEffect) {
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

                cameraViewModel.videoQuality.observe(lifecycleOwner) {
                    val videoQuality = it ?: return@observe

                    videoQualityButton.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        when (videoQuality) {
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
                        when (videoQuality) {
                            Quality.SD -> R.string.video_quality_sd
                            Quality.HD -> R.string.video_quality_hd
                            Quality.FHD -> R.string.video_quality_fhd
                            Quality.UHD -> R.string.video_quality_uhd
                            else -> throw Exception("Unknown video quality $it")
                        }
                    )

                    updateSecondaryBarButtons()
                }

                cameraViewModel.videoFrameRate.observe(lifecycleOwner) {
                    val videoFrameRate = it

                    videoFrameRateButton.text = videoFrameRate?.let { frameRate ->
                        resources.getString(R.string.video_frame_rate_value, frameRate.value)
                    } ?: resources.getString(R.string.video_frame_rate_auto)
                }

                cameraViewModel.videoMicMode.observe(lifecycleOwner) {
                    val videoMicMode = it ?: return@observe

                    micButton.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        if (videoMicMode) R.drawable.ic_mic_on else R.drawable.ic_mic_off,
                        0,
                        0
                    )
                    micButton.text = resources.getText(
                        if (videoMicMode) R.string.mic_on else R.string.mic_off
                    )
                }

                cameraViewModel.videoAudioConfig.observe(lifecycleOwner) {
                    updateSecondaryBarButtons()
                }
            }
        }

    init {
        layoutInflater.inflate(R.layout.secondary_bar_layout, this)

        proButton.setOnClickListener { slide() }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (!toggledAtLeastOnce) {
            slideDown(true)
        }
    }

    fun slideDown(now: Boolean = false) {
        if (!isOut) {
            return
        }

        isOut = false
        toggledAtLeastOnce = true

        animate()
            .translationY(measuredHeight.toFloat() / 2)
            .duration = if (now) 0 else 300
    }

    fun slideUp(now: Boolean = false) {
        if (isOut) {
            return
        }

        isOut = true
        toggledAtLeastOnce = true

        animate()
            .translationY(0f)
            .duration = if (now) 0 else 300
    }

    fun slide() {
        if (isOut) {
            slideDown()
        } else {
            slideUp()
        }
    }

    private fun updateViewsRotation(screenRotation: Rotation) {
        val compensationValue = screenRotation.compensationValue.toFloat()

        proButton.smoothRotate(compensationValue)
        lensSelectorLayout.screenRotation = screenRotation
        flashButton.smoothRotate(compensationValue)
    }

    /**
     * Some UI elements requires checking more than one value, this function will be called
     * when one of these values will change.
     */
    private fun updateSecondaryBarButtons() {
        val cameraViewModel = cameraViewModel ?: return

        val camera = cameraViewModel.camera.value ?: return
        val cameraMode = cameraViewModel.cameraMode.value ?: return
        val cameraState = cameraViewModel.cameraState.value ?: return
        val photoCaptureMode = cameraViewModel.photoCaptureMode.value ?: return
        val videoQuality = cameraViewModel.videoQuality.value ?: return
        val videoAudioConfig = cameraViewModel.videoAudioConfig.value ?: return

        val supportedVideoQualities = camera.supportedVideoQualities
        val supportedVideoFrameRates = supportedVideoQualities.getOrDefault(
            videoQuality, setOf()
        )

        flashButton.isEnabled = cameraMode != CameraMode.PHOTO || cameraState == CameraState.IDLE
        effectButton.isVisible = cameraMode == CameraMode.PHOTO &&
                photoCaptureMode != ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG &&
                camera.supportedExtensionModes.size > 1
        videoQualityButton.isEnabled =
            cameraState == CameraState.IDLE && supportedVideoQualities.size > 1
        videoFrameRateButton.isEnabled =
            cameraState == CameraState.IDLE && supportedVideoFrameRates.size > 1
        micButton.isEnabled = cameraState == CameraState.IDLE || videoAudioConfig.audioEnabled
    }
}
