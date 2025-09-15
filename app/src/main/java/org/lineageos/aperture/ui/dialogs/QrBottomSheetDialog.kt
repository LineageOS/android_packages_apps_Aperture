/*
 * SPDX-FileCopyrightText: 2025 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui.dialogs

import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.method.LinkMovementMethod
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import org.lineageos.aperture.R
import org.lineageos.aperture.ext.getThemeColor
import org.lineageos.aperture.ext.px
import org.lineageos.aperture.ext.sendWithBalAllowed
import org.lineageos.aperture.models.QrResult
import org.lineageos.aperture.ui.recyclerview.SimpleListAdapter
import org.lineageos.aperture.ui.recyclerview.UniqueItemDiffCallback

class QrBottomSheetDialog(context: Context) : BottomSheetDialog(context) {
    // System services
    private val clipboardManager = context.getSystemService(ClipboardManager::class.java)
    private val keyguardManager = context.getSystemService(KeyguardManager::class.java)

    // Views
    private val actionsRecyclerView by lazy {
        findViewById<RecyclerView>(R.id.actionsRecyclerView)!!
    }
    private val cardView by lazy {
        findViewById<CardView>(R.id.cardView)!!
    }
    private val copyImageButton by lazy {
        findViewById<ImageButton>(R.id.copyImageButton)!!
    }
    private val dataTextView by lazy {
        findViewById<TextView>(R.id.dataTextView)!!
    }
    private val iconImageView by lazy {
        findViewById<ImageView>(R.id.iconImageView)!!
    }
    private val shareImageButton by lazy {
        findViewById<ImageButton>(R.id.shareImageButton)!!
    }
    private val titleTextView by lazy {
        findViewById<TextView>(R.id.titleTextView)!!
    }

    private val textAction by lazy {
        QrResult.Action(context) {
            setTitle(R.string.qr_text)
            setContentDescription(R.string.qr_text)
            setIcon(R.drawable.ic_text_snippet)
        }
    }

    // RecyclerView
    private val actionsAdapter by lazy {
        object : SimpleListAdapter<QrResult.Action, MaterialButton>(
            UniqueItemDiffCallback(),
            R.layout.qr_bottom_sheet_action_button,
        ) {
            override fun ViewHolder.onBindView(item: QrResult.Action) {
                view.text = item.title
                view.contentDescription = item.contentDescription
                view.setOnClickListener {
                    try {
                        item.pendingIntent?.sendWithBalAllowed()
                    } catch (_: PendingIntent.CanceledException) {
                        Toast.makeText(
                            context,
                            R.string.qr_no_app_available_for_action,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                if (item.canTintIcon) {
                    item.icon?.setTint(
                        context.getThemeColor(com.google.android.material.R.attr.colorOnBackground)
                    )
                }
                item.icon?.loadDrawable(context)?.also { drawable ->
                    drawable.setBounds(0, 0, 15.px, 15.px)

                    view.setCompoundDrawables(
                        drawable, null, null, null
                    )
                } ?: view.setCompoundDrawables(null, null, null, null)
            }
        }
    }

    private var qrResult: QrResult? = null

    init {
        setContentView(R.layout.qr_bottom_sheet_dialog)

        cardView.setOnClickListener {
            qrResult?.actions?.firstOrNull()?.let { action ->
                try {
                    action.pendingIntent?.sendWithBalAllowed()
                } catch (_: PendingIntent.CanceledException) {
                    Toast.makeText(
                        context,
                        R.string.qr_no_app_available_for_action,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        copyImageButton.setOnClickListener {
            qrResult?.let {
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText("", it.text)
                )
            }
        }

        shareImageButton.setOnClickListener {
            qrResult?.text?.let {
                context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = ClipDescription.MIMETYPE_TEXT_PLAIN
                            putExtra(Intent.EXTRA_TEXT, it)
                        },
                        context.getString(R.string.qr_share_with_action),
                    )
                )
            }
        }

        actionsRecyclerView.adapter = actionsAdapter
    }

    fun setQrResult(qrResult: QrResult) {
        if (isShowing) {
            return
        }

        this.qrResult = qrResult

        dataTextView.text = qrResult.text

        val firstAction = qrResult.actions.firstOrNull() ?: textAction
        cardView.contentDescription = firstAction.contentDescription
        titleTextView.text = firstAction.title
        if (firstAction.canTintIcon) {
            firstAction.icon?.setTint(
                context.getThemeColor(com.google.android.material.R.attr.colorOnBackground)
            )
        }
        iconImageView.isVisible = firstAction.icon != null
        iconImageView.setImageIcon(firstAction.icon)

        // Make links clickable if not on locked keyguard
        dataTextView.movementMethod = when (keyguardManager.isKeyguardLocked) {
            true -> null
            false -> LinkMovementMethod.getInstance()
        }

        actionsAdapter.submitList(qrResult.actions.drop(1))

        // Show the dialog
        show()
    }
}
