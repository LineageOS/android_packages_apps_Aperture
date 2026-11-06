/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture

/**
 * A camera with its use cases bound.
 *
 * @param camera The CameraX camera
 * @param imageCapture The bound [ImageCapture], if any
 * @param videoCapture The bound [VideoCapture], if any
 */
class CameraSession(
    val camera: androidx.camera.core.Camera,
    val imageCapture: ImageCapture?,
    val videoCapture: VideoCapture<Recorder>?,
) {
    fun setFlashMode(flashMode: FlashMode) {
        camera.cameraControl.enableTorch(flashMode == FlashMode.TORCH)

        imageCapture?.flashMode = when (flashMode) {
            FlashMode.OFF, FlashMode.TORCH -> ImageCapture.FLASH_MODE_OFF
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            FlashMode.SCREEN -> when (camera.cameraInfo.lensFacing) {
                CameraSelector.LENS_FACING_FRONT -> ImageCapture.FLASH_MODE_SCREEN
                else -> ImageCapture.FLASH_MODE_OFF
            }
        }
    }
}
