/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

import android.graphics.Matrix

data class Transform(val rotation: Rotation, val mirror: Boolean) {
    fun toMatrix() = Matrix().apply {
        if (mirror) {
            postScale(-1f, 1f)
        }

        postRotate(rotation.offset.toFloat())
    }

    companion object {
        val DEFAULT = Transform(Rotation.ROTATION_0, false)
    }
}
