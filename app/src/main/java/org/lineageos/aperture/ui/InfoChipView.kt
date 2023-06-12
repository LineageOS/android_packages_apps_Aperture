/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.findViewTreeLifecycleOwner
import org.lineageos.aperture.R
import org.lineageos.aperture.camera.CameraMode
import org.lineageos.aperture.camera.CameraViewModel
import org.lineageos.aperture.utils.Rotation
import kotlin.math.roundToInt

class InfoChipView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    // Views
    private val lowBatteryImageView by lazy { findViewById<ImageView>(R.id.lowBatteryImageView) }
    private val videoMicStatusImageView by lazy { findViewById<ImageView>(R.id.videoMicStatusImageView) }

    // System services
    private val layoutInflater = context.getSystemService(LayoutInflater::class.java)

    internal var batteryIntent: Intent? = null
        set(value) {
            field = value

            lowBatteryImageView.isVisible = value?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

                val batteryPercentage = ((level * 100) / scale.toFloat()).roundToInt()

                batteryPercentage <= 15
            } ?: false

            update()
        }
    internal var cameraViewModel: CameraViewModel? = null
        set(value) {
            val lifecycleOwner = findViewTreeLifecycleOwner() ?: return

            field?.let {
                // Unregister
                it.screenRotation.removeObservers(lifecycleOwner)
                it.cameraMode.removeObservers(lifecycleOwner)
                it.videoMicMode.removeObservers(lifecycleOwner)
            }

            field = value

            value?.let { cameraViewModel ->
                cameraViewModel.screenRotation.observe(lifecycleOwner) {
                    updateRotation()
                }
                cameraViewModel.cameraMode.observe(lifecycleOwner) {
                    val cameraMode = it ?: return@observe

                    videoMicStatusImageView.isVisible = cameraMode == CameraMode.VIDEO

                    update()
                }
                cameraViewModel.videoMicMode.observe(lifecycleOwner) {
                    val videoMicMode = it ?: return@observe

                    videoMicStatusImageView.setImageResource(
                        if (videoMicMode) R.drawable.ic_mic_on else R.drawable.ic_mic_off
                    )

                    update()
                }
            }
        }

    init {
        layoutInflater.inflate(R.layout.info_chip_view, this)
    }

    private fun update() {
        isVisible = listOf(
            lowBatteryImageView,
            videoMicStatusImageView,
        ).any { it.isVisible }

        updateRotation()
    }

    private fun updateRotation() {
        val cameraViewModel = cameraViewModel ?: return

        val screenRotation = cameraViewModel.screenRotation.value ?: return

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
