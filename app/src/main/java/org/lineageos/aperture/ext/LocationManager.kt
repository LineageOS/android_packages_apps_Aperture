/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.Manifest
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.RequiresPermission
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

@RequiresPermission(
    anyOf = [
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ]
)
fun LocationManager.locationFlow(
    locationRequest: LocationRequestCompat,
) = callbackFlow {
    var currentLocation: Location? = null

    val listener = LocationListenerCompat { location ->
        currentLocation?.let {
            val currentLocationAgeSeconds = SystemClock.elapsedRealtimeNanos()
                .minus(it.elapsedRealtimeNanos)

            if (currentLocationAgeSeconds > 60_000_000_000) { // 60 seconds
                currentLocation = null
            }
        }

        val isMoreAccurate = currentLocation?.let {
            it.accuracy < location.accuracy
        } ?: true

        if (!isMoreAccurate) {
            return@LocationListenerCompat
        }

        currentLocation = location
        trySend(location)
    }

    for (provider in allProviders) {
        LocationManagerCompat.requestLocationUpdates(
            this@locationFlow,
            provider,
            locationRequest,
            listener,
            Looper.getMainLooper()
        )
    }

    awaitClose {
        LocationManagerCompat.removeUpdates(this@locationFlow, listener)
    }
}
