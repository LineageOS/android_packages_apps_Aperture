/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import androidx.camera.view.PreviewView
import androidx.core.view.children
import com.google.zxing.LuminanceSource
import com.google.zxing.ResultPoint
import org.lineageos.aperture.R
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
            pts.add((point.x * width) + wOffset)
            pts.add((point.y * height) + hOffset)
        }

        canvas?.drawPoints(pts.toFloatArray(), paint)
    }

    fun scalePoints(points: Array<ResultPoint>, image: LuminanceSource, rotation: Int) =
        points.map { point ->
            if (rotation == 90 || rotation == 270) {
                PointF(
                    (image.width - point.x) / image.width,
                    point.y / image.height,
                )
            } else {
                PointF(
                    (image.height - point.y) / image.height,
                    (image.width - point.x) / image.width,
                )
            }
        }.toTypedArray()
}
