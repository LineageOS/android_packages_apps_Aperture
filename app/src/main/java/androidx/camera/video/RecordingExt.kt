/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package androidx.camera.video

fun Recording.mute(muted: Boolean) = (Recording::class.java.getDeclaredField("mRecorder").apply {
    isAccessible = true
}.get(this) as Recorder).mAudioSource.mute(muted)
