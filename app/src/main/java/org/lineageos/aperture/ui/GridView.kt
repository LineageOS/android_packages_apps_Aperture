/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import androidx.camera.view.PreviewView
import androidx.core.view.children
import org.lineageos.aperture.models.GridMode
import kotlin.math.roundToInt

/**
 * A simple view that shows a grid
 */
class GridView(context: Context?, attributeSet: AttributeSet?) : View(context, attributeSet) {
    private val paint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 2F
        style = Paint.Style.STROKE
        color = 0x7FFFFFFF
    }

    var mode: GridMode = GridMode.OFF
        set(value) {
            field = value
            invalidate()
        }
    var previewView: PreviewView? = null
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val previewView = previewView ?: return

        val size = when (mode) {
            GridMode.OFF -> 0
            GridMode.ON_3 -> 3
            GridMode.ON_4 -> 4
            GridMode.ON_GOLDEN_RATIO -> 3
        }

        if (size <= 0) {
            return
        }

        val surface = previewView.children.firstOrNull {
            it is TextureView || it is SurfaceView
        } ?: throw Exception("Unable to get preview image surface!")

        val width = (surface.scaleX * surface.width).roundToInt().coerceIn(0, previewView.width)
        val height = (surface.scaleY * surface.height).roundToInt().coerceIn(0, previewView.height)

        val wOffset = (this.width - width) / 2F
        val hOffset = (this.height - height) / 2F

        val unitDiv = if (mode == GridMode.ON_GOLDEN_RATIO) GOLDEN_RATIO_UNIT else size.toFloat()

        val widthSection = width / unitDiv
        val heightSection = height / unitDiv

        for (i in size - 1 downTo 1) {
            val position =
                if (mode == GridMode.ON_GOLDEN_RATIO && i == 2) 1 + GOLDEN_RATIO
                else i.toFloat()

            canvas.drawLine(
                (widthSection * position) + wOffset, hOffset,
                (widthSection * position) + wOffset, height + hOffset,
                paint
            )
            canvas.drawLine(
                wOffset, (heightSection * position) + hOffset,
                width + wOffset, (heightSection * position) + hOffset,
                paint
            )
        }
    }

    companion object {
        private const val GOLDEN_RATIO = 0.618f
        private const val GOLDEN_RATIO_UNIT = 2 + GOLDEN_RATIO
    }
}
