/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.viewmodels

import android.app.Application
import android.net.Uri
import androidx.camera.video.Quality
import androidx.camera.video.Recording
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import org.lineageos.aperture.camera.Camera
import org.lineageos.aperture.ext.*
import org.lineageos.aperture.models.CameraMode
import org.lineageos.aperture.models.CameraState
import org.lineageos.aperture.models.FlashMode
import org.lineageos.aperture.models.FrameRate
import org.lineageos.aperture.models.GridMode
import org.lineageos.aperture.models.Rotation
import org.lineageos.aperture.models.TimerMode
import org.lineageos.aperture.models.VideoDynamicRange
import org.lineageos.aperture.repository.MediaRepository

/**
 * [ViewModel] representing a camera session. This data is used to receive
 * live data regarding the setting currently enabled.
 */
class CameraViewModel(application: Application) : AndroidViewModel(application) {
    // Base

    /**
     * The camera currently in use.
     */
    val camera = MutableLiveData<Camera>()

    /**
     * Current camera mode.
     */
    val cameraMode = MutableLiveData<CameraMode>()

    /**
     * Whether the current session is in single capture mode.
     */
    val inSingleCaptureMode = MutableLiveData(false)

    /**
     * Current camera state.
     */
    val cameraState = MutableLiveData<CameraState>()

    /**
     * Current screen rotation.
     */
    val screenRotation = MutableLiveData(Rotation.ROTATION_0)

    /**
     * Captured media [Uri]s
     */
    val capturedMedia = MediaRepository.capturedMedia(context).flowOn(
        Dispatchers.IO
    ).stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = listOf(),
    )

    // General

    /**
     * Flash mode.
     */
    val flashMode = MutableLiveData(FlashMode.AUTO)

    /**
     * Grid mode.
     */
    val gridMode = MutableLiveData<GridMode>()

    /**
     * Timer mode.
     */
    val timerMode = MutableLiveData<TimerMode>()

    // Photo

    /**
     * Photo capture mode.
     */
    val photoCaptureMode = MutableLiveData<Int>()

    /**
     * Photo aspect ratio.
     */
    val photoAspectRatio = MutableLiveData<Int>()

    /**
     * Photo effect.
     */
    val photoEffect = MutableLiveData<Int>()

    // Video

    /**
     * Video quality.
     */
    val videoQuality = MutableLiveData<Quality>()

    /**
     * Video frame rate.
     */
    val videoFrameRate = MutableLiveData<FrameRate?>()

    /**
     * Video dynamic range.
     */
    val videoDynamicRange = MutableLiveData<VideoDynamicRange>()

    /**
     * Video mic mode.
     */
    val videoMicMode = MutableLiveData<Boolean>()

    /**
     * Video [Recording].
     */
    val videoRecording = MutableLiveData<Recording?>()

    /**
     * Video recording duration.
     */
    val videoRecordingDuration = MutableLiveData<Long>()
}
