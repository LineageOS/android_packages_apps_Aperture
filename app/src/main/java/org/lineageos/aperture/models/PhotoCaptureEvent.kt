/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import java.io.ByteArrayOutputStream

/**
 * Photo capture event.
 */
sealed interface PhotoCaptureEvent {
    data object CaptureStarted : PhotoCaptureEvent

    data class ImageSaved(
        val output: ImageCapture.OutputFileResults,
        val photoOutputStream: ByteArrayOutputStream?,
    ) : PhotoCaptureEvent

    data class Error(
        val exception: ImageCaptureException,
    ) : PhotoCaptureEvent
}
