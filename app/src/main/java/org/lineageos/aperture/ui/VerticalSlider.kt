/*
 * Copyright (C) 2022 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Range
import android.view.MotionEvent
import android.view.View
import org.lineageos.aperture.R
import org.lineageos.aperture.mapToRange
import org.lineageos.aperture.px

class VerticalSlider @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val trackPaint = Paint().apply {
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        setShadowLayer(1f, 0f, 0f, Color.BLACK)
    }

    private val thumbPaint = Paint().apply {
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        setShadowLayer(3f, 0f, 0f, Color.BLACK)
    }

    private val thumbTextPaint = Paint()

    var progress = 0.5f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }
    var onProgressChangedByUser: ((value: Float) -> Unit)? = null

    var textFormatter: (value: Float) -> String = {
        "%.01f".format(it)
    }

    var steps = 0

    init {
        context.obtainStyledAttributes(attrs, R.styleable.VerticalSlider, 0, 0).apply {
            try {
                trackPaint.color = getColor(R.styleable.VerticalSlider_trackColor, Color.WHITE)
                thumbPaint.color = getColor(R.styleable.VerticalSlider_thumbColor, Color.BLACK)
                thumbTextPaint.color =
                    getColor(R.styleable.VerticalSlider_thumbTextColor, Color.WHITE)
                thumbTextPaint.textSize =
                    getDimension(R.styleable.VerticalSlider_thumbTextSize, 10.px.toFloat())
            } finally {
                recycle()
            }
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas!!)

        drawTrack(canvas)
        drawThumb(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
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

    private fun track(): RectF {
        val trackWidth = width / 5

        val left = (width - trackWidth) / 2f
        val right = left + trackWidth

        val top = width / 2f
        val bottom = height - top

        return RectF(left, top, right, bottom)
    }

    private fun drawTrack(canvas: Canvas) {
        val track = track()
        val trackRadius = track.width() * 0.75f

        // Draw round rect
        canvas.drawRoundRect(track, trackRadius, trackRadius, trackPaint)
    }

    private fun thumb(): Triple<Float, Float, Float> {
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

    private fun drawThumb(canvas: Canvas) {
        // Draw circle
        val thumb = thumb()
        canvas.drawCircle(thumb.first, thumb.second, thumb.third, thumbPaint)

        // Draw text
        val text = textFormatter(progress)
        val textBounds = Rect().apply {
            thumbTextPaint.getTextBounds(text, 0, text.length, this)
        }
        canvas.drawText(
            text,
            thumb.first - (textBounds.width() / 2),
            thumb.second + (textBounds.height() / 2),
            thumbTextPaint
        )
    }
}
