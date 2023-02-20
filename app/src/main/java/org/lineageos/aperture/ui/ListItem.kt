/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import org.lineageos.aperture.R

/**
 * A poor man's M3 ListItem implementation
 * https://m3.material.io/components/lists/overview
 */
class ListItem @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val headlineTextView by lazy { findViewById<TextView>(R.id.headlineTextView) }
    private val supportingTextView by lazy { findViewById<TextView>(R.id.supportingTextView) }
    private val trailingSupportingTextView by lazy { findViewById<TextView>(R.id.trailingSupportingTextView) }

    var headlineText: CharSequence?
        get() = headlineTextView.text
        set(value) {
            headlineTextView.text = value
        }
    var supportingText: CharSequence?
        get() = supportingTextView.text
        set(value) {
            supportingTextView.text = value
        }
    var trailingSupportingText: CharSequence?
        get() = trailingSupportingTextView.text
        set(value) {
            trailingSupportingTextView.text = value
        }

    init {
        inflate(context, R.layout.list_item, this)

        context.obtainStyledAttributes(attrs, R.styleable.ListItem, 0, 0).apply {
            try {
                headlineText = getString(R.styleable.ListItem_headlineText)
                supportingText = getString(R.styleable.ListItem_supportingText)
                trailingSupportingText = getString(R.styleable.ListItem_trailingSupportingText)
            } finally {
                recycle()
            }
        }
    }
}
