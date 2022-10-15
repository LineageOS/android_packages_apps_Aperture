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
    var onLensChangeCallback: (camera: Camera) -> Unit = {}

    fun setCamera(activeCamera: Camera, availableCameras: Collection<Camera>) {
        this.activeCamera = activeCamera

        removeAllViews()
        buttonToCamera.clear()

        for (camera in availableCameras.sortedBy { it.zoomRatio }) {
            val button = Button(context).apply {
                background = ContextCompat.getDrawable(
                    context, R.drawable.lens_selector_button_background
                )
                isSingleLine = true
                isAllCaps = false
                setOnClickListener {
                    buttonToCamera[it]?.let(onLensChangeCallback)
                }
                text = "%.1fx".format(camera.zoomRatio)
                setTextColor(
                    ContextCompat.getColorStateList(context, R.color.lens_selector_button_text)
                )
                textSize = TEXT_SIZE
                layoutParams = LayoutParams(BUTTON_SIZE, BUTTON_SIZE).apply {
                    setMargins(5)
                }
            }

            addView(button)
            buttonToCamera[button] = camera
        }

        updateActiveButton()
    }

    private fun updateActiveButton() {
        for ((button, camera) in buttonToCamera) {
            button.isEnabled = camera != activeCamera
        }
    }

    companion object {
        const val BUTTON_SIZE = 96
        const val TEXT_SIZE = 10f
    }
}
