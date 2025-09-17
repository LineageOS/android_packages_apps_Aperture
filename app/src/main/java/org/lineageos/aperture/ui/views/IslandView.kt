/*
 * SPDX-FileCopyrightText: 2023-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import org.lineageos.aperture.R
import org.lineageos.aperture.ext.getThemeColor
import org.lineageos.aperture.models.IslandItem
import org.lineageos.aperture.models.Rotation
import org.lineageos.aperture.ui.recyclerview.SimpleListAdapter
import org.lineageos.aperture.ui.recyclerview.UniqueItemDiffCallback
import java.util.concurrent.atomic.AtomicBoolean

class IslandView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    // Views
    private val itemsRecyclerView by lazy { findViewById<RecyclerView>(R.id.itemsRecyclerView) }

    // System services
    private val layoutInflater = context.getSystemService(LayoutInflater::class.java)

    private val viewPropertyAnimator by lazy { animate() }

    private val shortAnimationDuration by lazy {
        resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
    }

    private var screenRotation = Rotation.ROTATION_0

    private val lastVisibilityState = AtomicBoolean(false)

    // RecyclerView
    private val adapter by lazy {
        object : SimpleListAdapter<IslandItem, ImageView>(
            UniqueItemDiffCallback(),
            {
                ImageView(
                    it.context,
                    null,
                    0,
                    R.style.Theme_Aperture_Camera_Island_ImageView,
                )
            },
        ) {
            override fun ViewHolder.onBindView(item: IslandItem) {
                view.setImageResource(
                    when (item) {
                        is IslandItem.ThermalThrottling -> R.drawable.ic_thermostat

                        is IslandItem.LowBattery -> when (item.isCharging) {
                            true -> R.drawable.ic_battery_charging_20
                            false -> R.drawable.ic_battery_1_bar
                        }

                        is IslandItem.PhotoJpegUltraHdr -> R.drawable.ic_hdr_on

                        is IslandItem.PhotoRawEnabled -> when (item.withJpeg) {
                            true -> R.drawable.ic_image_add_raw_on
                            false -> R.drawable.ic_raw_on
                        }

                        is IslandItem.VideoMicMuted -> R.drawable.ic_mic_off
                    }
                )

                val isWarning = when (item) {
                    is IslandItem.ThermalThrottling -> item.isCritical
                    is IslandItem.LowBattery -> true
                    else -> false
                }

                view.imageTintList = ColorStateList.valueOf(
                    context.getThemeColor(
                        when (isWarning) {
                            true -> com.google.android.material.R.attr.colorError
                            false -> com.google.android.material.R.attr.colorOnSurface
                        }
                    )
                )
            }
        }
    }

    init {
        layoutInflater.inflate(R.layout.island_view, this)

        itemsRecyclerView.adapter = adapter
    }

    fun setItems(items: List<IslandItem>) {
        val shouldBeVisible = items.isNotEmpty()

        val postUpdate = {
            adapter.submitList(items)

            setScreenRotation(screenRotation)
        }

        if (lastVisibilityState.compareAndSet(!shouldBeVisible, shouldBeVisible)) {
            if (shouldBeVisible) {
                postUpdate()

                // Set the content view to 0% opacity but visible, so that it is visible
                // (but fully transparent) during the animation.
                alpha = 0f
                isVisible = true
            }

            viewPropertyAnimator.apply {
                cancel()

                alpha(
                    when (shouldBeVisible) {
                        true -> 1f
                        false -> 0f
                    }
                )
                setDuration(shortAnimationDuration)
                setListener(
                    object : AnimatorListenerAdapter() {
                        var animationCanceled = false

                        override fun onAnimationCancel(animation: Animator) {
                            super.onAnimationCancel(animation)

                            animationCanceled = true
                        }

                        override fun onAnimationEnd(animation: Animator) {
                            if (!shouldBeVisible && !animationCanceled) {
                                isVisible = false

                                postUpdate()
                            }
                        }
                    }
                )
            }
        } else {
            postUpdate()
        }
    }

    fun setScreenRotation(screenRotation: Rotation) {
        this.screenRotation = screenRotation

        val compensationValue = screenRotation.compensationValue.toFloat()

        updateLayoutParams<ConstraintLayout.LayoutParams> {
            startToStart = when (screenRotation) {
                Rotation.ROTATION_0,
                Rotation.ROTATION_90,
                Rotation.ROTATION_180 -> R.id.viewFinder

                Rotation.ROTATION_270 -> ConstraintLayout.LayoutParams.UNSET
            }
            endToEnd = when (screenRotation) {
                Rotation.ROTATION_0,
                Rotation.ROTATION_90,
                Rotation.ROTATION_180 -> ConstraintLayout.LayoutParams.UNSET

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
