/*
 * SPDX-FileCopyrightText: 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.camera

enum class CameraState(
    val isRecordingVideo: Boolean = false,
) {
    IDLE,
    TAKING_PHOTO,
    PRE_RECORDING_VIDEO,
    RECORDING_VIDEO(true),
    RECORDING_VIDEO_PAUSED(true),
}
