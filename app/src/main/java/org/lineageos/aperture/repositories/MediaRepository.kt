/*
 * SPDX-FileCopyrightText: 2024-2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.repositories

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.lineageos.aperture.flow.CapturedMediaFlow

class MediaRepository(
    private val context: Context,
) {
    fun capturedMedia() = CapturedMediaFlow(context).flowData()

    fun broadcastNewPicture(uri: Uri) = context.sendBroadcast(Intent(ACTION_NEW_PICTURE, uri))

    fun broadcastNewVideo(uri: Uri) = context.sendBroadcast(Intent(ACTION_NEW_VIDEO, uri))

    companion object {
        private const val ACTION_NEW_PICTURE = "android.hardware.action.NEW_PICTURE"
        private const val ACTION_NEW_VIDEO = "android.hardware.action.NEW_VIDEO"
    }
}
