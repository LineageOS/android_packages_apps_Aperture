/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.content.Context
import androidx.camera.video.Quality
import org.lineageos.aperture.R
import org.lineageos.aperture.ext.getBoolean
import org.lineageos.aperture.ext.getOrCreate
import org.lineageos.aperture.ext.getStringArray
import org.lineageos.aperture.models.FrameRate
import kotlin.math.absoluteValue

/**
 * Overlay configuration manager.
 */
class OverlayConfiguration(context: Context) {
    private val resources = context.resources

    /**
     * @see R.bool.config_enableAuxCameras
     */
    val enableAuxCameras = resources.getBoolean(context, R.bool.config_enableAuxCameras)

    /**
     * @see R.array.config_ignoredAuxCameraIds
     */
    val ignoredAuxCameraIds = resources.getStringArray(context, R.array.config_ignoredAuxCameraIds)

    /**
     * @see R.bool.config_ignoreLogicalAuxCameras
     */
    val ignoreLogicalAuxCameras = resources.getBoolean(
        context, R.bool.config_ignoreLogicalAuxCameras
    )

    /**
     * @see R.array.config_additionalVideoConfigurations
     */
    val additionalVideoConfigurations =
        mutableMapOf<String, MutableMap<Quality, MutableMap<FrameRate, Boolean>>>().apply {
            resources.getStringArray(context, R.array.config_additionalVideoConfigurations)
                .let {
                    if (it.size % 3 != 0) {
                        // Invalid configuration
                        return@apply
                    }

                    for (i in it.indices step 3) {
                        val cameraId = it[i]
                        val frameRates = it[i + 2].split("|").mapNotNull { frameRate ->
                            FrameRate.fromValue(frameRate.toInt().absoluteValue)?.let { value ->
                                value to frameRate.startsWith('-')
                            }
                        }.toMap()

                        it[i + 1].split("|").mapNotNull { quality ->
                            when (quality) {
                                "sd" -> Quality.SD
                                "hd" -> Quality.HD
                                "fhd" -> Quality.FHD
                                "uhd" -> Quality.UHD
                                else -> null
                            }
                        }.distinct().forEach { quality ->
                            getOrCreate(cameraId).apply {
                                getOrCreate(quality).apply {
                                    putAll(frameRates)
                                }
                            }
                        }
                    }
                }
        }.map { a ->
            a.key to a.value.map { b ->
                b.key to b.value.toList()
            }.toMap()
        }.toMap()

    /**
     * @see R.array.config_logicalZoomRatios
     */
    val logicalZoomRatios = mutableMapOf<String, MutableMap<Float, Float>>().apply {
        resources.getStringArray(context, R.array.config_logicalZoomRatios).let {
            if (it.size % 3 != 0) {
                // Invalid configuration
                return@apply
            }

            for (i in it.indices step 3) {
                val cameraId = it[i]
                val approximateZoomRatio = it[i + 1].toFloat()
                val exactZoomRatio = it[i + 2].toFloat()

                getOrCreate(cameraId).apply {
                    this[approximateZoomRatio] = exactZoomRatio
                }
            }
        }
    }.map { a ->
        a.key to a.value.toMap()
    }.toMap()

    /**
     * @see R.bool.config_enableHighResolution
     */
    val enableHighResolution = resources.getBoolean(context, R.bool.config_enableHighResolution)
}
