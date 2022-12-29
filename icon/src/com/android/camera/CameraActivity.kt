/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.camera2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore

open class CameraLauncher : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apertureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .setPackage("org.lineageos.aperture")
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(apertureIntent)
    }
}
