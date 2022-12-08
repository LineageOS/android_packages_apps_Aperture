/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.content.Context
import android.util.Range
import org.lineageos.aperture.R
import org.lineageos.aperture.obtainTypedArray

class FramerateUtils(val context: Context) {
    private val resources = context.resources

    val additionalFramerates = mutableMapOf<String, List<Framerate>>()

    init {
        resources.obtainTypedArray(context, R.array.config_additionalCameraFramerateRanges).let {
            for (i in 0 until it.length()) {
                val id = it.getResourceId(i, -1)
                if (id == -1) {
                    continue
                }

                resources.obtainTypedArray(context, id).takeIf { it.length() > 1 }?.let {
                    val cameraId = it.getString(0)!!
                    val framerates = mutableListOf<Framerate>()

                    for (i in 1 until it.length()) {
                        val range = it.getString(i)?.split("-", limit = 2) ?: continue
                        Framerate.fromRange(
                            Range(range[0].toInt(), range[1].toInt())
                        )?.let {
                            framerates.add(it)
                        }
                    }

                    additionalFramerates[cameraId] = framerates

                    it.recycle()
                }
            }

            it.recycle()
        }
    }
}
