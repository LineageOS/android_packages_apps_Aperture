/*
 * SPDX-FileCopyrightText: 2023-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.content.Context
import android.content.Intent

object GoogleLensUtils {
    private const val GSA_PACKAGE_NAME = "com.google.android.googlequicksearchbox"
    private const val LAUNCHER_PACKAGE_NAME = "com.google.android.apps.googlecamera.fishfood"

    private fun isGsaAvailable(context: Context) = runCatching {
        context.packageManager.getApplicationInfo(GSA_PACKAGE_NAME, 0).enabled
    }.getOrDefault(false)

    private fun isLensLauncherAvailable(context: Context) = runCatching {
        context.packageManager.getApplicationInfo(LAUNCHER_PACKAGE_NAME, 0).enabled
    }.getOrDefault(false)

    fun isGoogleLensAvailable(context: Context) =
        isGsaAvailable(context) && isLensLauncherAvailable(context)

    fun launchGoogleLens(context: Context) {
        context.startActivity(
            Intent().setClassName(LAUNCHER_PACKAGE_NAME, "$LAUNCHER_PACKAGE_NAME.MainActivity")
        )
    }
}
