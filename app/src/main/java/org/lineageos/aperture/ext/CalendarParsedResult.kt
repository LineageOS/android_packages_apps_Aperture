/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.CalendarContract
import android.view.textclassifier.TextClassification
import android.view.textclassifier.TextClassifier
import androidx.core.os.bundleOf
import com.google.zxing.client.result.CalendarParsedResult
import org.lineageos.aperture.R

fun CalendarParsedResult.createIntent() = Intent(
    Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI
).apply {
    summary?.let {
        putExtra(CalendarContract.Events.TITLE, it)
    }
    description?.let {
        putExtra(CalendarContract.Events.DESCRIPTION, it)
    }
    location?.let {
        putExtra(CalendarContract.Events.EVENT_LOCATION, it)
    }
    organizer?.let {
        putExtra(CalendarContract.Events.ORGANIZER, it)
    }
    attendees?.let {
        putExtra(Intent.EXTRA_EMAIL, it.joinToString(","))
    }

    putExtras(
        bundleOf(
            CalendarContract.EXTRA_EVENT_BEGIN_TIME to startTimestamp,
            CalendarContract.EXTRA_EVENT_END_TIME to endTimestamp,
            CalendarContract.EXTRA_EVENT_ALL_DAY to (isStartAllDay && isEndAllDay),
        )
    )
}

fun CalendarParsedResult.createTextClassification(context: Context) = TextClassification.Builder()
    .setText(summary)
    .setEntityType(TextClassifier.TYPE_OTHER, 1.0f)
    .apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            addAction(
                RemoteAction::class.build(
                    context,
                    R.drawable.ic_calendar_add_on,
                    R.string.qr_calendar_title,
                    R.string.qr_calendar_content_description,
                    createIntent()
                )
            )
        }
    }
    .build()
