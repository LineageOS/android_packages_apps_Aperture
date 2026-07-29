/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.qr

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.LocaleList
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import android.text.SpannableString
import android.view.textclassifier.TextClassificationManager
import androidx.core.os.bundleOf
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.google.zxing.client.result.AddressBookParsedResult
import com.google.zxing.client.result.CalendarParsedResult
import com.google.zxing.client.result.EmailAddressParsedResult
import com.google.zxing.client.result.GeoParsedResult
import com.google.zxing.client.result.ISBNParsedResult
import com.google.zxing.client.result.ProductParsedResult
import com.google.zxing.client.result.ResultParser
import com.google.zxing.client.result.SMSParsedResult
import com.google.zxing.client.result.TelParsedResult
import com.google.zxing.client.result.TextParsedResult
import com.google.zxing.client.result.URIParsedResult
import com.google.zxing.client.result.VINParsedResult
import com.google.zxing.client.result.WifiParsedResult
import org.lineageos.aperture.R
import org.lineageos.aperture.models.QrResult
import zxingcpp.BarcodeReader

class QrTextClassifier(private val context: Context) {
    // System services
    private val textClassificationManager by lazy {
        context.getSystemService(TextClassificationManager::class.java)
    }
    private val wifiManager by lazy {
        runCatching { context.getSystemService(WifiManager::class.java) }.getOrNull()
    }

    fun classify(result: BarcodeReader.Result): QrResult {
        // Try with ZXing parser
        val parsedResult = ResultParser.parseResult(result.toResult())

        when (parsedResult) {
            is AddressBookParsedResult -> parsedResult.toQrResult(context)
            is CalendarParsedResult -> parsedResult.toQrResult(context)
            is EmailAddressParsedResult -> parsedResult.toQrResult(context)
            is GeoParsedResult -> parsedResult.toQrResult(context)
            is ISBNParsedResult -> parsedResult.toQrResult(context)
            is ProductParsedResult -> parsedResult.toQrResult(context)
            is SMSParsedResult -> parsedResult.toQrResult(context)
            is TelParsedResult -> parsedResult.toQrResult(context)
            is TextParsedResult -> null // Try with the next methods
            is URIParsedResult -> null // We handle this manually
            is VINParsedResult -> parsedResult.toQrResult(context)
            is WifiParsedResult -> parsedResult.toQrResult(context)
            else -> error("Unknown parsed result type")
        }?.let {
            return it
        }

        // We handle text-based results here
        val qrText = when (parsedResult) {
            is TextParsedResult -> parsedResult.text
            is URIParsedResult -> parsedResult.uri
            else -> result.text
        }

        // Try parsing it as a Uri
        Uri.parse(qrText.toString()).let { uri ->
            when (uri.scheme?.lowercase()) {
                SCHEME_DPP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    wifiManager?.isEasyConnectSupported == true
                ) {
                    return QrResult(context) {
                        setText(R.string.qr_dpp_description)

                        addAction {
                            setTitle(R.string.qr_dpp_title)
                            setContentDescription(R.string.qr_dpp_description)
                            setIcon(R.drawable.ic_network_wifi)
                            setIntent(Intent(Settings.ACTION_PROCESS_WIFI_EASY_CONNECT_URI, uri))
                        }
                    }
                }

                SCHEME_FIDO -> return QrResult(context) {
                    setText(R.string.qr_fido_content_description)

                    addAction {
                        setTitle(R.string.qr_fido_title)
                        setContentDescription(R.string.qr_fido_content_description)
                        setIcon(R.drawable.ic_passkey)
                        setIntent(Intent(Intent.ACTION_VIEW, uri))
                    }
                }

                SCHEME_UPI -> return QrResult(context) {
                    setText(R.string.qr_upi_title)

                    addAction {
                        setTitle(R.string.qr_upi_title)
                        setContentDescription(R.string.qr_upi_content_description)
                        setIcon(R.drawable.ic_currency_rupee)
                        setIntent(Intent(Intent.ACTION_VIEW, uri))
                    }
                }
            }
        }

        // Let Android classify it
        val spannableString = SpannableString(qrText)
        return textClassificationManager.textClassifier.classifyText(
            spannableString,
            0,
            spannableString.length,
            LocaleList.getDefault(),
        ).let {
            QrResult(context) {
                text = it.text

                it.actions.forEach { action ->
                    addAction {
                        title = action.title.toString()
                        contentDescription = action.contentDescription.toString()
                        icon = action.icon
                        pendingIntent = action.actionIntent
                    }
                }
            }
        }
    }

    private fun BarcodeReader.Result.toResult() = Result(
        text,
        bytes,
        null,
        when (format) {
            BarcodeReader.Format.NONE -> null
            BarcodeReader.Format.ALL -> null
            BarcodeReader.Format.ALL_CREATABLE -> null
            BarcodeReader.Format.ALL_GS1 -> null
            BarcodeReader.Format.ALL_INDUSTRIAL -> null
            BarcodeReader.Format.ALL_LINEAR -> null
            BarcodeReader.Format.ALL_MATRIX -> null
            BarcodeReader.Format.ALL_READABLE -> null
            BarcodeReader.Format.ALL_RETAIL -> null
            BarcodeReader.Format.AZTEC -> BarcodeFormat.AZTEC
            BarcodeReader.Format.AZTEC_CODE -> BarcodeFormat.AZTEC
            BarcodeReader.Format.AZTEC_RUNE -> BarcodeFormat.AZTEC
            BarcodeReader.Format.CODABAR -> BarcodeFormat.CODABAR
            BarcodeReader.Format.CODE_32 -> null
            BarcodeReader.Format.CODE_39 -> BarcodeFormat.CODE_39
            BarcodeReader.Format.CODE_39_EXT -> BarcodeFormat.CODE_39
            BarcodeReader.Format.CODE_39_STD -> BarcodeFormat.CODE_39
            BarcodeReader.Format.CODE_93 -> BarcodeFormat.CODE_93
            BarcodeReader.Format.CODE_128 -> BarcodeFormat.CODE_128
            BarcodeReader.Format.COMPACT_PDF_417 -> BarcodeFormat.PDF_417
            BarcodeReader.Format.DATA_BAR -> null
            BarcodeReader.Format.DATA_BAR_EXP -> null
            BarcodeReader.Format.DATA_BAR_EXP_STK -> null
            BarcodeReader.Format.DATA_BAR_LTD -> null
            BarcodeReader.Format.DATA_BAR_OMNI -> null
            BarcodeReader.Format.DATA_BAR_STK -> null
            BarcodeReader.Format.DATA_BAR_STK_OMNI -> null
            BarcodeReader.Format.DATA_MATRIX -> BarcodeFormat.DATA_MATRIX
            BarcodeReader.Format.DX_FILM_EDGE -> null
            BarcodeReader.Format.EAN_2 -> null
            BarcodeReader.Format.EAN_5 -> null
            BarcodeReader.Format.EAN_8 -> BarcodeFormat.EAN_8
            BarcodeReader.Format.EAN_13 -> BarcodeFormat.EAN_13
            BarcodeReader.Format.EAN_UPC -> BarcodeFormat.UPC_EAN_EXTENSION
            BarcodeReader.Format.ISBN -> null
            BarcodeReader.Format.ITF -> BarcodeFormat.ITF
            BarcodeReader.Format.ITF_14 -> null
            BarcodeReader.Format.MAXI_CODE -> BarcodeFormat.MAXICODE
            BarcodeReader.Format.MICRO_PDF_417 -> BarcodeFormat.PDF_417
            BarcodeReader.Format.MICRO_QR_CODE -> BarcodeFormat.QR_CODE
            BarcodeReader.Format.OTHER_BARCODE -> null
            BarcodeReader.Format.PDF_417 -> BarcodeFormat.PDF_417
            BarcodeReader.Format.PZN -> null
            BarcodeReader.Format.QR_CODE -> BarcodeFormat.QR_CODE
            BarcodeReader.Format.QR_CODE_MODEL_1 -> BarcodeFormat.QR_CODE
            BarcodeReader.Format.QR_CODE_MODEL_2 -> BarcodeFormat.QR_CODE
            BarcodeReader.Format.RMQR_CODE -> BarcodeFormat.QR_CODE
            BarcodeReader.Format.TELEPEN -> null
            BarcodeReader.Format.TELEPEN_ALPHA -> null
            BarcodeReader.Format.TELEPEN_NUMERIC -> null
            BarcodeReader.Format.UPC_A -> BarcodeFormat.UPC_A
            BarcodeReader.Format.UPC_E -> BarcodeFormat.UPC_E
        },
    )

    fun AddressBookParsedResult.toQrResult(
        context: Context
    ) = QrResult(context) {
        text = this@toQrResult.title ?: names.firstOrNull() ?: ""

        addAction {
            setTitle(R.string.qr_address_title)
            setContentDescription(R.string.qr_address_content_description)
            setIcon(R.drawable.ic_contact_phone)
            setIntent(
                Intent(
                    Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI
                ).apply {
                    names.firstOrNull()?.let {
                        putExtra(ContactsContract.Intents.Insert.NAME, it)
                    }

                    pronunciation?.let {
                        putExtra(ContactsContract.Intents.Insert.PHONETIC_NAME, it)
                    }

                    phoneNumbers?.let { phoneNumbers ->
                        val phoneTypes = phoneTypes ?: arrayOf()

                        for ((i, keys) in listOf(
                            listOf(
                                ContactsContract.Intents.Insert.PHONE,
                                ContactsContract.Intents.Insert.PHONE_TYPE,
                            ),
                            listOf(
                                ContactsContract.Intents.Insert.SECONDARY_PHONE,
                                ContactsContract.Intents.Insert.SECONDARY_PHONE_TYPE,
                            ),
                            listOf(
                                ContactsContract.Intents.Insert.TERTIARY_PHONE,
                                ContactsContract.Intents.Insert.TERTIARY_PHONE_TYPE,
                            ),
                        ).withIndex()) {
                            phoneNumbers.getOrNull(i)?.let { phone ->
                                putExtra(keys.first(), phone)
                                phoneTypes.getOrNull(i)?.let {
                                    putExtra(keys.last(), it)
                                }
                            }
                        }
                    }

                    emails?.let { emails ->
                        val emailTypes = emailTypes ?: arrayOf()

                        for ((i, keys) in listOf(
                            listOf(
                                ContactsContract.Intents.Insert.EMAIL,
                                ContactsContract.Intents.Insert.EMAIL_TYPE,
                            ),
                            listOf(
                                ContactsContract.Intents.Insert.SECONDARY_EMAIL,
                                ContactsContract.Intents.Insert.SECONDARY_EMAIL_TYPE,
                            ),
                            listOf(
                                ContactsContract.Intents.Insert.TERTIARY_EMAIL,
                                ContactsContract.Intents.Insert.TERTIARY_EMAIL_TYPE,
                            ),
                        ).withIndex()) {
                            emails.getOrNull(i)?.let { phone ->
                                putExtra(keys.first(), phone)
                                emailTypes.getOrNull(i)?.let {
                                    putExtra(keys.last(), it)
                                }
                            }
                        }
                    }

                    instantMessenger?.let {
                        putExtra(ContactsContract.Intents.Insert.IM_HANDLE, it)
                    }

                    note?.let {
                        putExtra(ContactsContract.Intents.Insert.NOTES, it)
                    }

                    addresses?.let { emails ->
                        val addressTypes = addressTypes ?: arrayOf()

                        for ((i, keys) in listOf(
                            listOf(
                                ContactsContract.Intents.Insert.POSTAL,
                                ContactsContract.Intents.Insert.POSTAL_TYPE,
                            ),
                        ).withIndex()) {
                            emails.getOrNull(i)?.let { phone ->
                                putExtra(keys.first(), phone)
                                addressTypes.getOrNull(i)?.let {
                                    putExtra(keys.last(), it)
                                }
                            }
                        }
                    }

                    org?.let {
                        putExtra(ContactsContract.Intents.Insert.COMPANY, it)
                    }
                }
            )
        }
    }

    fun CalendarParsedResult.toQrResult(context: Context) = QrResult(context) {
        text = summary

        addAction {
            setTitle(R.string.qr_calendar_title)
            setContentDescription(R.string.qr_calendar_content_description)
            setIcon(R.drawable.ic_calendar_add_on)
            setIntent(
                Intent(
                    Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI
                ).apply {
                    summary?.let {
                        putExtra(CalendarContract.Events.TITLE, it)
                    }
                    description?.let {
                        putExtra(CalendarContract.Events.DESCRIPTION, it)
                    }
                    location?.let {
                        putExtra(CalendarContract.Events.EVENT_LOCATION, it)
                    }
                    organizer?.let {
                        putExtra(CalendarContract.Events.ORGANIZER, it)
                    }
                    attendees?.let {
                        putExtra(Intent.EXTRA_EMAIL, it.joinToString(","))
                    }

                    putExtras(
                        bundleOf(
                            CalendarContract.EXTRA_EVENT_BEGIN_TIME to startTimestamp,
                            CalendarContract.EXTRA_EVENT_END_TIME to endTimestamp,
                            CalendarContract.EXTRA_EVENT_ALL_DAY to (isStartAllDay && isEndAllDay),
                        )
                    )
                }
            )
        }
    }

    fun EmailAddressParsedResult.toQrResult(
        context: Context
    ) = QrResult(context) {
        text = tos.joinToString()

        addAction {
            setTitle(R.string.qr_email_title)
            setContentDescription(R.string.qr_email_content_description)
            setIcon(R.drawable.ic_email)
            setIntent(
                Intent(
                    Intent.ACTION_SENDTO,
                    Uri.parse("mailto:${tos?.firstOrNull() ?: ""}")
                ).apply {
                    putExtras(
                        bundleOf(
                            Intent.EXTRA_EMAIL to tos,
                            Intent.EXTRA_CC to cCs,
                            Intent.EXTRA_BCC to bcCs,
                            Intent.EXTRA_SUBJECT to subject,
                            Intent.EXTRA_TEXT to body,
                        )
                    )
                }
            )
        }
    }

    fun GeoParsedResult.toQrResult(context: Context) = QrResult(context) {
        text = displayResult

        addAction {
            setTitle(R.string.qr_geo_title)
            setContentDescription(R.string.qr_geo_content_description)
            setIcon(R.drawable.ic_location_on)
            setIntent(Intent(Intent.ACTION_VIEW, Uri.parse(geoURI)))
        }
    }

    fun ISBNParsedResult.toQrResult(context: Context) = QrResult(context) {
        text = isbn

        addAction {
            setTitle(R.string.qr_isbn_title)
            setContentDescription(R.string.qr_isbn_content_description)
            setIcon(R.drawable.ic_book)
            setIntent(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://isbnsearch.org/isbn/${isbn}"),
                )
            )
        }
    }

    fun ProductParsedResult.toQrResult(context: Context) = QrResult(context) {
        text = productID

        addAction {
            setTitle(R.string.qr_product_title)
            setContentDescription(R.string.qr_product_content_description)
            setIcon(R.drawable.ic_shopping_cart)
            setIntent(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.barcodelookup.com/${productID}"),
                )
            )
        }
    }

    fun SMSParsedResult.toQrResult(context: Context) = QrResult(context) {
        text = numbers.first()

        addAction {
            setTitle(R.string.qr_sms_title)
            setContentDescription(R.string.qr_sms_content_description)
            setIcon(R.drawable.ic_sms)
            setIntent(Intent(Intent.ACTION_SENDTO, Uri.parse(smsuri)))
        }
    }

    fun TelParsedResult.toQrResult(context: Context) = QrResult(context) {
        text = number

        addAction {
            setTitle(R.string.qr_tel_title)
            setContentDescription(R.string.qr_tel_content_description)
            setIcon(R.drawable.ic_phone)
            setIntent(Intent(Intent.ACTION_SENDTO, Uri.parse(telURI)))
        }
    }

    fun VINParsedResult.toQrResult(context: Context) = QrResult(context) {
        text = vin

        addAction {
            setTitle(R.string.qr_vin_title)
            setContentDescription(R.string.qr_vin_content_description)
            setIcon(R.drawable.ic_directions_car)
            setIntent(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.vindecoderz.com/EN/check-lookup/${vin}"),
                )
            )
        }
    }

    fun WifiParsedResult.toQrResult(context: Context) = QrResult(context) {
        text = ssid

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            addAction {
                setTitle(R.string.qr_wifi_title)
                setContentDescription(R.string.qr_wifi_content_description)
                setIcon(R.drawable.ic_network_wifi)
                setIntent(
                    Intent(Settings.ACTION_WIFI_ADD_NETWORKS).apply {
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
                                                    // WPA2 and WPA-Mixed networks, we can safely
                                                    // assume this networks supports WPA2
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
                )
            }
        }
    }

    companion object {
        private const val SCHEME_DPP = "dpp"
        private const val SCHEME_FIDO = "fido"
        private const val SCHEME_UPI = "upi"
    }
}
