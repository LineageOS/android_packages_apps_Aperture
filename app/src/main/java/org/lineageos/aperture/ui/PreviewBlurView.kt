/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.camera.view.PreviewView
import com.google.android.renderscript.Toolkit
import org.lineageos.aperture.createBitmap
import org.lineageos.aperture.fastBlur

/**
 * Display a blurred viewfinder snapshot during camera rebind.
 */
class PreviewBlurView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    var previewView: PreviewView? = null

    fun freeze() {
        previewView?.let {
            if (it.height <= 0 || it.width <= 0) {
                return@let null
            }

            return@let if (false /* useToolkit */) {
                Toolkit.blur(it.createBitmap())
            } else {
                it.createBitmap().fastBlur()
            }
        }?.let {
            setImageBitmap(it)
            true
        } ?: setImageResource(android.R.color.black)
    }
}
