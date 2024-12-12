/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.apps.googlecamera.fishfood

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val lensActivityResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lensActivityResult.launch(
            Intent()
                .setAction(Intent.ACTION_VIEW)
                .setData(Uri.parse("googleapp://lens"))
                .setPackage("com.google.android.googlequicksearchbox")
        )
    }
}
