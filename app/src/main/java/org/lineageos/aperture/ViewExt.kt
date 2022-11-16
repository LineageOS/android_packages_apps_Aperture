/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import androidx.core.view.isVisible

internal fun View.setPadding(value: Int) {
    setPadding(value, value, value, value)
}

internal fun View.slideUp() {
    if (isVisible) {
        return
    }

    isVisible = true
    measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
    val translateAnimation = TranslateAnimation(0f, 0f, measuredHeight.toFloat(), 0f).apply {
        duration = 250
    }
    val alphaAnimation = AlphaAnimation(0.0f, 1.0f).apply {
        duration = 250
    }
    val animationSet = AnimationSet(true).apply {
        addAnimation(translateAnimation)
        addAnimation(alphaAnimation)
    }
    startAnimation(animationSet)
}

internal fun View.slideDown() {
    if (!isVisible) {
        return
    }

    isVisible = false
    val translateAnimation = TranslateAnimation(0f, 0f, 0f, height.toFloat()).apply {
        duration = 200
    }
    val alphaAnimation = AlphaAnimation(1.0f, 0.0f).apply {
        duration = 200
    }
    val animationSet = AnimationSet(true).apply {
        addAnimation(translateAnimation)
        addAnimation(alphaAnimation)
    }
    startAnimation(animationSet)
}
