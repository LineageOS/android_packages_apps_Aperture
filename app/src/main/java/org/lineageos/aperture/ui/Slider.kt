/*
 * SPDX-FileCopyrightText: 2022-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import org.lineageos.aperture.R
import org.lineageos.aperture.ext.*
import org.lineageos.aperture.models.Rotation

@Suppress("PrivateResource")
abstract class Slider @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val trackPaint = Paint().apply {
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        setShadowLayer(1f, 0f, 0f, Color.BLACK)
    }

    private val trackBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        strokeWidth = 2F
    }

    private val thumbPaint = Paint().apply {
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        setShadowLayer(3f, 0f, 0f, Color.BLACK)
    }

    private val thumbTextPaint = Paint()

    private var gradientColors: IntArray

    var progress = 0.5f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }
    var onProgressChangedByUser: ((value: Float) -> Unit)? = null

    var textFormatter: (value: Float) -> String = {
        "%.01f".format(it)
    }

    var screenRotation = Rotation.ROTATION_0
        set(value) {
            field = value
            invalidate()
        }

    var steps = 0

    init {
        context.obtainStyledAttributes(attrs, R.styleable.Slider, 0, 0).apply {
            try {
                if (getBoolean(R.styleable.Slider_trackColorGradient, false)) {
                    val trackColorGradientStart =
                        getColor(R.styleable.Slider_trackColorGradientStart, Color.WHITE)
                    val trackColorGradientCenter =
                        getColor(R.styleable.Slider_trackColorGradientCenter, Color.WHITE)
                    val trackColorGradientEnd =
                        getColor(R.styleable.Slider_trackColorGradientEnd, Color.WHITE)
                    gradientColors = intArrayOf(
                        trackColorGradientStart,
                        trackColorGradientCenter,
                        trackColorGradientEnd
                    )
                } else {
                    gradientColors = intArrayOf()
                    trackPaint.color = getColor(R.styleable.Slider_trackColor, Color.WHITE)
                }
                thumbPaint.color = getColor(R.styleable.Slider_thumbColor, Color.BLACK)
                trackBorderPaint.color = getColor(
                    R.styleable.Slider_trackBorderColor, Color.TRANSPARENT
                )
                thumbTextPaint.color = getColor(R.styleable.Slider_thumbTextColor, Color.WHITE)
                thumbTextPaint.textSize =
                    getDimension(R.styleable.Slider_thumbTextSize, 10.px.toFloat())
            } finally {
                recycle()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawTrack(canvas)
        drawThumb(canvas)
    }

    abstract fun track(): RectF

    private fun drawTrack(canvas: Canvas) {
        val track = track()
        val trackRadius = track.width() * 0.75f

        if (gradientColors.isNotEmpty()) {
            trackPaint.shader = LinearGradient(
                track().width() / 2,
                0f,
                track().width() / 2,
                track.height(),
                gradientColors,
                null,
                Shader.TileMode.CLAMP
            )
        }

        // Draw round rect
        canvas.drawRoundRect(track, trackRadius, trackRadius, trackPaint)

        // Draw border
        canvas.drawRoundRect(track, trackRadius, trackRadius, trackBorderPaint)
    }

    abstract fun thumb(): Triple<Float, Float, Float>

    private fun drawThumb(canvas: Canvas) {
        val thumb = thumb()

        // Rotate canvas
        canvas.save()
        canvas.rotate(screenRotation.compensationValue.toFloat(), thumb.first, thumb.second)

        // Draw circle
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

        // Restore original rotation
        canvas.restore()
    }
}
