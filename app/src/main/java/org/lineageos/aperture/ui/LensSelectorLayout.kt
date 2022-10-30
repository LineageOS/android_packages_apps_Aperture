/*
 * Copyright (C) 2022 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui

import android.content.Context
import android.icu.text.DecimalFormat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.setMargins
import org.lineageos.aperture.R
import org.lineageos.aperture.px
import org.lineageos.aperture.utils.Camera

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class LensSelectorLayout(context: Context, attrs: AttributeSet?) : LinearLayoutCompat(
    context, attrs
) {
    private lateinit var activeCamera: Camera

    private val buttonToCamera = mutableMapOf<Button, Camera>()
    private val buttonToFocalLength = mutableMapOf<Button, Float>()

    private val numLenses: Int
        get() = if (usesFocalLength) buttonToFocalLength.size else buttonToCamera.size

    private var usesFocalLength = false
    private var currentFocalLength = 0f

    var onCameraChangeCallback: (camera: Camera) -> Unit = {}
    var onFocalLengthChangeCallback: (focalLength: Float) -> Unit = {}

    override fun setVisibility(visibility: Int) {
        super.setVisibility(if (numLenses < 2) View.GONE else visibility)
    }

    fun setCamera(activeCamera: Camera, availableCameras: Collection<Camera>) {
        this.activeCamera = activeCamera

        removeAllViews()
        buttonToCamera.clear()
        buttonToFocalLength.clear()

        usesFocalLength = activeCamera.isLogical && availableCameras.size == 1

        if (usesFocalLength) {
            val mainMm35FocalLength = activeCamera.mm35FocalLengths!![0]
            val sensorSize = activeCamera.sensorSize!!
            val zoomRatioToFocalLength = activeCamera.focalLengths.associateBy {
                Camera.getMm35FocalLength(it, sensorSize) / mainMm35FocalLength
            }
            for ((zoomRatio, focalLength) in zoomRatioToFocalLength.toSortedMap()) {
                val button = getDefaultButton().apply {
                    setOnClickListener {
                        buttonToFocalLength[it]?.let { focalLength ->
                            onFocalLengthChangeCallback(focalLength)
                            currentFocalLength = focalLength
                        }
                    }
                    text = ZOOM_RATIO_FORMATTER.format(zoomRatio)
                }

                addView(button)
                buttonToFocalLength[button] = focalLength
            }
            currentFocalLength = buttonToFocalLength.values.first()
        } else {
            for (camera in availableCameras.sortedBy { it.zoomRatio }) {
                val button = getDefaultButton().apply {
                    setOnClickListener {
                        buttonToCamera[it]?.let(onCameraChangeCallback)
                    }
                    text = ZOOM_RATIO_FORMATTER.format(camera.zoomRatio)
                }

                addView(button)
                buttonToCamera[button] = camera
            }
        }

        updateActiveButton()
    }

    private fun updateActiveButton() {
        if (usesFocalLength) {
            for ((button, focalLength) in buttonToFocalLength) {
                button.isEnabled = focalLength != currentFocalLength
            }
        } else {
            for ((button, camera) in buttonToCamera) {
                button.isEnabled = camera != activeCamera
            }
        }
    }

    private fun getDefaultButton(): Button {
        val button = LayoutInflater.from(context).inflate(
            R.layout.lens_selector_button, null
        ) as Button
        return button.apply {
            layoutParams = LayoutParams(32.px, 32.px).apply {
                setMargins(5)
            }
        }
    }

    companion object {
        private val ZOOM_RATIO_FORMATTER = DecimalFormat("0.#×")
    }
}
