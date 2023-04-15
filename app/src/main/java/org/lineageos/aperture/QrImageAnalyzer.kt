/*
 * SPDX-FileCopyrightText: 2022-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture

import android.app.Activity
import android.app.KeyguardManager
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.LocaleList
import android.os.Looper
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.LinearLayoutCompat.LayoutParams
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.cardview.widget.CardView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import org.lineageos.aperture.utils.QrTextClassifier

class QrImageAnalyzer(private val activity: Activity) : ImageAnalysis.Analyzer {
    private val bottomSheetDialog by lazy {
        BottomSheetDialog(activity).apply {
            setContentView(R.layout.qr_bottom_sheet_dialog)
        }
    }
    private val bottomSheetDialogCardView by lazy {
        bottomSheetDialog.findViewById<CardView>(R.id.cardView)!!
    }
    private val bottomSheetDialogTitle by lazy {
        bottomSheetDialog.findViewById<TextView>(R.id.title)!!
    }
    private val bottomSheetDialogData by lazy {
        bottomSheetDialog.findViewById<TextView>(R.id.data)!!.apply {
            setTextClassifier(QrTextClassifier(context, textClassifier))
        }
    }
    private val bottomSheetDialogIcon by lazy {
        bottomSheetDialog.findViewById<ImageView>(R.id.icon)!!
    }
    private val bottomSheetDialogCopy by lazy {
        bottomSheetDialog.findViewById<ImageButton>(R.id.copy)!!
    }
    private val bottomSheetDialogShare by lazy {
        bottomSheetDialog.findViewById<ImageButton>(R.id.share)!!
    }
    private val bottomSheetDialogActionsLayout by lazy {
        bottomSheetDialog.findViewById<LinearLayout>(R.id.actionsLayout)!!
    }

    private val reader by lazy { MultiFormatReader() }

    private val clipboardManager by lazy { activity.getSystemService(ClipboardManager::class.java) }
    private val keyguardManager by lazy { activity.getSystemService(KeyguardManager::class.java) }

    override fun analyze(image: ImageProxy) {
        val source = image.planarYUVLuminanceSource

        val result = runCatching {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
        }.getOrNull() ?: runCatching {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(source.invert())))
        }.getOrNull()

        result?.let {
            showQrDialog(it)
        }

        reader.reset()
        image.close()
    }

    private fun showQrDialog(result: Result) {
        activity.runOnUiThread {
            if (bottomSheetDialog.isShowing) {
                return@runOnUiThread
            }

            // Classify message
            val span = SpannableString(result.text)
            bottomSheetDialogData.text = span
            Thread {
                val textClassification = bottomSheetDialogData.textClassifier.classifyText(
                    span, 0, span.length, LocaleList.getDefault()
                )

                activity.runOnUiThread {
                    bottomSheetDialogData.text = textClassification.text
                    bottomSheetDialogActionsLayout.removeAllViews()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                        textClassification.actions.isNotEmpty()
                    ) {
                        with(textClassification.actions[0]) {
                            bottomSheetDialogCardView.setOnClickListener { actionIntent.send() }
                            bottomSheetDialogData.movementMethod = null
                            bottomSheetDialogTitle.text = this.title
                            this.icon.loadDrawableAsync(activity, {
                                bottomSheetDialogIcon.setImageDrawable(it)
                            }, Handler(Looper.getMainLooper()))
                        }
                        for (action in textClassification.actions.drop(1)) {
                            bottomSheetDialogActionsLayout.addView(inflateButton().apply {
                                setOnClickListener { action.actionIntent.send() }
                                text = action.title
                                action.icon.loadDrawableAsync(activity, {
                                    it.setBounds(0, 0, 15.px, 15.px)
                                    setCompoundDrawables(
                                        it, null, null, null
                                    )
                                }, Handler(Looper.getMainLooper()))
                            })
                        }
                    } else {
                        bottomSheetDialogCardView.setOnClickListener {}
                        bottomSheetDialogTitle.text = activity.resources.getText(R.string.qr_text)
                        bottomSheetDialogIcon.setImageDrawable(
                            AppCompatResources.getDrawable(activity, R.drawable.ic_qr_type_text)
                        )
                    }
                }
            }.start()

            // Make links clickable if not on locked keyguard
            bottomSheetDialogData.movementMethod =
                if (!keyguardManager.isKeyguardLocked) LinkMovementMethod.getInstance()
                else null

            // Set buttons
            bottomSheetDialogCopy.setOnClickListener {
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText(
                        "", result.text
                    )
                )
            }

            bottomSheetDialogShare.setOnClickListener {
                activity.startActivity(
                    Intent.createChooser(
                        Intent().apply {
                            action = Intent.ACTION_SEND
                            type = ClipDescription.MIMETYPE_TEXT_PLAIN
                            putExtra(
                                Intent.EXTRA_TEXT, result.text
                            )
                        },
                        activity.getString(androidx.transition.R.string.abc_shareactionprovider_share_with)
                    )
                )
            }

            // Show dialog
            bottomSheetDialog.show()
        }
    }

    private fun inflateButton(): MaterialButton {
        val button = activity.layoutInflater.inflate(
            R.layout.qr_bottom_sheet_action_button, bottomSheetDialogActionsLayout, false
        ) as MaterialButton
        return button.apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
    }
}
