/*
 * SPDX-FileCopyrightText: 2022-2024 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.qr

import android.app.Activity
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.text.method.LinkMovementMethod
import android.view.textclassifier.TextClassificationManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.LinearLayoutCompat.LayoutParams
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import io.github.zxingcpp.BarcodeReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lineageos.aperture.R
import org.lineageos.aperture.ext.*
import kotlin.reflect.cast

class QrImageAnalyzer(private val activity: Activity, private val scope: CoroutineScope) :
    ImageAnalysis.Analyzer {
    // Views
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
        bottomSheetDialog.findViewById<TextView>(R.id.data)!!
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

    // System services
    private val clipboardManager by lazy { activity.getSystemService(ClipboardManager::class.java) }
    private val keyguardManager by lazy { activity.getSystemService(KeyguardManager::class.java) }
    private val textClassificationManager by lazy {
        activity.getSystemService(TextClassificationManager::class.java)
    }

    // QR
    private val reader by lazy { BarcodeReader() }

    private val qrTextClassifier by lazy {
        QrTextClassifier(activity, textClassificationManager.textClassifier)
    }

    override fun analyze(image: ImageProxy) {
        image.use {
            showQrDialog(reader.read(image))
        }
    }

    private fun showQrDialog(results: List<BarcodeReader.Result>) {
        scope.launch(Dispatchers.Main) {
            if (bottomSheetDialog.isShowing) {
                return@launch
            }

            val result = results.firstOrNull() ?: return@launch
            val text = result.text ?: return@launch
            bottomSheetDialogData.text = text

            // Classify message
            val textClassification = withContext(Dispatchers.IO) {
                qrTextClassifier.classifyText(
                    Result(
                        text, result.bytes, null, when (result.format) {
                            BarcodeReader.Format.NONE -> null
                            BarcodeReader.Format.AZTEC -> BarcodeFormat.AZTEC
                            BarcodeReader.Format.CODABAR -> BarcodeFormat.CODABAR
                            BarcodeReader.Format.CODE_39 -> BarcodeFormat.CODE_39
                            BarcodeReader.Format.CODE_93 -> BarcodeFormat.CODE_93
                            BarcodeReader.Format.CODE_128 -> BarcodeFormat.CODE_128
                            BarcodeReader.Format.DATA_BAR -> null
                            BarcodeReader.Format.DATA_BAR_EXPANDED -> null
                            BarcodeReader.Format.DATA_MATRIX -> BarcodeFormat.DATA_MATRIX
                            BarcodeReader.Format.EAN_8 -> BarcodeFormat.EAN_8
                            BarcodeReader.Format.EAN_13 -> BarcodeFormat.EAN_13
                            BarcodeReader.Format.ITF -> BarcodeFormat.ITF
                            BarcodeReader.Format.MAXICODE -> BarcodeFormat.MAXICODE
                            BarcodeReader.Format.PDF_417 -> BarcodeFormat.PDF_417
                            BarcodeReader.Format.QR_CODE -> BarcodeFormat.QR_CODE
                            BarcodeReader.Format.MICRO_QR_CODE -> BarcodeFormat.QR_CODE
                            BarcodeReader.Format.RMQR_CODE -> BarcodeFormat.QR_CODE
                            BarcodeReader.Format.UPC_A -> BarcodeFormat.UPC_A
                            BarcodeReader.Format.UPC_E -> BarcodeFormat.UPC_E
                        }
                    )
                )
            }

            bottomSheetDialogData.text = textClassification.text
            bottomSheetDialogActionsLayout.removeAllViews()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                textClassification.actions.isNotEmpty()
            ) {
                with(textClassification.actions[0]) {
                    bottomSheetDialogCardView.setOnClickListener {
                        try {
                            actionIntent.sendWithBalAllowed()
                        } catch (e: PendingIntent.CanceledException) {
                            Toast.makeText(
                                activity,
                                R.string.qr_no_app_available_for_action,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    bottomSheetDialogCardView.contentDescription = contentDescription
                    bottomSheetDialogData.movementMethod = null
                    bottomSheetDialogTitle.text = title
                    bottomSheetDialogIcon.setImageIcon(icon)
                }
                for (action in textClassification.actions.drop(1)) {
                    bottomSheetDialogActionsLayout.addView(inflateButton().apply {
                        setOnClickListener {
                            try {
                                action.actionIntent.sendWithBalAllowed()
                            } catch (e: PendingIntent.CanceledException) {
                                Toast.makeText(
                                    activity,
                                    R.string.qr_no_app_available_for_action,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        contentDescription = action.contentDescription
                        this.text = action.title
                        withContext(Dispatchers.IO) {
                            val drawable = action.icon.loadDrawable(activity)!!
                            drawable.setBounds(0, 0, 15.px, 15.px)
                            withContext(Dispatchers.Main) {
                                setCompoundDrawables(
                                    drawable, null, null, null
                                )
                            }
                        }
                    })
                }
            } else {
                bottomSheetDialogCardView.setOnClickListener {}
                bottomSheetDialogTitle.text = activity.resources.getText(R.string.qr_text)
                bottomSheetDialogIcon.setImageDrawable(AppCompatResources.getDrawable(
                    activity, R.drawable.ic_text_snippet
                )?.let {
                    DrawableCompat.wrap(it.mutate()).apply {
                        DrawableCompat.setTint(
                            this, activity.getThemeColor(
                                com.google.android.material.R.attr.colorOnBackground
                            )
                        )
                    }
                })
            }

            // Make links clickable if not on locked keyguard
            bottomSheetDialogData.movementMethod =
                if (!keyguardManager.isKeyguardLocked) LinkMovementMethod.getInstance()
                else null

            // Set buttons
            bottomSheetDialogCopy.setOnClickListener {
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText(
                        "", text
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
                                Intent.EXTRA_TEXT, text
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

    private fun inflateButton() = MaterialButton::class.cast(
        activity.layoutInflater.inflate(
            R.layout.qr_bottom_sheet_action_button,
            bottomSheetDialogActionsLayout,
            false
        )
    ).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }
}
