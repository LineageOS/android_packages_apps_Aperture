/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import androidx.camera.view.CameraController
import org.lineageos.aperture.utils.PhysicalCamera

internal val CameraController.physicalCamera: PhysicalCamera?
    @androidx.camera.camera2.interop.ExperimentalCamera2Interop
    get() = cameraInfo?.let { PhysicalCamera(it) }
