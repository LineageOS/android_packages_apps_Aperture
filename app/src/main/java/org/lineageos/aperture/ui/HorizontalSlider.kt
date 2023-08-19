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

        val cx = if (steps > 0) {
            val progress = Int.mapToRange(Range(0, steps), progress).toFloat() / steps
            (trackWidth * progress) + track.left
        } else {
            (trackWidth * progress) + track.left
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
                progress = event.x.coerceIn(0f, width.toFloat()) / width
                onProgressChangedByUser?.invoke(progress)
            }
        }

        return true
    }
}
