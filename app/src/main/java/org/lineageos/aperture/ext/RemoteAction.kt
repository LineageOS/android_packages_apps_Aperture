/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.app.PendingIntent
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import org.lineageos.aperture.R
import kotlin.reflect.KClass

fun KClass<RemoteAction>.build(
    context: Context,
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    @StringRes contentDescriptionRes: Int,
    intent: Intent,
    requestCode: Int = 0,
    flags: Int = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    @AttrRes iconTint: Int = com.google.android.material.R.attr.colorOnBackground,
) = RemoteAction(
    Icon.createWithBitmap(
        AppCompatResources.getDrawable(context, iconRes)?.let {
            DrawableCompat.wrap(it.mutate()).apply {
                DrawableCompat.setTint(
                    this,
                    context.getThemeColor(iconTint)
                )
            }
        }?.toBitmap()
    ),
    context.getString(titleRes),
    context.getString(contentDescriptionRes),
    PendingIntent.getActivity(context, requestCode, intent, flags)
)
