/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.ContactsContract
import android.view.textclassifier.TextClassification
import android.view.textclassifier.TextClassifier
import com.google.zxing.client.result.AddressBookParsedResult
import org.lineageos.aperture.R

fun AddressBookParsedResult.createIntent() = Intent(
    Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI
).apply {
    // TODO
}

fun AddressBookParsedResult.createTextClassification(
    context: Context
) = TextClassification.Builder()
    .setText(title)
    .setEntityType(TextClassifier.TYPE_OTHER, 1.0f)
    .apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            addAction(
                RemoteAction::class.build(
                    context,
                    R.drawable.ic_contact_phone,
                    R.string.qr_address_title,
                    R.string.qr_address_content_description,
                    createIntent()
                )
            )
        }
    }
    .build()
