/*
 * SPDX-FileCopyrightText: 2022-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.content.ContentValues
import android.content.Context
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.video.MediaStoreOutputOptions
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import org.lineageos.aperture.ext.lastSavedUri
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale

object StorageUtils {
    private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    private val STORAGE_DESTINATION = "${Environment.DIRECTORY_DCIM}/Camera"

    /**
     * Returns a new ImageCapture.OutputFileOptions to use to store a JPEG photo
     */
    fun getPhotoMediaStoreOutputOptions(
        context: Context,
        customStorageLocation: Uri?,
        metadata: ImageCapture.Metadata,
        outputStream: OutputStream? = null
    ): ImageCapture.OutputFileOptions {
        if (customStorageLocation != null) {
            val documentFile = DocumentFile.fromTreeUri(context, customStorageLocation)
            val file = documentFile?.createFile("image/jpeg", getCurrentTimeString())

            // Store URI in shared preferences
            PreferenceManager.getDefaultSharedPreferences(context).lastSavedUri = file!!.uri

            return ImageCapture.OutputFileOptions
                .Builder(context.contentResolver.openOutputStream(file.uri)!!)
                .setMetadata(metadata)
                .build()
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, getCurrentTimeString())
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, STORAGE_DESTINATION)
            }
        }

        val outputFileOptions = if (outputStream != null) {
            ImageCapture.OutputFileOptions.Builder(outputStream)
        } else {
            ImageCapture.OutputFileOptions.Builder(
                context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
        }
        return outputFileOptions
            .setMetadata(metadata)
            .build()
    }

    /**
     * Returns a new OutputFileOptions to use to store a MP4 video
     */
    @androidx.camera.view.video.ExperimentalVideo
    fun getVideoMediaStoreOutputOptions(
        context: Context,
        customStorageLocation: Uri?,
        location: Location?
    ): MediaStoreOutputOptions {
        if (customStorageLocation != null) {
            val documentFile = DocumentFile.fromTreeUri(context, customStorageLocation)
            val file = documentFile?.createFile("video/mp4", getCurrentTimeString())

            // Store URI in shared preferences
            PreferenceManager.getDefaultSharedPreferences(context).lastSavedUri = file!!.uri

            return MediaStoreOutputOptions
                .Builder(context.contentResolver, file.uri)
                .build()
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, getCurrentTimeString())
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, STORAGE_DESTINATION)
            }
        }

        return MediaStoreOutputOptions
            .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .setLocation(location)
            .build()
    }

    private fun getCurrentTimeString(): String {
        return SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
    }
}
