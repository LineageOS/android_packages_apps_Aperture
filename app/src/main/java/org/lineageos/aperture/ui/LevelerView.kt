/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.OrientationEventListener
import android.view.OrientationEventListener.ORIENTATION_UNKNOWN
import android.view.View
import org.lineageos.aperture.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

class LevelerView(context: Context, attributeSet: AttributeSet?) : View(context, attributeSet) {
    private var currentOrientation = ORIENTATION_UNKNOWN
    private val orientationEventListener = runCatching {
        object : OrientationEventListener(context, SensorManager.SENSOR_DELAY_UI) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    return
                }

                currentOrientation = orientation
                postInvalidate()
            }
        }
    }.getOrNull()

    private val defaultLevelPaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 4f
        style = Paint.Style.STROKE
        color = 0x7FFFFFFF
    }

    private val defaultBasePaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 4f
        style = Paint.Style.STROKE
        color = 0x7FFFFFFF
    }

    private val highlightPaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 4f
        style = Paint.Style.STROKE
        color = context.getColor(R.color.yellow)
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)

        if (visibility == VISIBLE) {
            orientationEventListener?.enable()
        } else {
            orientationEventListener?.disable()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        currentOrientation.takeUnless { it == ORIENTATION_UNKNOWN }?.let {
            val isLevel = isLevel(it)
            drawBase(canvas, isLevel, isLandscape(it))

            if (!isLevel) {
                val radians = -((it.toFloat() / 180F) * PI.toFloat())
                drawLevelLine(canvas, radians)
            }
        }
    }

    private fun drawBase(canvas: Canvas, isLevel: Boolean, isLandscape: Boolean) {
        val xLen = if (isLandscape) RELATIVE_BASE_LENGTH_Y else RELATIVE_BASE_LENGTH_X
        val yLen = if (isLandscape) RELATIVE_BASE_LENGTH_X else RELATIVE_BASE_LENGTH_Y

        val xLength = (min(width, height) * xLen)
        val yLength = (min(width, height) * yLen)

        val wCenter = width / 2
        val hCenter = height / 2

        val paint = if (isLevel) highlightPaint else defaultBasePaint

        canvas.drawLine(
            (wCenter - xLength).toFloat(),
            hCenter.toFloat(),
            (wCenter + xLength).toFloat(),
            hCenter.toFloat(),
            paint
        )
        canvas.drawLine(
            wCenter.toFloat(),
            (hCenter - yLength).toFloat(),
            wCenter.toFloat(),
            (hCenter + yLength).toFloat(),
            paint
        )
    }

    private fun drawLevelLine(canvas: Canvas, radians: Float) {
        val length = (min(width, height) * RELATIVE_LINE_LENGTH).roundToInt()

        val wCenter = width / 2
        val hCenter = height / 2
        val wOffset = cos(radians) * length
        val hOffset = sin(radians) * length

        val wStart = wCenter - wOffset
        val hStart = hCenter - hOffset
        val wEnd = wCenter + wOffset
        val hEnd = hCenter + hOffset

        canvas.drawLine(wStart, hStart, wEnd, hEnd, defaultLevelPaint)
    }

    private fun isLevel(currentOrientation: Int): Boolean {
        val o = currentOrientation % 90
        return o < LEVEL_ZONE || (90 - o) < LEVEL_ZONE
    }

    private fun isLandscape(currentOrientation: Int): Boolean {
        return currentOrientation in 45..134 || currentOrientation in 225..314
    }

    companion object {
        private const val LEVEL_ZONE = 2
        private const val RELATIVE_LINE_LENGTH = 0.15
        private const val RELATIVE_BASE_LENGTH_X = 0.15
        private const val RELATIVE_BASE_LENGTH_Y = 0.01
    }
}