/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import org.lineageos.aperture.R
import org.lineageos.aperture.ext.smoothRotate
import org.lineageos.aperture.utils.Rotation

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class SecondaryBarLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    // Views
    val aspectRatioButton by lazy { findViewById<Button>(R.id.aspectRatioButton)!! }
    val effectButton by lazy { findViewById<Button>(R.id.effectButton)!! }
    val flashButton by lazy { findViewById<ImageButton>(R.id.flashButton)!! }
    val gridButton by lazy { findViewById<Button>(R.id.gridButton)!! }
    val lensSelectorLayout by lazy { findViewById<LensSelectorLayout>(R.id.lensSelectorLayout)!! }
    val micButton by lazy { findViewById<Button>(R.id.micButton)!! }
    val proButton by lazy { findViewById<ImageButton>(R.id.proButton)!! }
    val settingsButton by lazy { findViewById<Button>(R.id.settingsButton)!! }
    val timerButton by lazy { findViewById<Button>(R.id.timerButton)!! }
    val videoFramerateButton by lazy { findViewById<Button>(R.id.videoFramerateButton)!! }
    val videoQualityButton by lazy { findViewById<Button>(R.id.videoQualityButton)!! }

    // System services
    private val layoutInflater = context.getSystemService(LayoutInflater::class.java)

    // Open/close state
    private var isOut = true
    private var toggledAtLeastOnce = false

    internal var screenRotation = Rotation.ROTATION_0
        set(value) {
            field = value
            updateViewsRotation()
        }

    init {
        layoutInflater.inflate(R.layout.secondary_bar_layout, this)

        proButton.setOnClickListener { slide() }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (!toggledAtLeastOnce) {
            slideDown(true)
        }
    }

    fun slideDown(now: Boolean = false) {
        if (!isOut) {
            return
        }

        isOut = false
        toggledAtLeastOnce = true

        animate()
            .translationY(measuredHeight.toFloat() / 2)
            .duration = if (now) 0 else 300
    }

    fun slideUp(now: Boolean = false) {
        if (isOut) {
            return
        }

        isOut = true
        toggledAtLeastOnce = true

        animate()
            .translationY(0f)
            .duration = if (now) 0 else 300
    }

    fun slide() {
        if (isOut) {
            slideDown()
        } else {
            slideUp()
        }
    }

    private fun updateViewsRotation() {
        val compensationValue = screenRotation.compensationValue.toFloat()

        proButton.smoothRotate(compensationValue)
        lensSelectorLayout.screenRotation = screenRotation
        flashButton.smoothRotate(compensationValue)
    }
}
