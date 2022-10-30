/*
 * Copyright (C) 2022 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.VideoView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import org.lineageos.aperture.R
import org.lineageos.aperture.utils.MediaType

/**
 * Image/video preview fragment
 */
class CapturePreviewLayout(context: Context, attrs: AttributeSet?) : ConstraintLayout(
    context, attrs
) {
    private lateinit var uri: Uri
    private lateinit var mediaType: MediaType

    private val cancelButton by lazy { findViewById<ImageButton>(R.id.cancelButton) }
    private val confirmButton by lazy { findViewById<ImageButton>(R.id.confirmButton) }
    private val imageView by lazy { findViewById<ImageView>(R.id.imageView) }
    private val videoView by lazy { findViewById<VideoView>(R.id.videoView) }

    /**
     * URI is null == canceled
     * URI is not null == confirmed
     */
    internal var onChoiceCallback: (uri: Uri?) -> Unit = {}

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        cancelButton.setOnClickListener {
            onChoiceCallback(null)
        }
        confirmButton.setOnClickListener {
            onChoiceCallback(uri)
        }
    }

    internal fun updateUri(uri: Uri, mediaType: MediaType) {
        this.uri = uri
        this.mediaType = mediaType

        videoView.stopPlayback()

        imageView.isVisible = mediaType == MediaType.PHOTO
        videoView.isVisible = mediaType == MediaType.VIDEO

        when (mediaType) {
            MediaType.PHOTO -> {
                imageView.setImageURI(uri)
            }
            MediaType.VIDEO -> {
                videoView.setVideoURI(uri)
                videoView.start()
            }
        }
    }
}
