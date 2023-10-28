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
import androidx.core.os.bundleOf
import com.google.zxing.client.result.EmailAddressParsedResult
import org.lineageos.aperture.R

fun EmailAddressParsedResult.createIntent() = Intent(
    Intent.ACTION_SENDTO,
    Uri.parse("mailto:${tos.firstOrNull() ?: ""}")
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

fun EmailAddressParsedResult.createTextClassification(
    context: Context
) = TextClassification.Builder()
    .setText(tos.joinToString())
    .setEntityType(TextClassifier.TYPE_EMAIL, 1.0f)
    .apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            addAction(
                RemoteAction::class.build(
                    context,
                    R.drawable.ic_email,
                    R.string.qr_email_title,
                    R.string.qr_email_content_description,
                    createIntent()
                )
            )
        }
    }
    .build()
