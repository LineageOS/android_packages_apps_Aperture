/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources

@SuppressLint("DiscouragedApi")
private fun forEachRro(
    context: Context,
    id: Int,
    callback: (res: Resources, id: Int) -> Any
): Any? {
    listOf(
        "org.lineageos.aperture.auto_generated_rro_product__",
        "org.lineageos.aperture.auto_generated_rro_vendor__",
    ).forEach {
        runCatching {
            val resources = context.packageManager.getResourcesForApplication(it)
            return callback(
                resources, resources.getIdentifier(
                    context.resources.getResourceEntryName(id),
                    context.resources.getResourceTypeName(id),
                    it
                )
            )
        }
    }
    return null
}

internal fun Resources.getBoolean(context: Context, id: Int): Boolean {
    return (forEachRro(context, id) { resources, resourceId ->
        return@forEachRro resources.getBoolean(resourceId)
    } ?: getBoolean(id)) as Boolean
}

@Suppress("UNCHECKED_CAST")
internal fun Resources.getStringArray(context: Context, id: Int): Array<String> {
    return (forEachRro(context, id) { resources, resourceId ->
        return@forEachRro resources.getStringArray(resourceId)
    } ?: getStringArray(id)) as Array<String>
}
