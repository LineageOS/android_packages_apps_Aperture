/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

class BroadcastUtils(private val applicationContext: Context) {
    fun broadcastNewPicture(uri: Uri) =
        applicationContext.sendBroadcast(Intent(ACTION_NEW_PICTURE, uri))
    fun broadcastNewVideo(uri: Uri) =
        applicationContext.sendBroadcast(Intent(ACTION_NEW_VIDEO, uri))

    companion object {
        private const val ACTION_NEW_PICTURE = "android.hardware.action.NEW_PICTURE"
        private const val ACTION_NEW_VIDEO = "android.hardware.action.NEW_VIDEO"
    }
}
