/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources

private fun getResources(context: Context) =
    listOf(
        "org.lineageos.aperture.auto_generated_rro_product__",
        "org.lineageos.aperture.auto_generated_rro_vendor__",
    ).mapNotNull {
        runCatching { context.packageManager.getResourcesForApplication(it) }.getOrNull()
    }

@SuppressLint("DiscouragedApi")
private fun Resources.getIdentifier(context: Context, id: Int) =
    getIdentifier(context.resources.getResourceName(id), null, null)

internal fun Resources.getBoolean(context: Context, id: Int): Boolean {
    getResources(context).forEach {
        runCatching {
            return it.getBoolean(it.getIdentifier(context, id))
        }
    }
    return getBoolean(id)
}

internal fun Resources.getStringArray(context: Context, id: Int): Array<String> {
    getResources(context).forEach {
        runCatching {
            return it.getStringArray(it.getIdentifier(context, id))
        }
    }
    return getStringArray(id)
}
