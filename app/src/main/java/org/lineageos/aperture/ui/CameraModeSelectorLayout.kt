/*
 * SPDX-FileCopyrightText: 2022-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.core.view.doOnLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.google.android.material.button.MaterialButton
import org.lineageos.aperture.R
import org.lineageos.aperture.ext.px
import org.lineageos.aperture.models.CameraMode
import org.lineageos.aperture.models.CameraState
import org.lineageos.aperture.utils.TimeUtils
import org.lineageos.aperture.viewmodels.CameraViewModel
import kotlin.reflect.cast

@ExperimentalCamera2Interop
class CameraModeSelectorLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    // Views
    private val cameraModeHighlightButton by lazy { findViewById<MaterialButton>(R.id.cameraModeHighlightButton) }
    private val cameraModeButtonsLinearLayout by lazy { findViewById<LinearLayout>(R.id.cameraModeButtonsLinearLayout) }
    private val videoDurationButton by lazy { findViewById<MaterialButton>(R.id.videoDurationButton) }

    // System services
    private val layoutInflater by lazy { context.getSystemService(LayoutInflater::class.java) }

    private val cameraToButton = mutableMapOf<CameraMode, MaterialButton>()

    private val cameraModeObserver = Observer { cameraMode: CameraMode ->
        val currentCameraModeButton =
            cameraToButton[cameraMode] ?: throw Exception("No button for $cameraMode")

        cameraToButton.forEach {
            it.value.isEnabled = cameraMode != it.key
        }

        // Animate camera mode change
        doOnLayout {
            // Animate position
            ValueAnimator.ofFloat(
                cameraModeHighlightButton.x, currentCameraModeButton.x + 16.px
            ).apply {
                addUpdateListener { valueAnimator ->
                    cameraModeHighlightButton.x = valueAnimator.animatedValue as Float
                }
            }.start()

            // Animate width
            ValueAnimator.ofInt(
                cameraModeHighlightButton.width, currentCameraModeButton.width
            ).apply {
                addUpdateListener { valueAnimator ->
                    cameraModeHighlightButton.width = valueAnimator.animatedValue as Int
                }
            }.start()
        }
    }

    private val inSingleCaptureModeObserver = Observer { _: Boolean ->
        updateButtons()
    }

    private val cameraStateObserver = Observer { cameraState: CameraState ->
        updateButtons()

        // Update video duration button
        videoDurationButton.isVisible = cameraState.isRecordingVideo
    }

    private val videoDurationObserver = Observer { videoDuration: Long ->
        videoDurationButton.text = TimeUtils.convertNanosToString(videoDuration)
    }

    internal var cameraViewModel: CameraViewModel? = null
        set(value) {
            // Unregister
            field?.cameraMode?.removeObserver(cameraModeObserver)
            field?.inSingleCaptureMode?.removeObserver(inSingleCaptureModeObserver)
            field?.cameraState?.removeObserver(cameraStateObserver)
            field?.videoRecordingDuration?.removeObserver(videoDurationObserver)

            field = value

            val lifecycleOwner = findViewTreeLifecycleOwner() ?: return

            value?.cameraMode?.observe(lifecycleOwner, cameraModeObserver)
            value?.inSingleCaptureMode?.observe(lifecycleOwner, inSingleCaptureModeObserver)
            value?.cameraState?.observe(lifecycleOwner, cameraStateObserver)
            value?.videoRecordingDuration?.observe(lifecycleOwner, videoDurationObserver)
        }

    var onModeSelectedCallback: (cameraMode: CameraMode) -> Unit = {}

    init {
        inflate(context, R.layout.camera_mode_selector_layout, this)

        for (cameraMode in CameraMode.values()) {
            cameraToButton[cameraMode] = MaterialButton::class.cast(
                layoutInflater.inflate(
                    R.layout.camera_mode_button, this, false
                )
            ).apply {
                setText(cameraMode.title)
                setOnClickListener { onModeSelectedCallback(cameraMode) }
            }.also {
                cameraModeButtonsLinearLayout.addView(it)
            }
        }
    }

    private fun updateButtons() {
        val inSingleCaptureMode = cameraViewModel?.inSingleCaptureMode?.value ?: return
        val cameraState = cameraViewModel?.cameraState?.value ?: return

        cameraModeHighlightButton.isInvisible = cameraState.isRecordingVideo || inSingleCaptureMode
        cameraToButton.forEach {
            it.value.isInvisible = cameraState.isRecordingVideo || inSingleCaptureMode
        }
    }
}
