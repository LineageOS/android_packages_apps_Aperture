/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.qr

import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.client.result.WifiResultParser

data class WifiNetwork(
    val ssid: String,
    val isSsidHidden: Boolean = false,
    val password: String? = null,
    val encryptionType: EncryptionType = EncryptionType.NONE
) {
    enum class EncryptionType {
        NONE,
        WEP,
        WPA,
        SAE;
    }

    init {
        assert((encryptionType == EncryptionType.NONE) == (password == null)) {
            "Invalid encryption type/password combination"
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun toNetworkSuggestion(): WifiNetworkSuggestion? = when (encryptionType) {
        EncryptionType.WEP -> null
        else -> WifiNetworkSuggestion.Builder()
            .setSsid(ssid)
            .setIsHiddenSsid(isSsidHidden)
            .apply {
                password?.let {
                    when (encryptionType) {
                        EncryptionType.WPA -> {
                            // Per specs, Wi-Fi QR codes are only used for
                            // WPA2 and WPA-Mixed networks, we can safely assume
                            // this networks supports WPA2
                            setWpa2Passphrase(it)
                        }

                        EncryptionType.SAE -> {
                            setWpa3Passphrase(it)
                        }

                        else -> throw Exception("Invalid encryption type/password combination")
                    }
                }
            }
            .build()
    }

    companion object {
        fun fromQr(text: String): WifiNetwork? {
            val result = WifiResultParser().parse(
                Result(text, null, null, BarcodeFormat.QR_CODE)
            ) ?: return null

            val encryptionType = when (result.networkEncryption) {
                "WEP" -> EncryptionType.WEP
                "WPA" -> EncryptionType.WPA
                "SAE" -> EncryptionType.SAE
                else -> EncryptionType.NONE
            }

            return runCatching {
                WifiNetwork(result.ssid, result.isHidden, result.password, encryptionType)
            }.getOrNull()
        }
    }
}
