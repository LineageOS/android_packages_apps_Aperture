/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.content.Intent
import android.provider.CalendarContract
import androidx.core.os.bundleOf
import com.google.zxing.client.result.CalendarParsedResult

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
