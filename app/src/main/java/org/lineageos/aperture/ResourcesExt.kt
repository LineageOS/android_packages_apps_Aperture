/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources

@SuppressLint("DiscouragedApi")
private fun getResources(context: Context) =
    listOf(
        "org.lineageos.aperture.auto_generated_rro_product__",
        "org.lineageos.aperture.auto_generated_rro_vendor__",
    ).mapNotNull {
        runCatching {
            val resources = context.packageManager.getResourcesForApplication(it)
            Pair(resources) { id: Int ->
                resources.getIdentifier(
                    context.resources.getResourceEntryName(id),
                    context.resources.getResourceTypeName(id),
                    it
                )
            }
        }.getOrNull()
    }

internal fun Resources.getBoolean(context: Context, id: Int): Boolean {
    getResources(context).forEach { (resources, resourceId) ->
        runCatching {
            return resources.getBoolean(resourceId(id))
        }
    }
    return getBoolean(id)
}

internal fun Resources.getStringArray(context: Context, id: Int): Array<String> {
    getResources(context).forEach { (resources, resourceId) ->
        runCatching {
            return resources.getStringArray(resourceId(id))
        }
    }
    return getStringArray(id)
}
