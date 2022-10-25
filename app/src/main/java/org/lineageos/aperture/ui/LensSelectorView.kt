/*
 * Copyright (C) 2022 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.core.view.setMargins
import org.lineageos.aperture.R
import org.lineageos.aperture.utils.Camera

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class LensSelectorView(context: Context, attrs: AttributeSet?) : LinearLayoutCompat(
    context, attrs
) {
    private lateinit var activeCamera: Camera

    private val buttonToCamera = mutableMapOf<Button, Camera>()
    private val buttonToFocalLength = mutableMapOf<Button, Float>()

    private var usesFocalLength = false
    private var currentFocalLength = 0f

    var onCameraChangeCallback: (camera: Camera) -> Unit = {}
    var onFocalLengthChangeCallback: (focalLength: Float) -> Unit = {}

    fun setCamera(activeCamera: Camera, availableCameras: Collection<Camera>) {
        this.activeCamera = activeCamera

        removeAllViews()
        buttonToCamera.clear()
        buttonToFocalLength.clear()

        usesFocalLength = activeCamera.isLogical && availableCameras.size == 1

        if (usesFocalLength) {
            val zoomRatioToFocalLength = activeCamera.focalLengths.associateBy {
                activeCamera.getMm35FocalLength(it) / activeCamera.mm35FocalLengths.first()
            }
            for ((zoomRatio, focalLength) in zoomRatioToFocalLength.toSortedMap()) {
                val button = getDefaultButton().apply {
                    setOnClickListener {
                        buttonToFocalLength[it]?.let { focalLength ->
                            onFocalLengthChangeCallback(focalLength)
                            currentFocalLength = focalLength
                        }
                    }
                    text = "%.1fx".format(zoomRatio)
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
                    text = "%.1fx".format(camera.zoomRatio)
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
        return Button(context).apply {
            background = ContextCompat.getDrawable(
                context, R.drawable.lens_selector_button_background
            )
            isSingleLine = true
            isAllCaps = false
            setTextColor(
                ContextCompat.getColorStateList(context, R.color.lens_selector_button_text)
            )
            textSize = TEXT_SIZE
            layoutParams = LayoutParams(BUTTON_SIZE, BUTTON_SIZE).apply {
                setMargins(5)
            }
        }
    }

    companion object {
        const val BUTTON_SIZE = 96
        const val TEXT_SIZE = 10f
    }
}
