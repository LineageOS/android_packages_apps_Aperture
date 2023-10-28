/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.provider.Settings
import android.view.textclassifier.TextClassification
import android.view.textclassifier.TextClassifier
import androidx.annotation.RequiresApi
import com.google.zxing.client.result.WifiParsedResult
import org.lineageos.aperture.R

@RequiresApi(Build.VERSION_CODES.R)
fun WifiParsedResult.createIntent() = Intent(Settings.ACTION_WIFI_ADD_NETWORKS).apply {
    putExtra(
        Settings.EXTRA_WIFI_NETWORK_LIST,
        arrayListOf(
            WifiNetworkSuggestion.Builder()
                .setSsid(ssid)
                .setIsHiddenSsid(isHidden)
                .apply {
                    password?.let {
                        when (networkEncryption) {
                            "WPA" -> {
                                // Per specs, Wi-Fi QR codes are only used for
                                // WPA2 and WPA-Mixed networks, we can safely assume
                                // this networks supports WPA2
                                setWpa2Passphrase(it)
                            }

                            "SAE" -> {
                                setWpa3Passphrase(it)
                            }
                        }
                    }
                }
                .build()
        )
    )
}

fun WifiParsedResult.createTextClassification(context: Context) = TextClassification.Builder()
    .setText(ssid)
    .setEntityType(TextClassifier.TYPE_OTHER, 1.0f)
    .apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            addAction(
                RemoteAction::class.build(
                    context,
                    R.drawable.ic_network_wifi,
                    R.string.qr_wifi_title,
                    R.string.qr_wifi_content_description,
                    createIntent()
                )
            )
        }
    }
    .build()
