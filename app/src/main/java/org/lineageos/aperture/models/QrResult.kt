/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.PendingIntentCompat

/**
 * QR result.
 */
data class QrResult(
    val text: String?,
    val actions: List<Action>,
) {
    data class Action(
        val title: String?,
        val contentDescription: String?,
        val icon: Icon?,
        val canTintIcon: Boolean,
        val pendingIntent: PendingIntent?,
    ) : UniqueItem<Action> {
        class Builder(private val context: Context) {
            var title: String? = null
            var contentDescription: String? = null
            var icon: Icon? = null
            var canTintIcon: Boolean = false
            var pendingIntent: PendingIntent? = null

            fun setTitle(@StringRes stringResId: Int) {
                title = context.getString(stringResId)
            }

            fun setContentDescription(@StringRes stringResId: Int) {
                contentDescription = context.getString(stringResId)
            }

            fun setIcon(
                @DrawableRes drawableResId: Int,
                canTint: Boolean = true,
            ) {
                icon = Icon.createWithResource(context, drawableResId)
                canTintIcon = canTint
            }

            fun setIntent(
                intent: Intent,
                requestCode: Int = 0,
                @PendingIntentCompat.Flags flags: Int = PendingIntent.FLAG_UPDATE_CURRENT,
                isMutable: Boolean = false,
            ) {
                pendingIntent = PendingIntentCompat.getActivity(
                    context,
                    requestCode,
                    intent,
                    flags,
                    isMutable,
                )
            }

            fun build() = Action(
                title = title,
                contentDescription = contentDescription,
                icon = icon,
                canTintIcon = canTintIcon,
                pendingIntent = pendingIntent,
            )
        }

        override fun areItemsTheSame(other: Action) = this == other

        override fun areContentsTheSame(other: Action) = true

        companion object {
            operator fun invoke(
                context: Context,
                block: Builder.() -> Unit,
            ) = Builder(context).apply(block).build()
        }
    }

    class Builder(private val context: Context) {
        var text: String? = null
        val actions = mutableListOf<Action>()

        fun setText(@StringRes stringResId: Int) {
            text = context.getString(stringResId)
        }

        fun addAction(
            block: Action.Builder.() -> Unit,
        ) = actions.add(Action(context, block))

        fun build() = QrResult(
            text = text,
            actions = actions.toList(),
        )
    }

    companion object {
        operator fun invoke(
            context: Context,
            block: Builder.() -> Unit,
        ) = Builder(context).apply(block).build()
    }
}
