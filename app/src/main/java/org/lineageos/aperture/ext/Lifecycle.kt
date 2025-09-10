/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.eventFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.mapNotNull

/**
 * Emit a [Unit] only when the lifecycle moves to the requested state from a lower one.
 * Will also only emit if it goes at least once from the specified state to a lower one.
 *
 * @see Lifecycle.eventFlow
 */
fun Lifecycle.emitOnState(state: Lifecycle.State) = channelFlow {
    val reachStateEvent = Lifecycle.Event.upTo(state)
    val leaveStateEvent = Lifecycle.Event.downFrom(state)

    var shouldEmit = false

    eventFlow.collect {
        when (it) {
            reachStateEvent -> if (shouldEmit) {
                send(Unit)
            }

            leaveStateEvent -> shouldEmit = true

            else -> {}
        }
    }
}

/**
 * Emit a [Unit] only when the lifecycle reaches the requested event
 * @see Lifecycle.eventFlow
 */
fun Lifecycle.eventFlow(event: Lifecycle.Event) = eventFlow
    .mapNotNull {
        when (it) {
            event -> Unit
            else -> null
        }
    }
