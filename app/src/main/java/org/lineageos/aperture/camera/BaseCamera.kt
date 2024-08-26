/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.camera

import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import org.lineageos.aperture.models.CameraFacing
import org.lineageos.aperture.models.CameraType
import kotlin.reflect.safeCast

/**
 * A generic camera device.
 * The only contract in place is that the camera ID must be unique also between different
 * implementations (guaranteed by Android).
 */
@androidx.camera.camera2.interop.ExperimentalCamera2Interop
@androidx.camera.core.ExperimentalLensFacing
abstract class BaseCamera(cameraInfo: CameraInfo) {
    /**
     * The [CameraSelector] for this camera.
     */
    abstract val cameraSelector: CameraSelector

    /**
     * The [Camera2CameraInfo] of this camera.
     */
    protected val camera2CameraInfo = Camera2CameraInfo.from(cameraInfo)

    /**
     * Camera2's camera ID.
     */
    val cameraId = camera2CameraInfo.cameraId

    /**
     * The [CameraFacing] of this camera.
     */
    val cameraFacing = when (cameraInfo.lensFacing) {
        CameraSelector.LENS_FACING_UNKNOWN -> CameraFacing.UNKNOWN
        CameraSelector.LENS_FACING_FRONT -> CameraFacing.FRONT
        CameraSelector.LENS_FACING_BACK -> CameraFacing.BACK
        CameraSelector.LENS_FACING_EXTERNAL -> CameraFacing.EXTERNAL
        else -> throw Exception("Unknown lens facing value")
    }

    /**
     * The [CameraType] of this camera.
     */
    val cameraType = cameraFacing.cameraType

    override fun equals(other: Any?) = this::class.safeCast(other)?.let {
        this.cameraId == it.cameraId
    } ?: false

    override fun hashCode() = this::class.qualifiedName.hashCode() + cameraId.hashCode()
}
