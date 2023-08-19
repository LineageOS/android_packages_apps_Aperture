/*
 * SPDX-FileCopyrightText: 2022-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui

import android.content.Context
import android.icu.text.DecimalFormat
import android.icu.text.DecimalFormatSymbols
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.children
import androidx.core.view.setMargins
import androidx.lifecycle.findViewTreeLifecycleOwner
import org.lineageos.aperture.R
import org.lineageos.aperture.camera.Camera
import org.lineageos.aperture.camera.CameraState
import org.lineageos.aperture.camera.CameraViewModel
import org.lineageos.aperture.ext.*
import org.lineageos.aperture.utils.Rotation
import java.util.Locale

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class LensSelectorLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayoutCompat(context, attrs, defStyleAttr) {
    private val layoutInflater by lazy { context.getSystemService(LayoutInflater::class.java) }

    private lateinit var activeCamera: Camera

    private var usesLogicalZoomRatio = false
    private var currentZoomRatio = 1.0f

    private val buttonToApproximateZoomRatio = mutableMapOf<Button, Float>()

    private val buttonToCamera = mutableMapOf<Button, Camera>()
    private val buttonToZoomRatio = mutableMapOf<Button, Float>()

    var onCameraChangeCallback: (camera: Camera) -> Unit = {}
    var onZoomRatioChangeCallback: (zoomRatio: Float) -> Unit = {}

    internal var cameraViewModel: CameraViewModel? = null
        set(value) {
            val lifecycleOwner = findViewTreeLifecycleOwner() ?: return

            // Unregister
            field?.cameraState?.removeObservers(lifecycleOwner)
            field?.screenRotation?.removeObservers(lifecycleOwner)

            field = value

            value?.let { cameraViewModel ->
                cameraViewModel.cameraState.observe(lifecycleOwner) {
                    val cameraState = it ?: return@observe

                    children.forEach { view ->
                        view.isSoundEffectsEnabled = cameraState == CameraState.IDLE
                    }
                }
                cameraViewModel.screenRotation.observe(lifecycleOwner) {
                    val screenRotation = it ?: return@observe

                    updateViewsRotation(screenRotation)
                }
            }
        }
    private val screenRotation
        get() = cameraViewModel?.screenRotation?.value ?: Rotation.ROTATION_0

    fun setCamera(activeCamera: Camera, availableCameras: Collection<Camera>) {
        this.activeCamera = activeCamera

        removeAllViews()
        buttonToApproximateZoomRatio.clear()

        buttonToCamera.clear()
        buttonToZoomRatio.clear()

        usesLogicalZoomRatio = activeCamera.logicalZoomRatios.size > 1

        if (usesLogicalZoomRatio) {
            for ((approximateZoomRatio, exactZoomRatio) in activeCamera.logicalZoomRatios) {
                val button = inflateButton().apply {
                    setOnClickListener {
                        buttonToZoomRatio[it]?.let(onZoomRatioChangeCallback)
                    }
                    text = formatZoomRatio(approximateZoomRatio)
                }

                addView(button)
                buttonToZoomRatio[button] = exactZoomRatio
                buttonToApproximateZoomRatio[button] = approximateZoomRatio
            }
        } else {
            for (camera in availableCameras.sortedBy { it.intrinsicZoomRatio }) {
                val button = inflateButton().apply {
                    setOnClickListener {
                        buttonToCamera[it]?.let(onCameraChangeCallback)
                    }
                    text = formatZoomRatio(camera.intrinsicZoomRatio)
                }

                addView(button)
                buttonToCamera[button] = camera
                buttonToApproximateZoomRatio[button] = camera.intrinsicZoomRatio
            }
        }

        updateButtonsAttributes()
    }

    fun onZoomRatioChanged(zoomRatio: Float) {
        currentZoomRatio = zoomRatio
        updateButtonsAttributes()
    }

    private fun inflateButton(): Button {
        val button = layoutInflater.inflate(R.layout.lens_selector_button, this, false) as Button
        return button.apply {
            layoutParams = LayoutParams(32.px, 32.px).apply {
                setMargins(5)
            }
        }
    }

    private fun updateButtonsAttributes() {
        if (usesLogicalZoomRatio) {
            buttonToZoomRatio.asSequence().let {
                val firstButton = it.first().key
                val lastButton = it.last().key

                var activeButton: Button? = null
                for ((button, exactZoomRatio) in it) {
                    if (currentZoomRatio >= exactZoomRatio) {
                        activeButton = button
                    } else if (activeButton != null) {
                        break
                    } else if (button == firstButton || button == lastButton) {
                        activeButton = button
                        break
                    }
                }

                for ((button, _) in it) {
                    updateButtonAttributes(button, button == activeButton)
                }
            }
        } else {
            for ((button, camera) in buttonToCamera) {
                updateButtonAttributes(button, camera == activeCamera)
            }
        }
    }

    @Suppress("SetTextI18n")
    private fun updateButtonAttributes(button: Button, currentCamera: Boolean) {
        button.isEnabled = !currentCamera
        val formattedZoomRatio = formatZoomRatio(buttonToApproximateZoomRatio[button]!!)
        button.text = if (currentCamera) {
            "${formattedZoomRatio}×"
        } else {
            formattedZoomRatio
        }
        button.rotation = screenRotation.compensationValue.toFloat()
    }

    private fun updateViewsRotation(screenRotation: Rotation) {
        val rotation = screenRotation.compensationValue.toFloat()

        for (button in buttonToApproximateZoomRatio.keys) {
            button.smoothRotate(rotation)
        }
    }

    private fun formatZoomRatio(zoomRatio: Float): String =
        if (zoomRatio < 1f) {
            ZOOM_RATIO_FORMATTER_SUB_ONE.format(zoomRatio)
        } else {
            ZOOM_RATIO_FORMATTER.format(zoomRatio)
        }

    companion object {
        private val DECIMAL_FORMAT_SYMBOLS = DecimalFormatSymbols(Locale.US)

        private val ZOOM_RATIO_FORMATTER = DecimalFormat("0.#", DECIMAL_FORMAT_SYMBOLS)
        private val ZOOM_RATIO_FORMATTER_SUB_ONE = DecimalFormat(".#", DECIMAL_FORMAT_SYMBOLS)
    }
}
