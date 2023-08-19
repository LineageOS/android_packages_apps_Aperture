/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.qr

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
import org.lineageos.aperture.R

class QrTextClassifier(
    private val context: Context, private val parent: TextClassifier
) : TextClassifier {
    private val wifiManager by lazy {
        runCatching { context.getSystemService(WifiManager::class.java) }.getOrNull()
    }

    override fun classifyText(
        text: CharSequence,
        startIndex: Int,
        endIndex: Int,
        defaultLocales: LocaleList?
    ): TextClassification = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                isValidDppUri(text.toString()) &&
                wifiManager?.isEasyConnectSupported == true -> {
            TextClassification.Builder()
                .setText(context.getString(R.string.qr_dpp_description))
                .setEntityType(TextClassifier.TYPE_OTHER, 1.0f)
                .addAction(
                    RemoteAction(
                        Icon.createWithResource(context, R.drawable.ic_network_wifi),
                        context.getString(R.string.qr_dpp_title),
                        context.getString(R.string.qr_dpp_description),
                        PendingIntent.getActivity(
                            context,
                            0,
                            Intent(Settings.ACTION_PROCESS_WIFI_EASY_CONNECT_URI).apply {
                                data = Uri.parse(text.toString())
                            },
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )
                )
                .build()
        }

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && isValidWifiUri(text.toString()) -> {
            val wifiNetwork = WifiNetwork.fromQr(text.toString())!!
            val networkSuggestion = wifiNetwork.toNetworkSuggestion()!!

            TextClassification.Builder()
                .setText(wifiNetwork.ssid)
                .setEntityType(TextClassifier.TYPE_OTHER, 1.0f)
                .addAction(
                    RemoteAction(
                        Icon.createWithResource(context, R.drawable.ic_network_wifi),
                        context.getString(R.string.qr_wifi_title),
                        wifiNetwork.ssid,
                        PendingIntent.getActivity(
                            context,
                            0,
                            Intent(Settings.ACTION_WIFI_ADD_NETWORKS).apply {
                                putExtra(
                                    Settings.EXTRA_WIFI_NETWORK_LIST,
                                    arrayListOf(networkSuggestion)
                                )
                            },
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )
                )
                .build()
        }

        else -> parent.classifyText(text, startIndex, endIndex, defaultLocales)
    }

    companion object {
        private fun isValidDppUri(text: String): Boolean =
            text.startsWith("DPP:") &&
                    text.split(";").firstOrNull { it.startsWith("K:") } != null &&
                    runCatching { Uri.parse(text) }.getOrNull() != null

        private fun isValidWifiUri(text: String) =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    WifiNetwork.fromQr(text)?.toNetworkSuggestion() != null
    }
}
