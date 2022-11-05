/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.content.Context
import android.content.res.Resources

private fun getResources(context: Context) =
    listOf(
        "org.lineageos.aperture.auto_generated_rro_product__",
        "org.lineageos.aperture.auto_generated_rro_vendor__",
    ).mapNotNull {
        runCatching { context.packageManager.getResourcesForApplication(it) }.getOrNull()
    }

internal fun Resources.getBoolean(context: Context, id: Int): Boolean {
    getResources(context).forEach {
        runCatching {
            return it.getBoolean(id)
        }
    }
    return getBoolean(id)
}

internal fun Resources.getStringArray(context: Context, id: Int): Array<String> {
    getResources(context).forEach {
        runCatching {
            return it.getStringArray(id)
        }
    }
    return getStringArray(id)
}
