/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.flow

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.os.bundleOf
import org.lineageos.aperture.ext.mapEachRow
import org.lineageos.aperture.ext.queryFlow
import org.lineageos.aperture.query.Query
import org.lineageos.aperture.query.and
import org.lineageos.aperture.query.eq
import org.lineageos.aperture.query.`in`
import org.lineageos.aperture.query.join

class CapturedMediaFlow(private val context: Context) : QueryFlow<Uri> {
    override fun flowCursor() = context.contentResolver.queryFlow(
        MediaStore.Files.getContentUri(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.VOLUME_EXTERNAL
            } else {
                // ¯\_(ツ)_/¯
                "external"
            }
        ),
        arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
        ),
        bundleOf(
            ContentResolver.QUERY_ARG_SQL_SELECTION to listOfNotNull(
                MediaStore.Files.FileColumns.MEDIA_TYPE `in` listOf(
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE,
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO,
                ),
                MediaStore.Files.FileColumns.OWNER_PACKAGE_NAME eq Query.ARG,
            ).join(Query::and)?.build(),
            ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS to arrayOf(
                context.packageName,
            ),
            ContentResolver.QUERY_ARG_SQL_SORT_ORDER to
                    "${MediaStore.Files.FileColumns.DATE_ADDED} DESC",
        )
    )

    override fun flowData() = flowCursor().mapEachRow(
        arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
        )
    ) { it, indexCache ->
        var i = 0

        val id = it.getLong(indexCache[i++])

        val externalContentUri = when (val mediaType = it.getInt(indexCache[i++])) {
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE ->
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO ->
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI

            else -> throw Exception("Invalid media type: $mediaType")
        }

        return@mapEachRow ContentUris.withAppendedId(externalContentUri, id)
    }
}
