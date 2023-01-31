/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import androidx.annotation.RequiresApi

data class WifiNetwork(
    val ssid: String,
    val isSsidHidden: Boolean = false,
    val password: String? = null,
    val encryptionType: EncryptionType = EncryptionType.NONE
) {
    enum class EncryptionType {
        NONE,
        WEP,
        WPA;
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun toNetworkSuggestion(): WifiNetworkSuggestion? {
        if (encryptionType == EncryptionType.WEP) {
            return null
        }

        return WifiNetworkSuggestion.Builder()
            .setSsid(ssid)
            .setIsHiddenSsid(isSsidHidden)
            .apply {
                password?.let {
                    // Wi-Fi QR codes uses "WPA" for WPA/WPA2/WPA3 networks,
                    // let's pray for the best
                    setWpa2Passphrase(it)
                }
            }
            .build()
    }

    companion object {
        fun fromQr(text: String): WifiNetwork? {
            val prefix = "WIFI:"

            if (!text.uppercase().startsWith(prefix)) {
                return null
            }

            val data = text.removeRange(0, prefix.length).split(";").mapNotNull {
                runCatching {
                    with(it.split(":", limit = 2)) {
                        this[0] to this[1]
                    }
                }.getOrNull()
            }.toMap()

            val ssid = data["S"]?.let {
                if (it.startsWith('"') and it.endsWith('"')) {
                    it.removePrefix('"'.toString()).removeSuffix('"'.toString())
                } else {
                    it
                }
            } ?: return null
            val isSsidHidden = data["H"] == "true"
            val password = data["P"]
            val encryptionType = when (data["T"]) {
                "WEP" -> EncryptionType.WEP
                "WPA" -> EncryptionType.WPA
                else -> EncryptionType.NONE
            }

            return WifiNetwork(
                ssid, isSsidHidden, password, encryptionType
            )
        }
    }
}
