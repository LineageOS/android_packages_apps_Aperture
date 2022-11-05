/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources

const val rroPackageName = "org.lineageos.aperture.auto_generated_rro_vendor__"

private fun getRroResources(context: Context): Resources {
    return context.packageManager.getResourcesForApplication(rroPackageName)
}

@SuppressLint("DiscouragedApi")
private fun Resources.getRroIdentifier(context: Context, id: Int, defType: String): Int {
    return getRroResources(context).getIdentifier(
        getResourceEntryName(id),
        defType,
        rroPackageName
    )
}

internal fun Resources.getBoolean(context: Context, id: Int): Boolean {
    runCatching {
        return getRroResources(context).getBoolean(getRroIdentifier(context, id, "bool"))
    }
    return getBoolean(id)
}

internal fun Resources.getStringArray(context: Context, id: Int): Array<String> {
    runCatching {
        return getRroResources(context).getStringArray(getRroIdentifier(context, id, "array"))
    }
    return getStringArray(id)
}
