/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.textclassifier.TextClassification
import android.view.textclassifier.TextClassifier
import com.google.zxing.client.result.GeoParsedResult
import org.lineageos.aperture.R

fun GeoParsedResult.createIntent() = Intent(Intent.ACTION_VIEW, Uri.parse(geoURI))

fun GeoParsedResult.createTextClassification(context: Context) = TextClassification.Builder()
    .setText(displayResult)
    .setEntityType(TextClassifier.TYPE_ADDRESS, 1.0f)
    .apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            addAction(
                RemoteAction::class.build(
                    context,
                    R.drawable.ic_location_on,
                    R.string.qr_geo_title,
                    R.string.qr_geo_content_description,
                    createIntent()
                )
            )
        }
    }
    .build()
