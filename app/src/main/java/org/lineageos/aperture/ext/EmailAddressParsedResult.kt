/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ext

import android.content.Intent
import android.net.Uri
import androidx.core.os.bundleOf
import com.google.zxing.client.result.EmailAddressParsedResult

fun EmailAddressParsedResult.createIntent() = Intent(
    Intent.ACTION_SENDTO, Uri.parse("mailto:")
).apply {
    putExtras(
        bundleOf(
            Intent.EXTRA_EMAIL to tos,
            Intent.EXTRA_CC to cCs,
            Intent.EXTRA_BCC to bcCs,
            Intent.EXTRA_SUBJECT to subject,
            Intent.EXTRA_TEXT to body
        )
    )
}
