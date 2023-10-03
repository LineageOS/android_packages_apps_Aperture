/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.lifecycle.findViewTreeLifecycleOwner
import org.lineageos.aperture.R
import org.lineageos.aperture.camera.CameraViewModel
import org.lineageos.aperture.models.CameraMode
import org.lineageos.aperture.models.Rotation
import kotlin.math.roundToInt

class InfoChipView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    // Views
    private val lowBatteryImageView by lazy { findViewById<ImageView>(R.id.lowBatteryImageView) }
    private val videoMicMutedImageView by lazy { findViewById<ImageView>(R.id.videoMicMutedImageView) }

    // System services
    private val layoutInflater = context.getSystemService(LayoutInflater::class.java)

    private val shortAnimationDuration by lazy {
        resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
    }

    private val screenRotationObserver = Observer { screenRotation: Rotation ->
        updateRotation(screenRotation)
    }

    private val cameraModeObserver = Observer { _: CameraMode ->
        update()
    }

    private val videoMicModeObserver = Observer { _: Boolean ->
        update()
    }

    internal var batteryIntent: Intent? = null
        set(value) {
            field = value

            update()
        }
    internal var cameraViewModel: CameraViewModel? = null
        set(value) {
            // Unregister
            field?.screenRotation?.removeObserver(screenRotationObserver)
            field?.cameraMode?.removeObserver(cameraModeObserver)
            field?.videoMicMode?.removeObserver(videoMicModeObserver)

            field = value

            val lifecycleOwner = findViewTreeLifecycleOwner() ?: return

            value?.screenRotation?.observe(lifecycleOwner, screenRotationObserver)
            value?.cameraMode?.observe(lifecycleOwner, cameraModeObserver)
            value?.videoMicMode?.observe(lifecycleOwner, videoMicModeObserver)
        }

    init {
        layoutInflater.inflate(R.layout.info_chip_view, this)
    }

    private fun update() {
        val cameraViewModel = cameraViewModel ?: return

        val cameraMode = cameraViewModel.cameraMode.value ?: return
        val screenRotation = cameraViewModel.screenRotation.value ?: return
        val videoMicMode = cameraViewModel.videoMicMode.value ?: return

        // Get the new visibility values
        val lowBatteryImageViewVisible = batteryIntent?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

            val batteryPercentage = ((level * 100) / scale.toFloat()).roundToInt()

            batteryPercentage <= 15
        } ?: false
        val videoMicMutedImageViewVisible = cameraMode == CameraMode.VIDEO && !videoMicMode

        val shouldBeVisible = listOf(
            lowBatteryImageViewVisible,
            videoMicMutedImageViewVisible,
        ).any { it }

        val postUpdate = {
            // Set the visibility on the views
            lowBatteryImageView.isVisible = lowBatteryImageViewVisible
            videoMicMutedImageView.isVisible = videoMicMutedImageViewVisible

            updateRotation(screenRotation)
        }

        if (shouldBeVisible && !isVisible) {
            postUpdate()

            // Set the content view to 0% opacity but visible, so that it is visible
            // (but fully transparent) during the animation.
            alpha = 0f
            isVisible = true

            // Animate the content view to 100% opacity, and clear any animation
            // listener set on the view.
            animate()
                .alpha(1f)
                .setDuration(shortAnimationDuration)
                .setListener(null)
        } else if (!shouldBeVisible && isVisible) {
            animate()
                .alpha(0f)
                .setDuration(shortAnimationDuration)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        isVisible = false

                        postUpdate()
                    }
                })
        } else {
            postUpdate()
        }
    }

    private fun updateRotation(screenRotation: Rotation) {
        val compensationValue = screenRotation.compensationValue.toFloat()

        updateLayoutParams<LayoutParams> {
            startToStart = when (screenRotation) {
                Rotation.ROTATION_0,
                Rotation.ROTATION_90,
                Rotation.ROTATION_180 -> R.id.viewFinder

                Rotation.ROTATION_270 -> LayoutParams.UNSET
            }
            endToEnd = when (screenRotation) {
                Rotation.ROTATION_0,
                Rotation.ROTATION_90,
                Rotation.ROTATION_180 -> LayoutParams.UNSET

                Rotation.ROTATION_270 -> R.id.viewFinder
            }
        }

        rotation = compensationValue

        measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)

        translationX = when (screenRotation) {
            Rotation.ROTATION_0,
            Rotation.ROTATION_180 -> 0F

            Rotation.ROTATION_90 -> -((measuredWidth - measuredHeight) / 2).toFloat()
            Rotation.ROTATION_270 -> ((measuredWidth - measuredHeight) / 2).toFloat()
        }
        translationY = when (screenRotation) {
            Rotation.ROTATION_0,
            Rotation.ROTATION_180 -> 0F

            Rotation.ROTATION_90,
            Rotation.ROTATION_270 -> -((measuredHeight - measuredWidth) / 2).toFloat()
        }
    }
}
