package org.lineageos.aperture.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
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
        val query = contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            projection,
            MediaStore.MediaColumns.OWNER_PACKAGE_NAME + "=?",
            arrayOf(BuildConfig.APPLICATION_ID),
            MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
        )

        query?.use { cursor ->
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
