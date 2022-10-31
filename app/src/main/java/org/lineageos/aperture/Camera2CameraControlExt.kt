/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import com.google.common.util.concurrent.ListenableFuture

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun <T : Any> Camera2CameraControl.getCaptureRequestOption(key: CaptureRequest.Key<T>): T? =
    captureRequestOptions.getCaptureRequestOption(key)

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun <T : Any> Camera2CameraControl.setCaptureRequestOption(
    key: CaptureRequest.Key<T>, value: T
): ListenableFuture<Void> = addCaptureRequestOptions(
    CaptureRequestOptions.Builder()
        .setCaptureRequestOption(key, value)
        .build()
)
