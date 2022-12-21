/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.camera.view.PreviewView
import org.lineageos.aperture.fastBlur

/**
 * Display a blurred viewfinder snapshot during camera rebind.
 */
class PreviewBlurView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    var previewView: PreviewView? = null

    fun freeze() {
        previewView?.takeUnless { it.height <= 0 || it.width <= 0 }?.bitmap?.fastBlur(25)?.also {
            setImageBitmap(it)
        } ?: setImageResource(android.R.color.black)
    }
}
