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

object MediaStoreUtils {
    fun fileExists(context: Context, uri: Uri): Boolean {
        context.contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            arrayOf(MediaStore.Files.FileColumns._ID),
            Bundle().apply {
                // Limit
                putInt(ContentResolver.QUERY_ARG_LIMIT, 1)

                // Selection
                putString(
                    ContentResolver.QUERY_ARG_SQL_SELECTION,
                    MediaStore.MediaColumns._ID + "=?"
                )
                putStringArray(
                    ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                    arrayOf(ContentUris.parseId(uri).toString())
                )
            },
            null
        ).use {
            return it != null && it.count > 0
        }
    }
}
