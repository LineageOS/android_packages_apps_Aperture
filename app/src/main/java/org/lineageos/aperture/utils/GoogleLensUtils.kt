/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.content.Context
import android.content.Intent

object GoogleLensUtils {
    private const val PACKAGE_NAME = "com.google.ar.lens"

    fun isGoogleLensAvailable(context: Context): Boolean {
        return runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getApplicationInfo(PACKAGE_NAME, 0).enabled
        }.getOrDefault(false)
    }

    fun launchGoogleLens(context: Context) {
        assert(isGoogleLensAvailable(context))
        context.startActivity(
            context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)!!
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
