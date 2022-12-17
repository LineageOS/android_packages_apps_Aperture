/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PointF
import android.media.Image
import android.util.AttributeSet
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import androidx.camera.view.PreviewView
import androidx.core.view.children
import org.lineageos.aperture.R
import org.lineageos.aperture.next
import kotlin.math.roundToInt

class QrHighlightView(context: Context, attributeSet: AttributeSet?) : View(context, attributeSet) {
    private val paint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 10F
        style = Paint.Style.STROKE
        color = context.getColor(R.color.yellow)
    }

    var points: Array<PointF>? = null
        set(value) {
            val shouldRedraw = (value != null) || (field != null)
            field = value
            if (shouldRedraw) {
                invalidate()
            }
        }
    var previewView: PreviewView? = null
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val points = points ?: return
        val previewView = previewView ?: return

        val surface = previewView.children.firstOrNull {
            it is TextureView || it is SurfaceView
        } ?: throw Exception("Unable to get preview image surface!")

        val width = (surface.scaleX * surface.width).roundToInt().coerceIn(0, previewView.width)
        val height = (surface.scaleY * surface.height).roundToInt().coerceIn(0, previewView.height)

        val wOffset = (this.width - width) / 2F
        val hOffset = (this.height - height) / 2F

        val pts = mutableListOf<Float>()
        for (point in points) {
            val nextPoint = points.next(point)
            pts.add((point.x * height) + wOffset)
            pts.add((point.y * width) + hOffset)
            pts.add((nextPoint.x * height) + wOffset)
            pts.add((nextPoint.y * width) + hOffset)
        }

        canvas?.drawLines(pts.toFloatArray(), paint)
    }

    fun scalePoints(points: Array<Point>, image: Image) =
        points.map { point ->
            PointF(
                point.x.toFloat() / image.width,
                point.y.toFloat() / image.height
            )
        }.toTypedArray()
}
