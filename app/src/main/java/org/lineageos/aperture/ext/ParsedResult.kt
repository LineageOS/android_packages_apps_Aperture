/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import com.google.zxing.client.result.EmailAddressParsedResult
import com.google.zxing.client.result.WifiParsedResult

fun EmailAddressParsedResult.createIntent() = Intent(
    Intent.ACTION_SENDTO, Uri.parse("mailto:")
).apply {
    putExtras(
        bundleOf(
            Intent.EXTRA_EMAIL to tos,
            Intent.EXTRA_EMAIL to tos,
            Intent.EXTRA_BCC to bcCs,
            Intent.EXTRA_SUBJECT to subject,
            Intent.EXTRA_TEXT to body
        )
    )
}

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
