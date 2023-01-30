/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.app.PendingIntent
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.LocaleList
import android.provider.Settings
import android.view.textclassifier.TextClassification
import android.view.textclassifier.TextClassifier

class QrTextClassifier(
    private val context: Context, private val parent: TextClassifier
) : TextClassifier {
    private val wifiManager by lazy { context.getSystemService(WifiManager::class.java) }

    override fun classifyText(
        text: CharSequence,
        startIndex: Int,
        endIndex: Int,
        defaultLocales: LocaleList?
    ): TextClassification {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    wifiManager.isEasyConnectSupported &&
                    text.startsWith("DPP:") -> {
                TextClassification.Builder()
                    .setText(text.toString())
                    .setEntityType(TextClassifier.TYPE_OTHER, 1.0f)
                    .addAction(
                        RemoteAction(
                            Icon.createWithResource(context, R.drawable.ic_network_wifi),
                            context.getString(R.string.qr_dpp),
                            context.getString(R.string.qr_dpp),
                            PendingIntent.getActivity(
                                context,
                                0,
                                Intent(Settings.ACTION_PROCESS_WIFI_EASY_CONNECT_URI).apply {
                                    data = runCatching {
                                        Uri.parse(text.toString())
                                    }.getOrNull()
                                },
                                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                            )
                        )
                    )
                    .build()
            }
            else -> parent.classifyText(text, startIndex, endIndex, defaultLocales)
        }
    }
}
