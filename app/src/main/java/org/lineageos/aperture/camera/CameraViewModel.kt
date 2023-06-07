/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.camera

import androidx.camera.video.Quality
import androidx.camera.video.Recording
import androidx.camera.view.video.AudioConfig
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.lineageos.aperture.utils.GridMode
import org.lineageos.aperture.utils.Rotation
import org.lineageos.aperture.utils.TimerMode

/**
 * [ViewModel] representing a camera session. This data is used to receive
 * live data regarding the setting currently enabled.
 */
class CameraViewModel : ViewModel() {
    // Base

    /**
     * The camera currently in use.
     */
    val camera by lazy { MutableLiveData<Camera>() }

    /**
     * Current camera mode.
     */
    val cameraMode by lazy { MutableLiveData<CameraMode>() }

    /**
     * Whether the current session is in single capture mode.
     */
    val inSingleCaptureMode by lazy { MutableLiveData(false) }

    /**
     * Current camera state.
     */
    val cameraState by lazy { MutableLiveData<CameraState>() }

    /**
     * Current screen rotation.
     */
    val screenRotation by lazy { MutableLiveData<Rotation>() }

    // General

    /**
     * Flash mode.
     */
    val flashMode by lazy { MutableLiveData(FlashMode.AUTO) }

    /**
     * Grid mode.
     */
    val gridMode by lazy { MutableLiveData<GridMode>() }

    /**
     * Timer mode.
     */
    val timerMode by lazy { MutableLiveData<TimerMode>() }

    // Photo

    /**
     * Photo capture mode.
     */
    val photoCaptureMode by lazy { MutableLiveData<Int>() }

    /**
     * Photo aspect ratio.
     */
    val photoAspectRatio by lazy { MutableLiveData<Int>() }

    /**
     * Photo effect.
     */
    val photoEffect by lazy { MutableLiveData<Int>() }

    // Video

    /**
     * Video quality.
     */
    val videoQuality by lazy { MutableLiveData<Quality>() }

    /**
     * Video frame rate.
     */
    val videoFrameRate by lazy { MutableLiveData<FrameRate?>() }

    /**
     * Video mic mode.
     */
    val videoMicMode by lazy { MutableLiveData<Boolean>() }

    /**
     * Video [AudioConfig].
     */
    val videoAudioConfig by lazy { MutableLiveData<AudioConfig>() }

    /**
     * Video [Recording].
     */
    val videoRecording by lazy { MutableLiveData<Recording?>() }
}
