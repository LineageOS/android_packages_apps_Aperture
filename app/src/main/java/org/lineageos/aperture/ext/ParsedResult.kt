/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.content.Context
import com.google.zxing.client.result.AddressBookParsedResult
import com.google.zxing.client.result.CalendarParsedResult
import com.google.zxing.client.result.EmailAddressParsedResult
import com.google.zxing.client.result.GeoParsedResult
import com.google.zxing.client.result.ISBNParsedResult
import com.google.zxing.client.result.ParsedResult
import com.google.zxing.client.result.ProductParsedResult
import com.google.zxing.client.result.SMSParsedResult
import com.google.zxing.client.result.TelParsedResult
import com.google.zxing.client.result.TextParsedResult
import com.google.zxing.client.result.URIParsedResult
import com.google.zxing.client.result.VINParsedResult
import com.google.zxing.client.result.WifiParsedResult

fun ParsedResult.createTextClassification(context: Context) = when (this) {
    is AddressBookParsedResult -> createTextClassification(context)

    is CalendarParsedResult -> createTextClassification(context)

    is EmailAddressParsedResult -> createTextClassification(context)

    is GeoParsedResult -> createTextClassification(context)

    is ISBNParsedResult -> createTextClassification(context)

    //is ProductParsedResult -> TODO()

    //is SMSParsedResult -> TODO()

    //is TelParsedResult -> TODO()

    is TextParsedResult -> null // Try with the next methods

    is URIParsedResult -> createTextClassification(context)

    is VINParsedResult -> createTextClassification(context)

    is WifiParsedResult -> createTextClassification(context)

    else -> null
}
