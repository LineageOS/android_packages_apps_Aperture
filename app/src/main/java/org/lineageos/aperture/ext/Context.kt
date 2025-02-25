/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

@ColorInt
fun Context.getThemeColor(@AttrRes attribute: Int) = TypedValue().let {
    theme.resolveAttribute(attribute, it, true)
    it.data
}

fun Context.permissionGranted(
    permission: String
) = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun Context.permissionsGranted(permissions: Array<String>) = permissions.all {
    permissionGranted(it)
}

fun Context.permissionsGrantedFlow(
    lifecycle: Lifecycle,
    permissions: Array<String>,
) = lifecycle.eventFlow(Lifecycle.Event.ON_RESUME)
    .onStart { emit(Unit) }
    .map { permissionsGranted(permissions) }

fun Context.broadcastReceiverFlow(
    intentFilter: IntentFilter,
    flags: Int = ContextCompat.RECEIVER_NOT_EXPORTED,
) = callbackFlow {
    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val matched = intentFilter.match(
                contentResolver,
                intent,
                true,
                Context::class.simpleName!!
            )

            if (matched >= 0) {
                trySend(intent)
            }
        }
    }

    trySend(
        ContextCompat.registerReceiver(
            this@broadcastReceiverFlow,
            broadcastReceiver,
            intentFilter,
            flags
        )
    )

    awaitClose {
        unregisterReceiver(broadcastReceiver)
    }
}
