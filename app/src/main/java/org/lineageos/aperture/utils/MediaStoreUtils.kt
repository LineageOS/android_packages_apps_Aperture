/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import org.lineageos.aperture.BuildConfig

class MediaStoreUtils(context: Context) {
    companion object {
        private val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )
    }

    private val contentResolver = context.contentResolver
    private val mediaFiles = mutableListOf<MediaFiles>()

    fun getMediaFiles(): List<MediaFiles> {
        mediaFiles.clear()

        contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            Bundle().apply {
                // Limit
                putInt(ContentResolver.QUERY_ARG_LIMIT, 1)

                // Selection
                putString(
                    ContentResolver.QUERY_ARG_SQL_SELECTION,
                    MediaStore.MediaColumns.OWNER_PACKAGE_NAME + "=?"
                )
                putStringArray(
                    ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                    arrayOf(BuildConfig.APPLICATION_ID)
                )

                // Sort
                putInt(
                    ContentResolver.QUERY_ARG_SORT_DIRECTION,
                    ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                )
                putStringArray(
                    ContentResolver.QUERY_ARG_SORT_COLUMNS,
                    arrayOf(MediaStore.Files.FileColumns.DATE_ADDED)
                )
            },
            null
        )?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val mediaTypeColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

            while (cursor.moveToNext()) {
                // Get values of columns for a given Media.
                val mediaType = cursor.getInt(mediaTypeColumn)
                val collection = if (mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
                val contentUri: Uri =
                    ContentUris.withAppendedId(collection, cursor.getLong(idColumn))

                // Stores column values and the contentUri in a local object
                // that represents the media file.
                mediaFiles += MediaFiles(contentUri)
            }
        }
        return mediaFiles
    }
}

data class MediaFiles(val uri: Uri)
