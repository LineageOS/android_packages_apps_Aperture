/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.models

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import java.io.ByteArrayOutputStream

/**
 * A generic event, emitted by the VM, handled by the activity.
 * Consider those as one-shot events, so they shouldn't be handled more than once per emission.
 */
sealed interface Event {
    /**
     * No camera is available on the device, the app should close.
     */
    data object NoCamera : Event

    /**
     * Show the force torch introduction.
     */
    data object ShowForceTorchHelp : Event

    /**
     * Start the flip camera button animation.
     */
    data object FlipCameraAnimation : Event

    /**
     * Photo capture event.
     */
    sealed interface PhotoCaptureStatus : Event {
        data object CaptureStarted : PhotoCaptureStatus

        data class ImageSaved(
            val output: ImageCapture.OutputFileResults,
            val photoOutputStream: ByteArrayOutputStream?,
        ) : PhotoCaptureStatus

        data class Error(
            val exception: ImageCaptureException,
        ) : PhotoCaptureStatus
    }
}
