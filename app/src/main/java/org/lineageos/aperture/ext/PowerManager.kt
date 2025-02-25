/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

@RequiresApi(Build.VERSION_CODES.Q)
fun PowerManager.thermalStatusFlow() = callbackFlow {
    val listener = PowerManager.OnThermalStatusChangedListener {
        trySend(it)
    }

    addThermalStatusListener(listener)

    awaitClose {
        removeThermalStatusListener(listener)
    }
}
