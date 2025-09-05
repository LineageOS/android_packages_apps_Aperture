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

class VerticalSlider @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : Slider(context, attrs, defStyleAttr) {
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
                    (height - event.y.coerceIn(0f, height.toFloat())) / height,
                )
                onProgressChangedByUser?.invoke(value)
            }
        }

        return true
    }

    override fun track(): RectF {
        val trackWidth = width / 5

        val left = (width - trackWidth) / 2f
        val right = left + trackWidth

        val top = width / 2f
        val bottom = height - top

        return RectF(left, top, right, bottom)
    }

    override fun thumb(): Triple<Float, Float, Float> {
        val track = track()
        val trackHeight = track.height()

        val value = (value - valueFrom) / (valueTo - valueFrom)

        val cx = width / 2f
        val cy = if (stepSize > 0) {
            val progress = Float.mapToRange(0f..stepSize, value) / stepSize
            (trackHeight - (trackHeight * progress)) + track.top
        } else {
            (trackHeight - (trackHeight * value)) + track.top
        }

        return Triple(cx, cy, width / 2.15f)
    }
}
