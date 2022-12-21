/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.camera.view.PreviewView
import org.lineageos.aperture.stackBlur

/**
 * Display a blurred viewfinder snapshot during camera rebind.
 */
class PreviewBlurView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    var previewView: PreviewView? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setRenderEffect(RenderEffect.createBlurEffect(RADIUS, RADIUS, Shader.TileMode.REPEAT))
        }
    }

    fun freeze() {
        previewView?.takeUnless { it.height <= 0 || it.width <= 0 }?.bitmap?.also {
            setImageBitmap(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    it
                } else {
                    it.stackBlur(RADIUS.toInt())
                }
            )
        } ?: setImageResource(android.R.color.black)
    }

    companion object {
        private const val RADIUS = 25f
    }
}
