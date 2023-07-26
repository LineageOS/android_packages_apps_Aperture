/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package androidx.camera.video

import androidx.camera.video.internal.audio.muted

private val Recording.audioSource
    get() = (Recording::class.java.getDeclaredField("mRecorder").apply {
        isAccessible = true
    }.get(this) as Recorder).mAudioSource

var Recording.muted
    get() = audioSource.muted
    @Suppress("RestrictedApi")
    set(value) {
        audioSource.mute(value)
    }
