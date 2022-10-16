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
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import org.lineageos.aperture.px

class VerticalSlider @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.rgb(0xCC, 0xCC, 0xCC)
        setShadowLayer(1f, 0f, 0f, Color.BLACK)
    }

    private val thumbPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        setShadowLayer(3f, 0f, 0f, Color.BLACK)
    }

    private val thumbTextPaint = Paint().apply {
        color = Color.BLACK
        textSize = 10.px.toFloat()
    }

    var progress = 0.5f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }
    var onProgressChangedByUser: ((value: Float) -> Unit)? = null

    var textFormatter: (value: Float) -> String = {
        "%.01f".format(it)
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

    private fun drawTrack(canvas: Canvas) {
        val trackWidth = width / 5
        val trackRadius = trackWidth * 0.75f

        val left = (width - trackWidth) / 2f
        val right = left + trackWidth

        val top = width / 2f
        val bottom = height - top

        // Draw round rect
        canvas.drawRoundRect(left, top, right, bottom, trackRadius, trackRadius, trackPaint)
    }

    private fun drawThumb(canvas: Canvas) {
        val verticalOffset = width / 2f
        val trackHeight = height - verticalOffset * 2

        // Draw circle
        val cy = (trackHeight - (trackHeight * progress)) + verticalOffset
        canvas.drawCircle(width / 2f, cy, width / 2.15f, thumbPaint)

        // Draw text
        val text = textFormatter(progress)
        val textBounds = Rect().apply {
            thumbTextPaint.getTextBounds(text, 0, text.length, this)
        }
        canvas.drawText(
            text,
            (width - textBounds.width()) / 2f,
            cy + (textBounds.height() / 2),
            thumbTextPaint
        )
    }

}
