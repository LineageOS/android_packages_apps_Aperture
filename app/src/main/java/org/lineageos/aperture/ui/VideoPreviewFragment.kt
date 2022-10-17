/*
 * Copyright (C) 2022 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.VideoView
import androidx.fragment.app.Fragment
import org.lineageos.aperture.R

/**
 * Video preview fragment
 */
class VideoPreviewFragment : Fragment() {
    private lateinit var uri: Uri

    private val cancelButton by lazy { requireView().findViewById<ImageButton>(R.id.cancelButton) }
    private val confirmButton by lazy { requireView().findViewById<ImageButton>(R.id.confirmButton) }
    private val videoView by lazy { requireView().findViewById<VideoView>(R.id.videoView) }

    /**
     * URI is null == canceled
     * URI is not null == confirmed
     */
    internal var onChoiceCallback: (uri: Uri?) -> Unit = {}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_video_preview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cancelButton.setOnClickListener {
            onChoiceCallback(null)
        }
        confirmButton.setOnClickListener {
            onChoiceCallback(uri)
        }
    }

    internal fun updateUri(uri: Uri) {
        this.uri = uri

        videoView.stopPlayback()
        videoView.setVideoURI(uri)
        videoView.start()
    }
}
