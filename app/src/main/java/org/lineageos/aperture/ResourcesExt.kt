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
        runCatching {
            Pair(it, context.packageManager.getResourcesForApplication(it))
        }.getOrNull()
    }

@SuppressLint("DiscouragedApi")
private fun Resources.getIdentifier(context: Context, id: Int, packageName: String) =
    getIdentifier(
        context.resources.getResourceEntryName(id),
        context.resources.getResourceTypeName(id),
        packageName
    )

internal fun Resources.getBoolean(context: Context, id: Int): Boolean {
    getResources(context).forEach { (packageName, res) ->
        runCatching {
            return res.getBoolean(res.getIdentifier(context, id, packageName))
        }
    }
    return getBoolean(id)
}

internal fun Resources.getStringArray(context: Context, id: Int): Array<String> {
    getResources(context).forEach { (packageName, res) ->
        runCatching {
            return res.getStringArray(res.getIdentifier(context, id, packageName))
        }
    }
    return getStringArray(id)
}
