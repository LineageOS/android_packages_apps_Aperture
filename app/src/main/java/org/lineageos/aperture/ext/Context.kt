/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.TypedValue
import android.view.Display
import android.view.OrientationEventListener
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import org.lineageos.aperture.models.Rotation

@ColorInt
fun Context.getThemeColor(@AttrRes attribute: Int) = TypedValue().let {
    theme.resolveAttribute(attribute, it, true)
    it.data
}

val Context.displayExt
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display
    } else {
        getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
    }

/**
 * @see OrientationEventListener.onOrientationChanged
 */
fun Context.orientationFlow() = callbackFlow {
    val orientationEventListener = object : OrientationEventListener(this@orientationFlow) {
        override fun onOrientationChanged(orientation: Int) {
            trySend(orientation)
        }
    }

    orientationEventListener.enable()

    awaitClose {
        orientationEventListener.disable()
    }
}

/**
 * @see Display.getRotation
 */
fun Context.displayRotationFlow() = callbackFlow {
    val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            // Do nothing
        }

        override fun onDisplayRemoved(displayId: Int) {
            // Do nothing
        }

        override fun onDisplayChanged(displayId: Int) {
            displayExt?.takeIf { it.displayId == displayId }?.let {
                trySend(it.rotation)
            }
        }
    }

    val displayManager = getSystemService(DisplayManager::class.java)

    displayManager.registerDisplayListener(displayListener, null)

    awaitClose {
        displayManager.unregisterDisplayListener(displayListener)
    }
}

/**
 * The orientation of the display relative to the current display rotation.
 * @see orientationFlow
 * @see displayRotationFlow
 */
fun Context.relativeRotationFlow() = combine(
    orientationFlow(), displayRotationFlow()
) { orientation, displayRotation ->
    orientation.takeIf {
        it != OrientationEventListener.ORIENTATION_UNKNOWN
    }?.let {
        Rotation.fromDegreesInAperture(it).offset - Rotation.fromSurfaceRotation(
            displayRotation
        ).compensationValue
    } ?: 0
}
