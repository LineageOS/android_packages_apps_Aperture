/*
 * Copyright (C) 2022 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import androidx.camera.core.ImageCapture
import androidx.camera.core.TorchState
import androidx.camera.view.CameraController
import org.lineageos.aperture.utils.FlashMode

var CameraController.flashMode: FlashMode
    get() = when (torchState.value) {
        TorchState.ON -> FlashMode.TORCH
        else -> when (imageCaptureFlashMode) {
            ImageCapture.FLASH_MODE_AUTO -> FlashMode.AUTO
            ImageCapture.FLASH_MODE_ON -> FlashMode.ON
            ImageCapture.FLASH_MODE_OFF -> FlashMode.OFF
            else -> throw Exception("Invalid flash mode")
        }
    }
    set(value) {
        enableTorch(value == FlashMode.TORCH)

        // TODO: support FlashMode.RED_EYE
        imageCaptureFlashMode = when(value) {
            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            FlashMode.TORCH -> ImageCapture.FLASH_MODE_OFF
        }
    }
