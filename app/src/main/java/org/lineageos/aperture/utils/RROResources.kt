/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources

class RROResources private constructor(val resources: Resources, private val packageName: String) {
    @SuppressLint("DiscouragedApi")
    fun getIdentifier(context: Context, id: Int) =
        resources.getIdentifier(
            context.resources.getResourceEntryName(id),
            context.resources.getResourceTypeName(id),
            packageName,
        )

    fun getBoolean(context: Context, id: Int): Boolean =
        resources.getBoolean(getIdentifier(context, id))
    fun getStringArray(context: Context, id: Int): Array<String> =
        resources.getStringArray(getIdentifier(context, id))

    companion object {
        fun get(context: Context, packageName: String): RROResources {
            val resources = context.packageManager.getResourcesForApplication(packageName)
            return RROResources(resources, packageName)
        }

        private val AUTO_GENERATED_RRO_PACKAGES = listOf(
            "org.lineageos.aperture.auto_generated_rro_product__",
            "org.lineageos.aperture.auto_generated_rro_vendor__",
        )

        fun getAutoGeneratedRROResources(context: Context) =
            AUTO_GENERATED_RRO_PACKAGES.mapNotNull {
                runCatching {
                    get(context, it)
                }.getOrNull()
            }
    }
}
