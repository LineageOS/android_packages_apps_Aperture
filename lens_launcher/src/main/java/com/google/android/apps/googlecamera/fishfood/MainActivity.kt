/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.apps.googlecamera.fishfood

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivityForResult(
            Intent()
                .setAction(Intent.ACTION_VIEW)
                .setData(Uri.parse("googleapp://lens"))
                .setPackage("com.google.android.googlequicksearchbox"),
            0
        )

        finish()
    }
}
