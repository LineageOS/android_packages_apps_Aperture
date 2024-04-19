/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.qr

import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.LocaleList
import android.provider.Settings
import android.text.SpannableString
import android.view.textclassifier.TextClassification
import android.view.textclassifier.TextClassifier
import com.google.zxing.Result
import com.google.zxing.client.result.ResultParser
import com.google.zxing.client.result.URIParsedResult
import org.lineageos.aperture.R
import org.lineageos.aperture.ext.*
import kotlin.reflect.safeCast

class QrTextClassifier(
    private val context: Context, private val textClassifier: TextClassifier
) {
    private val wifiManager by lazy {
        runCatching { context.getSystemService(WifiManager::class.java) }.getOrNull()
    }

    fun classifyText(result: Result): TextClassification {
        // Try with ZXing parser
        val parsedResult = ResultParser.parseResult(result)
        parsedResult?.createTextClassification(context)?.let {
            return it
        }

        // We handle URIParsedResult here
        val text = URIParsedResult::class.safeCast(parsedResult)?.uri ?: result.text

        // Try parsing it as a Uri
        Uri.parse(text.toString()).let { uri ->
            when (uri.scheme?.lowercase()) {
                // Wi-Fi DPP
                SCHEME_DPP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    wifiManager?.isEasyConnectSupported == true
                ) {
                    return TextClassification.Builder()
                        .setText(context.getString(R.string.qr_dpp_description))
                        .setEntityType(TextClassifier.TYPE_OTHER, 1.0f)
                        .addAction(
                            RemoteAction::class.build(
                                context,
                                R.drawable.ic_network_wifi,
                                R.string.qr_dpp_title,
                                R.string.qr_dpp_description,
                                Intent(Settings.ACTION_PROCESS_WIFI_EASY_CONNECT_URI).apply {
                                    data = uri
                                }
                            )
                        )
                        .build()
                }

                SCHEME_FIDO -> return TextClassification.Builder()
                    .setText(context.getString(R.string.qr_fido_content_description))
                    .setEntityType(TextClassifier.TYPE_OTHER, 1.0f)
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            addAction(
                                RemoteAction::class.build(
                                    context,
                                    R.drawable.ic_passkey,
                                    R.string.qr_fido_title,
                                    R.string.qr_fido_content_description,
                                    Intent(Intent.ACTION_VIEW).apply {
                                        data = uri
                                    }
                                )
                            )
                        }
                    }
                    .build()

                SCHEME_UPI -> return TextClassification.Builder()
                    .setText(context.getString(R.string.qr_upi_content_description))
                    .setEntityType(TextClassifier.TYPE_OTHER, 1.0f)
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            addAction(
                                RemoteAction::class.build(
                                    context,
                                    R.drawable.ic_payments,
                                    R.string.qr_upi_title,
                                    R.string.qr_upi_content_description,
                                    Intent(Intent.ACTION_VIEW).apply {
                                        data = uri
                                    }
                                )
                            )
                        }
                    }
                    .build()
            }
        }

        // Let Android classify it
        val spannableString = SpannableString(text)
        return textClassifier.classifyText(
            spannableString, 0, spannableString.length, LocaleList.getDefault()
        )
    }

    companion object {
        private const val SCHEME_DPP = "dpp"
        private const val SCHEME_FIDO = "fido"
        private const val SCHEME_UPI = "upi"
    }
}
