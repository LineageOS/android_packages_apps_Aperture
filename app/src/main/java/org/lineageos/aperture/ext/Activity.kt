/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.app.Activity
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.lineageos.aperture.models.Rotation

private val Activity.currentDisplay
    get() = ContextCompat.getDisplayOrDefault(this)

private val Activity.displayRotation
    get() = Rotation.fromSurfaceRotation(currentDisplay.rotation)

fun Activity.displayRotationFlow() = callbackFlow {
    val displayManager = getSystemService(DisplayManager::class.java)
    val sendRotation = { trySend(displayRotation) }

    val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == currentDisplay.displayId) {
                sendRotation()
            }
        }
    }

    sendRotation()
    displayManager.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))

    awaitClose {
        displayManager.unregisterDisplayListener(displayListener)
    }
}.distinctUntilChanged()
