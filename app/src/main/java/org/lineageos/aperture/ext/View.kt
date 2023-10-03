/*
 * SPDX-FileCopyrightText: 2022-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import androidx.core.view.isVisible
import org.lineageos.aperture.models.Rotation

internal fun View.setPadding(value: Int) {
    setPadding(value, value, value, value)
}

internal fun View.slide() {
    if (isVisible) {
        slideDown()
    } else {
        slideUp()
    }
}

internal fun View.slideUp() {
    if (isVisible) {
        return
    }

    isVisible = true

    measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
    startAnimation(AnimationSet(true).apply {
        addAnimation(TranslateAnimation(0f, 0f, measuredHeight.toFloat(), 0f).apply {
            duration = 250
        })
        addAnimation(AlphaAnimation(0.0f, 1.0f).apply {
            duration = 250
        })
    })
}

internal fun View.slideDown() {
    if (!isVisible) {
        return
    }

    isVisible = false

    startAnimation(AnimationSet(true).apply {
        addAnimation(TranslateAnimation(0f, 0f, 0f, height.toFloat()).apply {
            duration = 200
        })
        addAnimation(AlphaAnimation(1.0f, 0.0f).apply {
            duration = 200
        })
    })
}

internal fun View.smoothRotate(rotation: Float) {
    with(animate()) {
        cancel()
        rotationBy(Rotation.getDifference(this@smoothRotate.rotation, rotation))
            .interpolator = AccelerateDecelerateInterpolator()
    }
}
