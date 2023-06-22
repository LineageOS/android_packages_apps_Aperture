/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object BroadcastUtils {
    private const val ACTION_NEW_PICTURE = "android.hardware.action.NEW_PICTURE"
    private const val ACTION_NEW_VIDEO = "android.hardware.action.NEW_VIDEO"

    fun broadcastNewPicture(context: Context, uri: Uri) =
        context.sendBroadcast(Intent(ACTION_NEW_PICTURE, uri))

    fun broadcastNewVideo(context: Context, uri: Uri) =
        context.sendBroadcast(Intent(ACTION_NEW_VIDEO, uri))
}
