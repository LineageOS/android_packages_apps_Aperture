/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.app.PendingIntent
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.LocaleList
import android.provider.Settings
import android.view.textclassifier.TextClassification
import android.view.textclassifier.TextClassifier
import androidx.annotation.RequiresApi
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
            val networkSuggestion = processWifiNetworkQr(text.toString())!!
            val ssid = networkSuggestion.ssid!!

            TextClassification.Builder()
                .setText(ssid)
                .setEntityType(TextClassifier.TYPE_OTHER, 1.0f)
                .addAction(
                    RemoteAction(
                        Icon.createWithResource(context, R.drawable.ic_network_wifi),
                        context.getString(R.string.qr_wifi_title),
                        ssid,
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

        private const val WIFI_PREFIX = "WIFI:"

        private fun isValidWifiUri(text: String) =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && processWifiNetworkQr(text) != null

        @RequiresApi(Build.VERSION_CODES.Q)
        private fun processWifiNetworkQr(text: String): WifiNetworkSuggestion? {
            if (!text.uppercase().startsWith(WIFI_PREFIX)) {
                return null
            }

            val data = text.removeRange(0, WIFI_PREFIX.length).split(";").mapNotNull {
                runCatching {
                    with(it.split(":", limit = 2)) {
                        this[0] to this[1]
                    }
                }.getOrNull()
            }.toMap()

            val ssid = data["S"] ?: return null
            val isHiddenSsid = data["H"] == "true"
            val password = data["P"]
            val encryptionType = data["T"]

            // WEP is deprecated
            if (encryptionType == "WEP") {
                return null
            }

            return WifiNetworkSuggestion.Builder()
                .setSsid(ssid)
                .setIsHiddenSsid(isHiddenSsid)
                .apply {
                    password?.let {
                        // Wi-Fi QR codes uses "WPA" for WPA/WPA2/WPA3 networks,
                        // let's pray for the best
                        setWpa2Passphrase(it)
                    }
                }
                .build()
        }
    }
}
