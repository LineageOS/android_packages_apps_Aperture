/*
 * Copyright (C) 2022 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.CameraControl
import com.google.common.util.concurrent.ListenableFuture

val CameraControl?.camera2CameraControl: Camera2CameraControl?
    @androidx.camera.camera2.interop.ExperimentalCamera2Interop
    get() = this?.let { Camera2CameraControl.from(it) }

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun <T : Any> CameraControl?.getCaptureRequestOption(key: CaptureRequest.Key<T>): T? =
    camera2CameraControl?.captureRequestOptions?.getCaptureRequestOption(key)

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
fun <T : Any> CameraControl?.setCaptureRequestOption(
    key: CaptureRequest.Key<T>, value: T
): ListenableFuture<Void>? = camera2CameraControl?.addCaptureRequestOptions(
    CaptureRequestOptions.Builder()
        .setCaptureRequestOption(key, value)
        .build()
)
