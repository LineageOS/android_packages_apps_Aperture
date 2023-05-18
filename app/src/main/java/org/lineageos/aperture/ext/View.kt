/*
 * SPDX-FileCopyrightText: 2022-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import org.lineageos.aperture.utils.Rotation

internal fun View.setPadding(value: Int) {
    setPadding(value, value, value, value)
}

internal fun View.smoothRotate(rotation: Float) {
    with(animate()) {
        cancel()
        rotationBy(Rotation.getDifference(this@smoothRotate.rotation, rotation))
            .interpolator = AccelerateDecelerateInterpolator()
    }
}
