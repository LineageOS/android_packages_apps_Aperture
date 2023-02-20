/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.extensions.ExtensionMode
import androidx.camera.video.Quality
import com.google.android.material.tabs.TabLayout
import org.lineageos.aperture.ui.ListItem
import org.lineageos.aperture.utils.Camera
import org.lineageos.aperture.utils.CameraFacing
import org.lineageos.aperture.utils.CameraManager
import org.lineageos.aperture.utils.VideoStabilizationMode

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class CamerasListActivity : AppCompatActivity() {
    // Views
    private val cameraFacingListItem by lazy { findViewById<ListItem>(R.id.cameraFacingListItem) }
    private val camerasTabLayout by lazy { findViewById<TabLayout>(R.id.camerasTabLayout) }
    private val exposureCompensationRangeListItem by lazy { findViewById<ListItem>(R.id.exposureCompensationRangeListItem) }
    private val hasFlashUnitListItem by lazy { findViewById<ListItem>(R.id.hasFlashUnitListItem) }
    private val intrinsicZoomRatioListItem by lazy { findViewById<ListItem>(R.id.intrinsicZoomRatioListItem) }
    private val isLogicalListItem by lazy { findViewById<ListItem>(R.id.isLogicalListItem) }
    private val supportedExtensionModesListItem by lazy { findViewById<ListItem>(R.id.supportedExtensionModesListItem) }
    private val supportedVideoQualitiesListItem by lazy { findViewById<ListItem>(R.id.supportedVideoQualitiesListItem) }
    private val supportedVideoStabilizationModesListItem by lazy { findViewById<ListItem>(R.id.supportedVideoStabilizationModesListItem) }

    private lateinit var cameraManager: CameraManager
    private lateinit var cameras: Map<String, Camera>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cameras_list)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = true
        }

        cameraManager = CameraManager(this)
        cameras = cameraManager.cameras

        camerasTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    val camera = it.text?.let { cameraId ->
                        cameras[cameraId]
                    } ?: return

                    cameraFacingListItem.supportingText = when (camera.cameraFacing) {
                        CameraFacing.BACK -> "Back"
                        CameraFacing.EXTERNAL -> "External"
                        CameraFacing.FRONT -> "Front"
                        CameraFacing.UNKNOWN -> "Unknown"
                    }
                    exposureCompensationRangeListItem.supportingText =
                        camera.exposureCompensationRange.toString()
                    hasFlashUnitListItem.supportingText = if (camera.hasFlashUnit) {
                        "True"
                    } else {
                        "False"
                    }
                    isLogicalListItem.supportingText = if (camera.isLogical) {
                        "True"
                    } else {
                        "False"
                    }
                    intrinsicZoomRatioListItem.supportingText = "${camera.intrinsicZoomRatio}x"
                    supportedVideoQualitiesListItem.supportingText =
                        camera.supportedVideoQualities.map { (videoQuality, framerates) ->
                            val videoQualityString = resources.getString(
                                when (videoQuality) {
                                    Quality.SD -> R.string.video_quality_sd
                                    Quality.HD -> R.string.video_quality_hd
                                    Quality.FHD -> R.string.video_quality_fhd
                                    Quality.UHD -> R.string.video_quality_uhd
                                    else -> throw Exception("Unknown video quality")
                                }
                            )
                            framerates.joinToString("\n") { framerate ->
                                "${videoQualityString}@${framerate.value}FPS"
                            }
                        }.joinToString("\n")
                    supportedExtensionModesListItem.supportingText =
                        camera.supportedExtensionModes.joinToString { extensionMode ->
                            when (extensionMode) {
                                ExtensionMode.AUTO -> "Auto"
                                ExtensionMode.BOKEH -> "Bokeh"
                                ExtensionMode.HDR -> "HDR"
                                ExtensionMode.NIGHT -> "Night"
                                ExtensionMode.FACE_RETOUCH -> "Face retouch"
                                ExtensionMode.NONE -> "None"
                                else -> "Unknown mode $extensionMode"
                            }
                        }
                    supportedVideoStabilizationModesListItem.supportingText =
                        camera.supportedVideoStabilizationModes.joinToString {
                                videoStabilizationMode -> when (videoStabilizationMode) {
                                    VideoStabilizationMode.OFF -> "Off"
                                    VideoStabilizationMode.ON -> "On"
                                    VideoStabilizationMode.ON_PREVIEW -> "On (preview)"
                                }
                        }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                return
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                return
            }
        })

        for ((cameraId, _) in cameras) {
            camerasTabLayout.addTab(camerasTabLayout.newTab().apply {
                text = cameraId
            })
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }
}
