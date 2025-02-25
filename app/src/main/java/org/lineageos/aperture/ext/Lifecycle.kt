/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.eventFlow
import kotlinx.coroutines.flow.mapNotNull

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
