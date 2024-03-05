/*
 * SPDX-FileCopyrightText: 2023-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.view.Window
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private val Window.windowInsetsController
    get() = WindowInsetsControllerCompat(this, decorView)

/**
 * Update the window bars visibility.
 * @param behavior One of [WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE],
 *   [WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE]
 *   or null to not change the current behavior
 * @param systemBars Whether the system bars should be visible, or null to not
 *   change the current behavior
 * @param statusBars Whether the status bars should be visible, or null to not
 *   change the current behavior
 * @param navigationBars Whether the navigation bars should be visible, or null to not
 *   change the current behavior
 */
fun Window.updateBarsVisibility(
    behavior: Int? = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE,
    systemBars: Boolean? = null,
    statusBars: Boolean? = null,
    navigationBars: Boolean? = null,
) {
    // Configure the behavior of the hidden bars
    behavior?.let {
        windowInsetsController.systemBarsBehavior = it
    }

    // Set the system bars visibility
    systemBars?.let {
        val systemBarsType = WindowInsetsCompat.Type.systemBars()

        when (it) {
            true -> windowInsetsController.show(systemBarsType)
            false -> windowInsetsController.hide(systemBarsType)
        }
    }

    // Set the status bars visibility
    statusBars?.let {
        val statusBarsType = WindowInsetsCompat.Type.statusBars()

        when (it) {
            true -> windowInsetsController.show(statusBarsType)
            false -> windowInsetsController.hide(statusBarsType)
        }
    }

    // Set the navigation bars visibility
    navigationBars?.let {
        val navigationBarsType = WindowInsetsCompat.Type.navigationBars()

        when (it) {
            true -> windowInsetsController.show(navigationBarsType)
            false -> windowInsetsController.hide(navigationBarsType)
        }
    }
}
