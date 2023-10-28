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
import android.view.textclassifier.TextClassification
import android.view.textclassifier.TextClassifier
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.client.result.EmailAddressParsedResult
import com.google.zxing.client.result.ParsedResultType
import com.google.zxing.client.result.ResultParser
import com.google.zxing.client.result.URIParsedResult
import com.google.zxing.client.result.WifiParsedResult
import org.lineageos.aperture.R
import org.lineageos.aperture.ext.*
import kotlin.reflect.safeCast

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
    ): TextClassification {
        // Try with ZXing parser
        ResultParser.parseResult(
            Result(text.toString(), null, null, BarcodeFormat.QR_CODE)
        )?.let { parsedResult ->
            when (parsedResult.type) {
                //ParsedResultType.ADDRESSBOOK -> TODO()
                ParsedResultType.EMAIL_ADDRESS ->
                    EmailAddressParsedResult::class.safeCast(parsedResult)?.let {
                        return TextClassification.Builder()
                            .setText(it.tos.joinToString())
                            .setEntityType(TextClassifier.TYPE_EMAIL, 1.0f)
                            .apply {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    addAction(
                                        RemoteAction::class.build(
                                            context,
                                            R.drawable.ic_email,
                                            R.string.qr_email_title,
                                            R.string.qr_email_content_description,
                                            it.createIntent()
                                        )
                                    )
                                }
                            }
                            .build()
                    }
                //ParsedResultType.PRODUCT -> TODO()
                ParsedResultType.URI -> URIParsedResult::class.safeCast(parsedResult)?.let {
                    return TextClassification.Builder()
                        .setText(it.uri)
                        .setEntityType(TextClassifier.TYPE_URL, 1.0f)
                        .apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                addAction(
                                    RemoteAction::class.build(
                                        context,
                                        R.drawable.ic_open_in_browser,
                                        R.string.qr_uri_title,
                                        R.string.qr_uri_content_description,
                                        it.createIntent()
                                    )
                                )
                            }
                        }
                        .build()
                }
                //ParsedResultType.TEXT -> TODO()
                //ParsedResultType.GEO -> TODO()
                //ParsedResultType.TEL -> TODO()
                //ParsedResultType.SMS -> TODO()
                //ParsedResultType.CALENDAR -> TODO()
                ParsedResultType.WIFI -> WifiParsedResult::class.safeCast(parsedResult)?.let {
                    return TextClassification.Builder()
                        .setText(it.ssid)
                        .setEntityType(TextClassifier.TYPE_OTHER, 1.0f)
                        .apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                addAction(
                                    RemoteAction::class.build(
                                        context,
                                        R.drawable.ic_network_wifi,
                                        R.string.qr_wifi_title,
                                        R.string.qr_wifi_content_description,
                                        it.createIntent()
                                    )
                                )
                            }
                        }
                        .build()
                }
                //ParsedResultType.ISBN -> TODO()
                //ParsedResultType.VIN -> TODO()
                else -> {}
            }
        }

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
            }
        }

        // Let Android classify it
        return parent.classifyText(text, startIndex, endIndex, defaultLocales)
    }

    companion object {
        private const val SCHEME_DPP = "dpp"
    }
}
