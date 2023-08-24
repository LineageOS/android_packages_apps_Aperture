/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package androidx.camera.video

import androidx.camera.video.internal.audio.AudioSource
import androidx.camera.video.internal.audio.muted

/**
 * Get the [Recording]'s [AudioSource]. Returns null if the recording started with muted mic.
 */
val Recording.audioSource: AudioSource?
    get() = (Recording::class.java.getDeclaredField("mRecorder").apply {
        isAccessible = true
    }.get(this) as Recorder).mAudioSource

/**
 * Get the [AudioSource] status
 * @see Recording.audioSource
 */
val Recording.isAudioSourceConfigured
    get() = audioSource != null

/**
 * [Recording] muted property. Changing this property will also affect running recordings.
 * If the [Recording]'s [AudioSource] is null, getting this value will
 * return true and setting it is a no-op.
 */
var Recording.muted
    get() = audioSource?.muted != false
    @Suppress("RestrictedApi")
    set(value) {
        audioSource?.mute(value)
    }
