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
        return context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME) != null
    }

    fun launchGoogleLens(context: Context) {
        assert(isGoogleLensAvailable(context))
        context.startActivity(
            context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)!!
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
