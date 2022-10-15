/*
 * Copyright (C) 2022 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.utils

import android.hardware.camera2.CameraCharacteristics
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraInfo
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import kotlin.reflect.safeCast

/**
 * Class representing a device camera
 */
@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class Camera(cameraInfo: CameraInfo) {
    val camera2CameraInfo = Camera2CameraInfo.from(cameraInfo)
    val cameraId = camera2CameraInfo.cameraId.toInt()

    val cameraFacing =
        when (camera2CameraInfo.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)) {
            CameraCharacteristics.LENS_FACING_FRONT -> CameraFacing.FRONT
            CameraCharacteristics.LENS_FACING_BACK -> CameraFacing.BACK
            CameraCharacteristics.LENS_FACING_EXTERNAL -> CameraFacing.EXTERNAL
            else -> CameraFacing.UNKNOWN
        }

    val exposureCompensationRange = cameraInfo.exposureState.exposureCompensationRange
    val hasFlashUnit = cameraInfo.hasFlashUnit()

    val supportedVideoQualities: MutableList<Quality> =
        QualitySelector.getSupportedQualities(cameraInfo)

    override fun equals(other: Any?): Boolean {
        val camera = this::class.safeCast(other) ?: return false
        return this.cameraId == camera.cameraId
    }

    override fun hashCode(): Int {
        return this::class.qualifiedName.hashCode() + cameraId
    }
}
