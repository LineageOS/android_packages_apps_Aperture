/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui.views

import android.content.Context
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import org.lineageos.aperture.ext.mapToRange

class HorizontalSlider @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : Slider(context, attrs) {
    override fun track(): RectF {
        val trackHeight = height / 5

        val left = height / 2f
        val right = width - left

        val top = (height - trackHeight) / 2f
        val bottom = height - top

        return RectF(left, top, right, bottom)
    }

    override fun thumb(): Triple<Float, Float, Float> {
        val track = track()
        val trackWidth = track.width()

        val value = (value - valueFrom) / (valueTo - valueFrom)

        val cx = if (stepSize > 0) {
            val progress = Float.mapToRange(0f..stepSize, value) / stepSize
            (trackWidth * progress) + track.left
        } else {
            (trackWidth * value) + track.left
        }
        val cy = height / 2f

        return Triple(cx, cy, height / 2.15f)
    }

    @Suppress("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        super.onTouchEvent(event)

        if (!isEnabled) {
            return false
        }

        when (event?.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE,
            MotionEvent.ACTION_UP -> {
                value = Float.mapToRange(
                    valueFrom..valueTo,
                    event.x.coerceIn(0f, width.toFloat()) / width,
                )
                onProgressChangedByUser?.invoke(value)
            }
        }

        return true
    }
}
