/*
 * SPDX-FileCopyrightText: 2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.camera

import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import org.lineageos.aperture.viewmodels.CameraViewModel

/**
 * A logical camera's backing physical camera.
 */
@androidx.camera.camera2.interop.ExperimentalCamera2Interop
@androidx.camera.core.ExperimentalLensFacing
class PhysicalCamera(
    cameraInfo: CameraInfo,
    model: CameraViewModel,
    logicalCamera: Camera,
) : BaseCamera(cameraInfo, model) {
    @Suppress("RestrictedApi")
    override val cameraSelector = CameraSelector.Builder.fromSelector(logicalCamera.cameraSelector)
        .setPhysicalCameraId(cameraId)
        .build()
}
