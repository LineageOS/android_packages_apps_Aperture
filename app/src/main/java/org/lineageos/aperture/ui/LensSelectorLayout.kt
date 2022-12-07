/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui

import android.annotation.SuppressLint
import android.content.Context
import android.icu.text.DecimalFormat
import android.icu.text.DecimalFormatSymbols
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.setMargins
import org.lineageos.aperture.R
import org.lineageos.aperture.px
import org.lineageos.aperture.utils.Camera
import java.util.Locale

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class LensSelectorLayout(context: Context, attrs: AttributeSet?) : LinearLayoutCompat(
    context, attrs
) {
    private val layoutInflater by lazy { LayoutInflater.from(context) }

    private lateinit var activeCamera: Camera

    private val buttonToZoomRatio = mutableMapOf<Button, Float>()

    private val buttonToCamera = mutableMapOf<Button, Camera>()

    var onCameraChangeCallback: (camera: Camera) -> Unit = {}

    fun setCamera(activeCamera: Camera, availableCameras: Collection<Camera>) {
        this.activeCamera = activeCamera

        removeAllViews()
        buttonToZoomRatio.clear()

        buttonToCamera.clear()

        for (camera in availableCameras.sortedBy { it.zoomRatio }) {
            val button = inflateButton().apply {
                setOnClickListener {
                    buttonToCamera[it]?.let(onCameraChangeCallback)
                }
                text = formatZoomRatio(camera.zoomRatio)
            }

            addView(button)
            buttonToCamera[button] = camera
            buttonToZoomRatio[button] = camera.zoomRatio
        }

        updateButtonsAttributes()
    }

    @SuppressLint("InflateParams")
    private fun inflateButton(): Button {
        val button = layoutInflater.inflate(R.layout.lens_selector_button, null) as Button
        return button.apply {
            layoutParams = LayoutParams(32.px, 32.px).apply {
                setMargins(5)
            }
        }
    }

    private fun updateButtonsAttributes() {
        for ((button, camera) in buttonToCamera) {
            updateButtonAttributes(button, camera == activeCamera)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateButtonAttributes(button: Button, currentCamera: Boolean) {
        button.isEnabled = !currentCamera
        val formattedZoomRatio = formatZoomRatio(buttonToZoomRatio[button]!!)
        button.text = if (currentCamera) {
            "${formattedZoomRatio}×"
        } else {
            formattedZoomRatio
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
