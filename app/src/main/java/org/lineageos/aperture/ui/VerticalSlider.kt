/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui

import android.content.Context
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Range
import android.view.MotionEvent
import org.lineageos.aperture.ext.*

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
                progress = (height - event.y.coerceIn(0f, height.toFloat())) / height
                onProgressChangedByUser?.invoke(progress)
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

        val cx = width / 2f
        val cy = if (steps > 0) {
            val progress = Int.mapToRange(Range(0, steps), progress).toFloat() / steps
            (trackHeight - (trackHeight * progress)) + track.top
        } else {
            (trackHeight - (trackHeight * progress)) + track.top
        }

        return Triple(cx, cy, width / 2.15f)
    }
}
