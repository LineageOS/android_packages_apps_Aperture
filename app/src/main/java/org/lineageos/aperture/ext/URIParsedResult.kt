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
import com.google.zxing.client.result.URIParsedResult
import org.lineageos.aperture.R

fun URIParsedResult.createIntent() = Intent(Intent.ACTION_VIEW, Uri.parse(uri))

fun URIParsedResult.createTextClassification(context: Context) = TextClassification.Builder()
    .setText(uri)
    .setEntityType(TextClassifier.TYPE_URL, 1.0f)
    .apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            addAction(
                RemoteAction::class.build(
                    context,
                    R.drawable.ic_open_in_browser,
                    R.string.qr_uri_title,
                    R.string.qr_uri_content_description,
                    createIntent()
                )
            )
        }
    }
    .build()
