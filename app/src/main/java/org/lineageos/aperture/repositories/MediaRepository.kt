/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.repositories

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import org.lineageos.aperture.flow.CapturedMediaFlow

class MediaRepository(
    private val context: Context,
) {
    fun capturedMedia() = CapturedMediaFlow(context).flowData()

    fun fileExists(uri: Uri): Boolean {
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

    fun broadcastNewPicture(uri: Uri) = context.sendBroadcast(Intent(ACTION_NEW_PICTURE, uri))

    fun broadcastNewVideo(uri: Uri) = context.sendBroadcast(Intent(ACTION_NEW_VIDEO, uri))

    companion object {
        private const val ACTION_NEW_PICTURE = "android.hardware.action.NEW_PICTURE"
        private const val ACTION_NEW_VIDEO = "android.hardware.action.NEW_VIDEO"
    }
}
